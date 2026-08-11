package com.thomrnowtea.livetracks

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thomrnowtea.livetracks.audio.AudioHardwareMonitor
import com.thomrnowtea.livetracks.audio.AudioOutputDevice
import com.thomrnowtea.livetracks.audio.AndroidAudioDecoder
import com.thomrnowtea.livetracks.audio.EngineDiagnostics
import com.thomrnowtea.livetracks.audio.NativeAudioController
import com.thomrnowtea.livetracks.audio.WavAnalysis
import com.thomrnowtea.livetracks.audio.WavAnalyzer
import com.thomrnowtea.livetracks.data.ProjectRepository
import com.thomrnowtea.livetracks.data.AppLanguage
import com.thomrnowtea.livetracks.data.AppSettings
import com.thomrnowtea.livetracks.data.AppSettingsRepository
import com.thomrnowtea.livetracks.domain.AudioMath
import com.thomrnowtea.livetracks.domain.MasterTrack
import com.thomrnowtea.livetracks.domain.MetronomeSettings
import com.thomrnowtea.livetracks.domain.Project
import com.thomrnowtea.livetracks.domain.SILENCE_DB
import com.thomrnowtea.livetracks.domain.SafetyStatus
import com.thomrnowtea.livetracks.domain.SourceMetadata
import com.thomrnowtea.livetracks.domain.TIMELINE_SAMPLE_RATE
import com.thomrnowtea.livetracks.domain.Track
import com.thomrnowtea.livetracks.domain.TrackType
import java.util.ArrayDeque
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

enum class Workspace { PROJECTS, PLAYLIST, TRACK, MASTER, SETTINGS }
enum class TrackWorkspace { TIMELINE, MIXER }

data class MixerTrackUi(
    val id: String,
    val name: String,
    val type: TrackType,
    val colorArgb: Long,
    val gainDb: Float,
    val pan: Float,
    val muted: Boolean,
    val soloed: Boolean,
    val mainSendLabel: String,
    val monitorSendLabel: String,
    val startOffsetFrames: Long,
    val durationSeconds: Double,
    val waveformPeaks: List<Float> = emptyList(),
    val peak: Float = 0f,
)

data class MainUiState(
    val projects: List<Project> = emptyList(),
    val selectedProjectId: String? = null,
    val selectedMasterTrackId: String? = null,
    val selectedTimelineTrackId: String? = null,
    val timelineCursorFrames: Long = 0,
    val canUndoTimeline: Boolean = false,
    val canRedoTimeline: Boolean = false,
    val tracks: List<MixerTrackUi> = emptyList(),
    val workspace: Workspace = Workspace.PROJECTS,
    val trackWorkspace: TrackWorkspace = TrackWorkspace.TIMELINE,
    val devices: List<AudioOutputDevice> = emptyList(),
    val diagnostics: EngineDiagnostics = EngineDiagnostics(),
    val safetyStatus: SafetyStatus = SafetyStatus.WARNING,
    val message: String = "Crea un proyecto para comenzar",
    val stereoSplit: Boolean = false,
    val openingOutput: Boolean = false,
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private data class TimelineEditSnapshot(
        val projectId: String,
        val masterTrackId: String,
        val tracks: List<Track>,
        val selectedTrackId: String?,
    )

    private val repository: ProjectRepository = (application as LiveTracksApplication).projectRepository
    private val settingsRepository: AppSettingsRepository = (application as LiveTracksApplication).settingsRepository
    private val resolver = application.contentResolver
    private val androidAudioDecoder = AndroidAudioDecoder(resolver, java.io.File(application.cacheDir, "decoded_audio"))
    private val audio = NativeAudioController()
    private val hardware = AudioHardwareMonitor(application, ::onDevicesChanged)
    private var requestedSampleRate = 0
    private var previousDeviceIds: Set<Int>? = null
    private var loadedSelectionKey: String? = null
    private var saveJob: Job? = null
    private val analysisCache = ConcurrentHashMap<String, WavAnalysis>()
    private val undoTimelineEdits = ArrayDeque<TimelineEditSnapshot>()
    private val redoTimelineEdits = ArrayDeque<TimelineEditSnapshot>()
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(settings = settingsRepository.read()) }
        hardware.start()
        viewModelScope.launch {
            val projects = runCatching { repository.getProjects() }.getOrElse { emptyList() }
            val project = projects.firstOrNull()
            val master = project?.playlist?.firstOrNull()
            _state.update {
                it.copy(
                    projects = projects,
                    selectedProjectId = project?.id,
                    selectedMasterTrackId = master?.id,
                    selectedTimelineTrackId = master?.tracks?.firstOrNull()?.id,
                    tracks = mixerTracks(master, analysisCache),
                    loading = false,
                    message = if (projects.isEmpty()) "Crea tu primer proyecto" else "Listo para configurar el show",
                )
            }
            analyzeTracks(master)
        }
        viewModelScope.launch {
            while (isActive) {
                if (_state.value.diagnostics.outputOpen) {
                    val diagnostics = audio.diagnostics(requestedSampleRate)
                    val peaks = audio.trackPeaks()
                    _state.update { current ->
                        current.copy(
                            diagnostics = diagnostics,
                            timelineCursorFrames = diagnostics.actualSampleRate.takeIf { it > 0 }?.let { rate ->
                                (diagnostics.renderedFrames.toDouble() * TIMELINE_SAMPLE_RATE / rate).roundToLong()
                            } ?: current.timelineCursorFrames,
                            tracks = current.tracks.mapIndexed { index, track -> track.copy(peak = peaks.getOrElse(index) { 0f }) },
                        )
                    }
                }
                delay(33)
            }
        }
    }

    fun setWorkspace(value: Workspace) = _state.update { it.copy(workspace = value) }
    fun setTrackWorkspace(value: TrackWorkspace) = _state.update { it.copy(trackWorkspace = value) }

    fun createProject(name: String) {
        clearTimelineHistory()
        val project = Project(UUID.randomUUID().toString(), name.trim().ifBlank { "Proyecto sin nombre" })
        _state.update {
            it.copy(projects = it.projects + project, selectedProjectId = project.id, selectedMasterTrackId = null,
                selectedTimelineTrackId = null, timelineCursorFrames = 0, tracks = emptyList(), message = "Proyecto creado")
        }
        invalidateAudio(); saveNow()
    }

    fun renameSelectedProject(name: String) {
        if (name.isBlank()) return
        updateSelectedProject { it.copy(name = name.trim()) }; saveNow()
    }

    fun deleteSelectedProject() {
        clearTimelineHistory()
        val selected = _state.value.selectedProjectId ?: return
        val remaining = _state.value.projects.filterNot { it.id == selected }
        val next = remaining.firstOrNull()
        _state.update {
            it.copy(projects = remaining, selectedProjectId = next?.id, selectedMasterTrackId = next?.playlist?.firstOrNull()?.id,
                selectedTimelineTrackId = next?.playlist?.firstOrNull()?.tracks?.firstOrNull()?.id, timelineCursorFrames = 0,
                tracks = mixerTracks(next?.playlist?.firstOrNull(), analysisCache), message = "Proyecto eliminado")
        }
        invalidateAudio(); saveNow()
    }

    fun selectProject(id: String) {
        val project = _state.value.projects.firstOrNull { it.id == id } ?: return
        val master = project.playlist.firstOrNull()
        clearTimelineHistory()
        _state.update { it.copy(selectedProjectId = id, selectedMasterTrackId = master?.id,
            selectedTimelineTrackId = master?.tracks?.firstOrNull()?.id, timelineCursorFrames = 0,
            tracks = mixerTracks(master, analysisCache), message = project.name) }
        invalidateAudio()
        analyzeTracks(master)
    }

    fun createMasterTrack(name: String) {
        if (selectedProject() == null) return
        clearTimelineHistory()
        val master = MasterTrack(UUID.randomUUID().toString(), name.trim().ifBlank { "Pista ${selectedProject()!!.playlist.size + 1}" })
        updateSelectedProject { it.copy(playlist = it.playlist + master) }
        _state.update { it.copy(selectedMasterTrackId = master.id, selectedTimelineTrackId = null,
            timelineCursorFrames = 0, tracks = emptyList(), workspace = Workspace.PLAYLIST, message = "Pista master agregada") }
        invalidateAudio(); saveNow()
    }

    fun renameSelectedMasterTrack(name: String) {
        if (name.isBlank()) return
        updateSelectedMaster { it.copy(name = name.trim()) }; saveNow()
    }

    fun deleteSelectedMasterTrack() {
        val selected = _state.value.selectedMasterTrackId ?: return
        val project = selectedProject() ?: return
        clearTimelineHistory()
        val remaining = project.playlist.filterNot { it.id == selected }
        val next = remaining.firstOrNull()
        updateSelectedProject { it.copy(playlist = remaining) }
        _state.update { it.copy(selectedMasterTrackId = next?.id, selectedTimelineTrackId = next?.tracks?.firstOrNull()?.id,
            timelineCursorFrames = 0, tracks = mixerTracks(next, analysisCache), message = "Pista master eliminada") }
        invalidateAudio(); saveNow()
    }

    fun selectMasterTrack(id: String) {
        val master = selectedProject()?.playlist?.firstOrNull { it.id == id } ?: return
        clearTimelineHistory()
        _state.update { it.copy(selectedMasterTrackId = id, selectedTimelineTrackId = master.tracks.firstOrNull()?.id,
            timelineCursorFrames = 0, tracks = mixerTracks(master, analysisCache), message = master.name) }
        invalidateAudio()
        analyzeTracks(master)
    }

    fun playMasterTrack(id: String) {
        selectMasterTrack(id)
        playPause()
    }

    fun moveMasterTrack(from: Int, direction: Int) {
        val project = selectedProject() ?: return
        val to = (from + direction).coerceIn(0, project.playlist.lastIndex)
        if (from !in project.playlist.indices || from == to) return
        val reordered = project.playlist.toMutableList().apply { add(to, removeAt(from)) }
        updateSelectedProject { it.copy(playlist = reordered) }; saveNow()
    }

    fun importTracks(uris: List<Uri>) {
        val master = selectedMasterTrack()
        if (master == null || uris.isEmpty()) {
            _state.update { it.copy(workspace = Workspace.PLAYLIST, message = "Selecciona una pista master antes de importar stems") }
            return
        }
        val imported = uris.take(16 - master.tracks.size).mapNotNull { uri ->
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment ?: "Stem de audio"
                val type = when {
                    name.contains("click", true) -> TrackType.CLICK
                    name.contains("cue", true) -> TrackType.CUE
                    else -> _state.value.settings.defaultStemType
                }
                Track.create(UUID.randomUUID().toString(), name, type, uri.toString()).let { track ->
                    if (type == TrackType.MUSIC) track.copy(monitorSendDb = _state.value.settings.defaultMonitorSendDb) else track
                }
            }.getOrNull()
        }
        if (imported.isEmpty()) {
            _state.update { it.copy(message = "No se pudo conservar acceso a los archivos") }; return
        }
        recordTimelineEdit()
        updateSelectedMaster { it.copy(tracks = it.tracks + imported) }
        _state.update {
            it.copy(
                workspace = Workspace.TRACK,
                trackWorkspace = if (it.settings.openTimelineAfterImport) TrackWorkspace.TIMELINE else TrackWorkspace.MIXER,
                selectedTimelineTrackId = it.selectedTimelineTrackId ?: imported.first().id,
                message = "${imported.size} stems agregados",
            )
        }
        invalidateAudio(); saveNow()
        analyzeTracks(selectedMasterTrack())
    }

    fun createEmptyTrack(name: String, durationSeconds: Double) {
        val master = selectedMasterTrack() ?: return
        if (master.tracks.size >= 16) {
            _state.update { it.copy(message = "Máximo de 16 stems") }
            return
        }
        val durationFrames = (durationSeconds.coerceIn(0.001, 24.0 * 60.0 * 60.0) * TIMELINE_SAMPLE_RATE)
            .roundToLong().coerceAtLeast(1)
        val track = Track.create(
            UUID.randomUUID().toString(),
            name.trim().ifBlank { "Stem vacío" },
            _state.value.settings.defaultStemType,
        ).copy(sourceMetadata = SourceMetadata(1, TIMELINE_SAMPLE_RATE, durationFrames))
        recordTimelineEdit()
        updateSelectedMaster { it.copy(tracks = it.tracks + track) }
        _state.update {
            it.copy(
                workspace = Workspace.TRACK,
                trackWorkspace = TrackWorkspace.TIMELINE,
                selectedTimelineTrackId = track.id,
                message = "Stem vacío agregado · ${"%.3f".format(durationSeconds)} s",
            )
        }
        invalidateAudio()
        saveNow()
    }

    fun removeTrack(trackId: String) {
        if (selectedMasterTrack()?.tracks?.none { it.id == trackId } != false) return
        recordTimelineEdit()
        updateSelectedMaster { it.copy(tracks = it.tracks.filterNot { track -> track.id == trackId }) }
        _state.update { current ->
            current.copy(selectedTimelineTrackId = if (current.selectedTimelineTrackId == trackId) current.tracks.firstOrNull()?.id else current.selectedTimelineTrackId)
        }
        invalidateAudio(); saveNow()
    }

    fun selectTimelineTrack(trackId: String) {
        if (_state.value.tracks.any { it.id == trackId }) _state.update { it.copy(selectedTimelineTrackId = trackId) }
    }

    fun cycleTrackType(trackId: String) {
        updateSelectedTracks { track ->
            if (track.id != trackId) track else {
                val type = when (track.type) { TrackType.MUSIC -> TrackType.CLICK; TrackType.CLICK -> TrackType.CUE; else -> TrackType.MUSIC }
                track.copy(type = type, mainSendDb = if (type == TrackType.MUSIC) 0f else SILENCE_DB, monitorSendDb = if (type == TrackType.MUSIC) -6f else 0f)
            }
        }
        saveNow()
    }

    fun setTrackOffset(trackId: String, timelineFrames: Long) {
        val index = _state.value.tracks.indexOfFirst { it.id == trackId }
        if (index < 0) return
        val nextOffset = timelineFrames.coerceAtLeast(0)
        if (selectedMasterTrack()?.tracks?.getOrNull(index)?.startOffsetFrames == nextOffset) return
        recordTimelineEdit()
        updateTrack(index) { it.copy(startOffsetFrames = nextOffset) }
        val selected = selectedMasterTrack() ?: return
        if (loadedSelectionKey == selectionKey()) {
            val rate = _state.value.diagnostics.actualSampleRate.takeIf { it > 0 } ?: TIMELINE_SAMPLE_RATE
            val track = selected.tracks.getOrNull(index) ?: return
            audio.setTrackStartOffset(index, (track.startSeconds() * rate).toLong())
        }
    }

    fun setTimelineCursor(timelineFrames: Long) {
        val duration = ((selectedMasterTrack()?.durationSeconds() ?: 0.0) * TIMELINE_SAMPLE_RATE).roundToLong()
        val cursor = timelineFrames.coerceIn(0, duration.coerceAtLeast(0))
        _state.update { it.copy(timelineCursorFrames = cursor) }
        val rate = _state.value.diagnostics.actualSampleRate
        if (rate > 0 && _state.value.diagnostics.durationFrames > 0) {
            audio.seekTransport((cursor.toDouble() * rate / TIMELINE_SAMPLE_RATE).roundToLong())
        }
    }

    fun splitSelectedTrackAtCursor() {
        val master = selectedMasterTrack() ?: return
        if (master.tracks.size >= 16) {
            _state.update { it.copy(message = "Máximo de 16 stems") }
            return
        }
        val selectedId = _state.value.selectedTimelineTrackId ?: return
        val index = master.tracks.indexOfFirst { it.id == selectedId }
        val selected = master.tracks.getOrNull(index) ?: return
        val baseName = selected.name.substringBeforeLast('.', selected.name)
        val extension = selected.name.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        val split = selected.splitAtTimelineFrame(
            _state.value.timelineCursorFrames,
            UUID.randomUUID().toString(),
            "$baseName · B$extension",
        ) ?: run {
            _state.update { it.copy(message = "Ubica el cursor dentro del stem seleccionado") }
            return
        }
        recordTimelineEdit()
        val updated = master.tracks.toMutableList().apply {
            this[index] = split.first
            add(index + 1, split.second)
        }
        updateSelectedMaster { it.copy(tracks = updated) }
        _state.update { it.copy(selectedTimelineTrackId = split.second.id, message = "Stem dividido en el cursor") }
        invalidateAudio(); saveNow()
    }

    fun undoTimelineEdit() {
        val target = undoTimelineEdits.pollLast() ?: return
        val current = timelineSnapshot() ?: return
        if (target.projectId != current.projectId || target.masterTrackId != current.masterTrackId) {
            clearTimelineHistory()
            return
        }
        pushTimelineSnapshot(redoTimelineEdits, current)
        restoreTimelineSnapshot(target, "Deshacer aplicado")
    }

    fun redoTimelineEdit() {
        val target = redoTimelineEdits.pollLast() ?: return
        val current = timelineSnapshot() ?: return
        if (target.projectId != current.projectId || target.masterTrackId != current.masterTrackId) {
            clearTimelineHistory()
            return
        }
        pushTimelineSnapshot(undoTimelineEdits, current)
        restoreTimelineSnapshot(target, "Rehacer aplicado")
    }

    fun playPause() {
        val current = _state.value
        if (current.openingOutput) return
        if (current.diagnostics.outputOpen && loadedSelectionKey == selectionKey()) {
            val play = !current.diagnostics.toneEnabled
            if (play && current.diagnostics.durationFrames > 0 && current.diagnostics.renderedFrames >= current.diagnostics.durationFrames) {
                audio.resetTransport()
                _state.update { it.copy(timelineCursorFrames = 0) }
            }
            audio.setToneEnabled(play)
            _state.update { it.copy(diagnostics = audio.diagnostics(requestedSampleRate), message = if (play) "REPRODUCIENDO" else "PAUSA") }
            return
        }
        val project = selectedProject()
        val master = selectedMasterTrack()
        if (project == null || master == null) {
            _state.update { it.copy(workspace = Workspace.PLAYLIST, message = "Selecciona una pista de la playlist") }; return
        }
        if (master.tracks.isEmpty()) {
            _state.update { it.copy(workspace = Workspace.TRACK, message = "Importa al menos un stem de audio") }; return
        }
        _state.update { it.copy(openingOutput = true, message = "Preparando ${master.tracks.size} stems...") }
        viewModelScope.launch(Dispatchers.IO) {
            requestedSampleRate = hardware.nativeOutputSampleRate()
            var diagnostics = audio.open(requestedSampleRate)
            var error: String? = if (!diagnostics.outputOpen) diagnostics.lastError else null
            val metadata = mutableMapOf<String, SourceMetadata>()
            if (error == null) {
                audio.resetTransport(); audio.clearTracks()
                master.tracks.forEachIndexed { index, track ->
                    if (error != null) return@forEachIndexed
                    if (track.sourceUri == null) return@forEachIndexed
                    var decodeError: String? = null
                    val result = track.sourceUri.let(Uri::parse).let { uri ->
                        runCatching {
                            val direct = resolver.openFileDescriptor(uri, "r")?.use { audio.loadWavTrack(index, it.fd) } ?: -1
                            if (direct == 0) direct else {
                                val decoded = androidAudioDecoder.decodedWav(uri)
                                ParcelFileDescriptor.open(decoded, ParcelFileDescriptor.MODE_READ_ONLY).use {
                                    audio.loadWavTrack(index, it.fd)
                                }
                            }
                        }.getOrElse {
                            decodeError = it.message
                            -9
                        }
                    }
                    if (result == 0) {
                        val raw = audio.trackMetadata(index)
                        if (raw.size >= 3 && raw[0] > 0) metadata[track.id] = SourceMetadata(raw[0].toInt(), raw[1].toInt(), raw[2])
                    } else {
                        error = "${track.name}: ${decodeError ?: "audio no compatible (código $result)"}"
                    }
                }
            }
            if (error == null) {
                val inspected = master.copy(tracks = master.tracks.map { track -> metadata[track.id]?.let { track.copy(sourceMetadata = it) } ?: track })
                replaceSelectedMaster(inspected)
                repository.replaceProjects(_state.value.projects)
                configureNativeMixer(project, inspected)
                val outputRate = diagnostics.actualSampleRate.takeIf { it > 0 } ?: requestedSampleRate
                if (outputRate > 0 && _state.value.timelineCursorFrames > 0) {
                    audio.seekTransport((_state.value.timelineCursorFrames.toDouble() * outputRate / TIMELINE_SAMPLE_RATE).roundToLong())
                }
                audio.setToneEnabled(true)
                loadedSelectionKey = selectionKey()
                diagnostics = audio.diagnostics(requestedSampleRate)
            } else {
                audio.resetTransport(); loadedSelectionKey = null
            }
            _state.update {
                it.copy(openingOutput = false, diagnostics = diagnostics,
                    safetyStatus = if (error == null) SafetyStatus.WARNING else SafetyStatus.UNSAFE,
                    message = error ?: "REPRODUCIENDO · ${master.name}")
            }
        }
    }

    fun stop() {
        audio.resetTransport()
        _state.update { it.copy(diagnostics = audio.diagnostics(requestedSampleRate), timelineCursorFrames = 0,
            tracks = it.tracks.map { t -> t.copy(peak = 0f) }, message = "DETENIDO · 00:00") }
    }

    fun seekToFraction(fraction: Float) {
        val timelineDuration = ((selectedMasterTrack()?.durationSeconds() ?: 0.0) * TIMELINE_SAMPLE_RATE).roundToLong()
        setTimelineCursor((timelineDuration * fraction.coerceIn(0f, 1f)).roundToLong())
    }

    fun panic() {
        audio.panic()
        _state.update { it.copy(diagnostics = audio.diagnostics(requestedSampleRate), safetyStatus = SafetyStatus.UNSAFE, message = "MUTE ALL · revalidacion requerida") }
    }

    fun setTrackGain(index: Int, gainDb: Float) { updateTrack(index) { it.copy(gainDb = gainDb) }; audio.setTrackGain(index, AudioMath.dbToLinear(gainDb)) }
    fun setTrackPan(index: Int, pan: Float) { updateTrack(index) { it.copy(pan = pan) }; audio.setTrackPan(index, pan) }
    fun toggleMute(index: Int) { val next = !_state.value.tracks.getOrNull(index)?.muted.orFalse(); updateTrack(index) { it.copy(muted = next) }; audio.setTrackMuted(index, next) }
    fun toggleSolo(index: Int) { val next = !_state.value.tracks.getOrNull(index)?.soloed.orFalse(); updateTrack(index) { it.copy(soloed = next) }; audio.setTrackSoloed(index, next) }

    fun setProjectGain(value: Float) { updateSelectedProject { it.copy(masterGainDb = value.coerceIn(-60f, 6f)) }; applyLiveMaster(); scheduleSave() }
    fun setProjectPan(value: Float) { updateSelectedProject { it.copy(masterPan = value.coerceIn(-1f, 1f)) }; applyLiveMaster(); scheduleSave() }
    fun setMasterGain(value: Float) { updateSelectedMaster { it.copy(gainDb = value.coerceIn(-60f, 6f)) }; applyLiveMaster(); scheduleSave() }
    fun setMasterPan(value: Float) { updateSelectedMaster { it.copy(pan = value.coerceIn(-1f, 1f)) }; applyLiveMaster(); scheduleSave() }

    fun setStereoSplit(enabled: Boolean) {
        audio.setOutputMode(enabled)
        _state.update { it.copy(stereoSplit = enabled, message = if (enabled) "STEREO SPLIT · L MAIN / R MONITOR" else "SINGLE MIX") }
    }

    fun setLanguage(value: AppLanguage) = updateSettings { it.copy(language = value) }
    fun setKeepScreenAwake(value: Boolean) = updateSettings { it.copy(keepScreenAwake = value) }
    fun setConfirmDestructiveActions(value: Boolean) = updateSettings { it.copy(confirmDestructiveActions = value) }
    fun setDefaultStemType(value: TrackType) = updateSettings { it.copy(defaultStemType = value) }
    fun setDefaultMonitorSendDb(value: Float) = updateSettings { it.copy(defaultMonitorSendDb = value.coerceIn(-60f, 0f)) }
    fun setOpenTimelineAfterImport(value: Boolean) = updateSettings { it.copy(openTimelineAfterImport = value) }

    fun setMasterUsesDefault(useDefault: Boolean) {
        val project = selectedProject() ?: return
        updateSelectedMaster { it.copy(metronomeOverride = if (useDefault) null else it.metronome(project.defaultMetronome)) }
        applyLiveMetronome(); saveNow()
    }

    fun updateDefaultMetronome(transform: (MetronomeSettings) -> MetronomeSettings) {
        updateSelectedProject { it.copy(defaultMetronome = transform(it.defaultMetronome)) }
        if (selectedMasterTrack()?.metronomeOverride == null) applyLiveMetronome()
        scheduleSave()
    }

    fun updateMasterMetronome(transform: (MetronomeSettings) -> MetronomeSettings) {
        val project = selectedProject() ?: return
        updateSelectedMaster { it.copy(metronomeOverride = transform(it.metronome(project.defaultMetronome))) }
        applyLiveMetronome(); scheduleSave()
    }

    private fun configureNativeMixer(project: Project, master: MasterTrack) {
        val outputRate = _state.value.diagnostics.actualSampleRate.takeIf { it > 0 } ?: requestedSampleRate.takeIf { it > 0 } ?: TIMELINE_SAMPLE_RATE
        master.tracks.forEachIndexed { index, track ->
            audio.setTrackGain(index, AudioMath.dbToLinear(track.gainDb)); audio.setTrackPan(index, track.pan)
            audio.setTrackMuted(index, track.muted); audio.setTrackSoloed(index, track.soloed)
            audio.setTrackSends(index, AudioMath.dbToLinear(track.mainSendDb), AudioMath.dbToLinear(track.monitorSendDb))
            audio.setTrackStartOffset(index, (track.startSeconds() * outputRate).toLong())
            audio.setTrackSourceRange(index, track.sourceStartFrame, track.sourceEndFrameExclusive ?: -1)
        }
        audio.setTimelineDuration((master.durationSeconds() * outputRate).roundToLong())
        val combinedGainDb = (project.masterGainDb + master.gainDb).coerceIn(-120f, 6f)
        audio.setMasterGainPan(AudioMath.dbToLinear(combinedGainDb), (project.masterPan + master.pan).coerceIn(-1f, 1f))
        audio.setOutputMode(_state.value.stereoSplit)
        configureNativeMetronome(master.metronome(project.defaultMetronome))
    }

    private fun configureNativeMetronome(value: MetronomeSettings) = audio.configureMetronome(
        value.enabled, value.bpm, value.numerator, value.denominator, AudioMath.dbToLinear(value.gainDb), value.mainEnabled,
    )

    private fun applyLiveMaster() {
        if (loadedSelectionKey != selectionKey()) return
        val project = selectedProject() ?: return; val master = selectedMasterTrack() ?: return
        audio.setMasterGainPan(AudioMath.dbToLinear((project.masterGainDb + master.gainDb).coerceIn(-120f, 6f)), (project.masterPan + master.pan).coerceIn(-1f, 1f))
    }

    private fun applyLiveMetronome() {
        if (loadedSelectionKey != selectionKey()) return
        val project = selectedProject() ?: return; val master = selectedMasterTrack() ?: return
        configureNativeMetronome(master.metronome(project.defaultMetronome))
    }

    private fun updateTrack(index: Int, transform: (Track) -> Track) {
        val id = _state.value.tracks.getOrNull(index)?.id ?: return
        updateSelectedTracks { if (it.id == id) transform(it) else it }; scheduleSave()
    }

    private fun updateSelectedTracks(transform: (Track) -> Track) = updateSelectedMaster { it.copy(tracks = it.tracks.map(transform)) }

    private fun updateSelectedProject(transform: (Project) -> Project) {
        val id = _state.value.selectedProjectId ?: return
        val projects = _state.value.projects.map { if (it.id == id) transform(it) else it }
        val master = projects.firstOrNull { it.id == id }?.playlist?.firstOrNull { it.id == _state.value.selectedMasterTrackId }
        val peaks = _state.value.tracks.associate { it.id to it.peak }
        _state.update { it.copy(projects = projects, tracks = mixerTracks(master, analysisCache).map { t -> t.copy(peak = peaks[t.id] ?: 0f) }) }
    }

    private fun updateSelectedMaster(transform: (MasterTrack) -> MasterTrack) {
        val masterId = _state.value.selectedMasterTrackId ?: return
        updateSelectedProject { project -> project.copy(playlist = project.playlist.map { if (it.id == masterId) transform(it) else it }) }
    }

    private fun replaceSelectedMaster(master: MasterTrack) = updateSelectedProject { project ->
        project.copy(playlist = project.playlist.map { if (it.id == master.id) master else it })
    }

    private fun selectedProject() = _state.value.projects.firstOrNull { it.id == _state.value.selectedProjectId }
    private fun selectedMasterTrack() = selectedProject()?.playlist?.firstOrNull { it.id == _state.value.selectedMasterTrackId }
    private fun selectionKey() = "${_state.value.selectedProjectId}/${_state.value.selectedMasterTrackId}"

    private fun timelineSnapshot(): TimelineEditSnapshot? {
        val projectId = _state.value.selectedProjectId ?: return null
        val master = selectedMasterTrack() ?: return null
        return TimelineEditSnapshot(projectId, master.id, master.tracks, _state.value.selectedTimelineTrackId)
    }

    private fun recordTimelineEdit() {
        val snapshot = timelineSnapshot() ?: return
        pushTimelineSnapshot(undoTimelineEdits, snapshot)
        redoTimelineEdits.clear()
        updateTimelineHistoryAvailability()
    }

    private fun pushTimelineSnapshot(history: ArrayDeque<TimelineEditSnapshot>, snapshot: TimelineEditSnapshot) {
        if (history.size >= TIMELINE_HISTORY_LIMIT) history.removeFirst()
        history.addLast(snapshot)
    }

    private fun restoreTimelineSnapshot(snapshot: TimelineEditSnapshot, message: String) {
        val master = selectedMasterTrack() ?: return
        replaceSelectedMaster(master.copy(tracks = snapshot.tracks))
        val selectedId = snapshot.selectedTrackId?.takeIf { id -> snapshot.tracks.any { it.id == id } }
            ?: snapshot.tracks.firstOrNull()?.id
        _state.update { it.copy(selectedTimelineTrackId = selectedId, message = message) }
        invalidateAudio()
        saveNow()
        analyzeTracks(selectedMasterTrack())
        updateTimelineHistoryAvailability()
    }

    private fun clearTimelineHistory() {
        undoTimelineEdits.clear()
        redoTimelineEdits.clear()
        updateTimelineHistoryAvailability()
    }

    private fun updateTimelineHistoryAvailability() = _state.update {
        it.copy(canUndoTimeline = undoTimelineEdits.isNotEmpty(), canRedoTimeline = redoTimelineEdits.isNotEmpty())
    }

    private fun analyzeTracks(master: MasterTrack?) {
        val candidates = master?.tracks?.filter { it.sourceUri != null }.orEmpty()
        if (candidates.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = mutableMapOf<String, SourceMetadata>()
            candidates.forEach { track ->
                val uri = track.sourceUri ?: return@forEach
                val analysis = analysisCache[uri] ?: runCatching {
                    resolver.openFileDescriptor(Uri.parse(uri), "r")?.let { descriptor ->
                        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input -> WavAnalyzer.analyze(input) }
                    }
                }.recoverCatching {
                    FileInputStream(androidAudioDecoder.decodedWav(Uri.parse(uri))).use(WavAnalyzer::analyze)
                }.getOrNull()?.also { analysisCache[uri] = it }
                if (analysis != null) metadata[track.id] = analysis.metadata
            }
            if (metadata.isEmpty()) return@launch
            _state.update { current ->
                val projects = current.projects.map { project ->
                    project.copy(playlist = project.playlist.map { item ->
                        item.copy(tracks = item.tracks.map { track -> metadata[track.id]?.let { track.copy(sourceMetadata = it) } ?: track })
                    })
                }
                val selected = projects.firstOrNull { it.id == current.selectedProjectId }
                    ?.playlist?.firstOrNull { it.id == current.selectedMasterTrackId }
                current.copy(projects = projects, tracks = mixerTracks(selected, analysisCache))
            }
            repository.replaceProjects(_state.value.projects)
        }
    }

    private fun invalidateAudio() { audio.resetTransport(); audio.clearTracks(); loadedSelectionKey = null }
    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _state.update { current -> current.copy(settings = transform(current.settings)) }
        settingsRepository.write(_state.value.settings)
    }
    private fun saveNow() { saveJob?.cancel(); viewModelScope.launch(Dispatchers.IO) { repository.replaceProjects(_state.value.projects) } }
    private fun scheduleSave() { saveJob?.cancel(); saveJob = viewModelScope.launch { delay(350); repository.replaceProjects(_state.value.projects) } }

    private fun onDevicesChanged(devices: List<AudioOutputDevice>) {
        val ids = devices.map(AudioOutputDevice::id).toSet()
        val changed = previousDeviceIds?.let { it != ids } == true && _state.value.diagnostics.outputOpen
        previousDeviceIds = ids
        if (changed) {
            audio.panic(); audio.close(); loadedSelectionKey = null
            _state.update { it.copy(devices = devices, diagnostics = EngineDiagnostics(requestedSampleRate = requestedSampleRate), safetyStatus = SafetyStatus.UNSAFE, message = "RUTA CAMBIO · salida detenida") }
        } else _state.update { it.copy(devices = devices) }
    }

    override fun onCleared() { hardware.stop(); audio.close(); super.onCleared() }
}

private const val TIMELINE_HISTORY_LIMIT = 50

private fun mixerTracks(master: MasterTrack?, analysisCache: Map<String, WavAnalysis>): List<MixerTrackUi> = master?.tracks?.mapIndexed { index, track ->
    val colors = longArrayOf(0xFF43D3B3, 0xFF5C8DFF, 0xFFB778FF, 0xFFF4B64A, 0xFFE95A64, 0xFF55C98A)
    val analysis = track.sourceUri?.let(analysisCache::get)
    val metadata = track.sourceMetadata ?: analysis?.metadata
    val fullWaveform = analysis?.peaks.orEmpty()
    val waveform = if (metadata != null && fullWaveform.isNotEmpty() && metadata.durationFrames > 0) {
        val from = ((track.sourceStartFrame.toDouble() / metadata.durationFrames) * fullWaveform.size)
            .toInt().coerceIn(0, fullWaveform.lastIndex)
        val sourceEnd = (track.sourceEndFrameExclusive ?: metadata.durationFrames).coerceAtMost(metadata.durationFrames)
        val to = ((sourceEnd.toDouble() / metadata.durationFrames) * fullWaveform.size)
            .toInt().coerceIn(from + 1, fullWaveform.size)
        fullWaveform.subList(from, to)
    } else emptyList()
    MixerTrackUi(
        id = track.id, name = track.name, type = track.type, colorArgb = colors[index % colors.size],
        gainDb = track.gainDb, pan = track.pan, muted = track.muted, soloed = track.soloed,
        mainSendLabel = if (track.mainSendDb == SILENCE_DB) "OFF" else "${track.mainSendDb.toInt()} dB",
        monitorSendLabel = if (track.monitorSendDb == SILENCE_DB) "OFF" else "${track.monitorSendDb.toInt()} dB",
        startOffsetFrames = track.startOffsetFrames, durationSeconds = track.durationSeconds(), waveformPeaks = waveform,
    )
} ?: emptyList()

private fun Boolean?.orFalse() = this ?: false
