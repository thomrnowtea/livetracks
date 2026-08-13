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
import com.thomrnowtea.livetracks.audio.PerformanceModeController
import com.thomrnowtea.livetracks.audio.WavAnalysis
import com.thomrnowtea.livetracks.audio.WavAnalyzer
import com.thomrnowtea.livetracks.audio.VoiceCueException
import com.thomrnowtea.livetracks.audio.VoiceCueRenderer
import com.thomrnowtea.livetracks.data.ProjectRepository
import com.thomrnowtea.livetracks.data.AppLanguage
import com.thomrnowtea.livetracks.data.WaveformCache
import com.thomrnowtea.livetracks.data.AppSettings
import com.thomrnowtea.livetracks.data.AppSettingsRepository
import com.thomrnowtea.livetracks.data.AppUpdateInstaller
import com.thomrnowtea.livetracks.data.GitHubAppReleaseRepository
import com.thomrnowtea.livetracks.data.UpdateInstallException
import com.thomrnowtea.livetracks.data.UpdateRepositoryException
import com.thomrnowtea.livetracks.data.isNewerRelease
import com.thomrnowtea.livetracks.domain.AppUpdateStatus
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
import com.thomrnowtea.livetracks.domain.TimelineMarker
import com.thomrnowtea.livetracks.domain.TimelineMarkerKind
import com.thomrnowtea.livetracks.domain.UpdateFailure
import com.thomrnowtea.livetracks.domain.voiceCueStartFrames
import com.thomrnowtea.livetracks.domain.nextPlaylistIndexAfterCompletion
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
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

enum class Workspace { PROJECTS, PLAYLIST, TRACK, SETTINGS }
enum class ProjectWorkspace { LIST, MIXER }
enum class PlaylistWorkspace { LIST, MIXER }
enum class TrackWorkspace { METRONOME, TIMELINE, MIXER }

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
    val hasAudioSource: Boolean,
    val isClickReference: Boolean = false,
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
    val projectWorkspace: ProjectWorkspace = ProjectWorkspace.LIST,
    val playlistWorkspace: PlaylistWorkspace = PlaylistWorkspace.LIST,
    val trackWorkspace: TrackWorkspace = TrackWorkspace.TIMELINE,
    val playlistPerformanceMode: Boolean = false,
    val playlistEditBarExpanded: Boolean = true,
    val devices: List<AudioOutputDevice> = emptyList(),
    val diagnostics: EngineDiagnostics = EngineDiagnostics(),
    val safetyStatus: SafetyStatus = SafetyStatus.WARNING,
    val message: String = "Crea un proyecto para comenzar",
    val stereoSplit: Boolean = false,
    val openingOutput: Boolean = false,
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val appUpdateStatus: AppUpdateStatus = AppUpdateStatus.Idle,
    val renderingVoiceCueIds: Set<String> = emptySet(),
    val failedVoiceCueIds: Set<String> = emptySet(),
    val notificationPolicyAccessGranted: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private data class TimelineEditSnapshot(
        val projectId: String,
        val playlist: List<MasterTrack>,
        val selectedMasterTrackId: String?,
        val selectedTrackId: String?,
    )

    private val repository: ProjectRepository = (application as LiveTracksApplication).projectRepository
    private val settingsRepository: AppSettingsRepository = (application as LiveTracksApplication).settingsRepository
    private val releaseRepository = GitHubAppReleaseRepository("LiveTracks/${BuildConfig.VERSION_NAME}")
    private val updateInstaller = AppUpdateInstaller(application)
    private val resolver = application.contentResolver
    private val androidAudioDecoder = AndroidAudioDecoder(resolver, java.io.File(application.cacheDir, "decoded_audio"))
    private val waveformCache = WaveformCache(application)
    private val voiceCueRenderer = VoiceCueRenderer(application)
    private val performanceMode = PerformanceModeController(application)
    private val audio = NativeAudioController()
    private val hardware = AudioHardwareMonitor(application, ::onDevicesChanged)
    private var requestedSampleRate = 0
    private var previousDeviceIds: Set<Int>? = null
    private var loadedSelectionKey: String? = null
    private var saveJob: Job? = null
    private var updateJob: Job? = null
    private val analysisCache = ConcurrentHashMap<String, WavAnalysis>()
    private val undoTimelineEdits = ArrayDeque<TimelineEditSnapshot>()
    private val redoTimelineEdits = ArrayDeque<TimelineEditSnapshot>()
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(settings = settingsRepository.read(), notificationPolicyAccessGranted = performanceMode.hasNotificationPolicyAccess) }
        hardware.start()
        viewModelScope.launch {
            val storedProjects = runCatching { repository.getProjects() }.getOrElse { emptyList() }
            val projects = storedProjects.map { project ->
                project.copy(playlist = project.playlist.map { master ->
                    if (master.metronomeOverride == null) {
                        master.copy(metronomeOverride = project.defaultMetronome)
                    } else master
                })
            }
            if (projects != storedProjects) repository.replaceProjects(projects)
            val project = projects.firstOrNull()
            val master = project?.playlist?.firstOrNull()
            val cachedAnalyses = withContext(Dispatchers.IO) {
                projects.asSequence()
                    .flatMap { it.playlist.asSequence() }
                    .flatMap { it.tracks.asSequence() }
                    .mapNotNull(Track::sourceUri)
                    .distinct()
                    .mapNotNull { uri ->
                        waveformCache.read(uri)?.let { cached -> uri to WavAnalysis(cached.metadata, cached.peaks) }
                    }
                    .toMap()
            }
            analysisCache.putAll(cachedAnalyses)
            _state.update {
                it.copy(
                    projects = projects,
                    selectedProjectId = project?.id,
                    selectedMasterTrackId = master?.id,
                    selectedTimelineTrackId = master?.tracks?.firstOrNull()?.id,
                    tracks = mixerTracks(master, analysisCache),
                    loading = false,
                    message = if (projects.isEmpty()) uiText("Crea tu primer proyecto", "Create your first project")
                    else uiText("Listo para configurar el show", "Ready to configure the show"),
                )
            }
            analyzeTracks(master)
        }
        if (_state.value.settings.automaticUpdateChecks) checkForUpdates(manual = false)
        viewModelScope.launch {
            while (isActive) {
                if (_state.value.diagnostics.outputOpen) {
                    val previousDiagnostics = _state.value.diagnostics
                    val diagnostics = audio.diagnostics(requestedSampleRate)
                    val completedNaturally = previousDiagnostics.toneEnabled &&
                        !diagnostics.toneEnabled && diagnostics.durationFrames > 0 &&
                        diagnostics.renderedFrames >= diagnostics.durationFrames
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
                    if (completedNaturally) continuePlaylistAfterCompletion()
                    else if (!diagnostics.toneEnabled && !_state.value.openingOutput) performanceMode.deactivate()
                }
                delay(33)
            }
        }
    }

    fun setWorkspace(value: Workspace) = _state.update {
        it.copy(workspace = value, playlistPerformanceMode = if (value == Workspace.PLAYLIST) it.playlistPerformanceMode else false)
    }

    fun setPlaylistPerformanceMode(enabled: Boolean) {
        val canEnter = selectedProject()?.playlist?.isNotEmpty() == true
        _state.update { it.copy(playlistPerformanceMode = enabled && canEnter, workspace = Workspace.PLAYLIST) }
    }

    fun setPlaylistEditBarExpanded(expanded: Boolean) = _state.update { it.copy(playlistEditBarExpanded = expanded) }
    fun setProjectWorkspace(value: ProjectWorkspace) = _state.update { it.copy(projectWorkspace = value) }
    fun setPlaylistWorkspace(value: PlaylistWorkspace) = _state.update { it.copy(playlistWorkspace = value) }
    fun setTrackWorkspace(value: TrackWorkspace) = _state.update { it.copy(trackWorkspace = value) }

    fun createProject(name: String) {
        clearTimelineHistory()
        val project = Project(UUID.randomUUID().toString(), name.trim().ifBlank { uiText("Proyecto sin nombre", "Untitled project") })
        _state.update {
            it.copy(projects = it.projects + project, selectedProjectId = project.id, selectedMasterTrackId = null,
                selectedTimelineTrackId = null, timelineCursorFrames = 0, tracks = emptyList(), message = uiText("Proyecto creado", "Project created"))
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
                tracks = mixerTracks(next?.playlist?.firstOrNull(), analysisCache), message = uiText("Proyecto eliminado", "Project deleted"))
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
        val project = selectedProject() ?: return
        clearTimelineHistory()
        val master = MasterTrack(
            UUID.randomUUID().toString(),
            name.trim().ifBlank { "${uiText("Pista", "Track")} ${project.playlist.size + 1}" },
            metronomeOverride = project.defaultMetronome,
        )
        updateSelectedProject { it.copy(playlist = it.playlist + master) }
        _state.update { it.copy(selectedMasterTrackId = master.id, selectedTimelineTrackId = null,
            timelineCursorFrames = 0, tracks = emptyList(), workspace = Workspace.PLAYLIST, message = uiText("Pista master agregada", "Master track added")) }
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
            timelineCursorFrames = 0, tracks = mixerTracks(next, analysisCache), message = uiText("Pista master eliminada", "Master track deleted")) }
        invalidateAudio(); saveNow()
    }

    fun selectMasterTrack(id: String) = selectMasterTrack(id, preservePerformanceMode = false)

    private fun selectMasterTrack(id: String, preservePerformanceMode: Boolean) {
        val master = selectedProject()?.playlist?.firstOrNull { it.id == id } ?: return
        clearTimelineHistory()
        _state.update { it.copy(selectedMasterTrackId = id, selectedTimelineTrackId = master.tracks.firstOrNull()?.id,
            timelineCursorFrames = 0, tracks = mixerTracks(master, analysisCache), message = master.name) }
        invalidateAudio(preservePerformanceMode)
        analyzeTracks(master)
    }

    fun playMasterTrack(id: String) {
        selectMasterTrack(id)
        playPause()
    }

    fun skipToPreviousMasterTrack() = skipMasterTrack(-1)

    fun skipToNextMasterTrack() = skipMasterTrack(1)

    private fun skipMasterTrack(direction: Int) {
        val project = selectedProject() ?: return
        val current = project.playlist.indexOfFirst { it.id == _state.value.selectedMasterTrackId }
        val target = project.playlist.getOrNull(current + direction) ?: return
        selectMasterTrack(target.id)
        _state.update { it.copy(message = "${uiText("ARMADA", "ARMED")} · ${target.name}") }
    }

    private fun continuePlaylistAfterCompletion() {
        val project = selectedProject() ?: return
        val current = project.playlist.indexOfFirst { it.id == _state.value.selectedMasterTrackId }
        val next = nextPlaylistIndexAfterCompletion(current, project.playlist.size)?.let(project.playlist::get)
        if (next == null) {
            performanceMode.deactivate()
            _state.update {
                it.copy(
                    timelineCursorFrames = ((selectedMasterTrack()?.durationSeconds() ?: 0.0) * TIMELINE_SAMPLE_RATE).roundToLong(),
                    message = uiText("FIN DE LA PLAYLIST", "END OF SETLIST"),
                )
            }
            return
        }
        selectMasterTrack(next.id, preservePerformanceMode = true)
        _state.update { it.copy(message = "${uiText("CONTINÚA", "CONTINUING")} · ${next.name}") }
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
            _state.update { it.copy(workspace = Workspace.PLAYLIST, message = uiText("Selecciona una pista master antes de importar stems", "Select a master track before importing stems")) }
            return
        }
        val imported = uris.take(16 - master.tracks.size).mapNotNull { uri ->
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment ?: uiText("Stem de audio", "Audio stem")
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
            _state.update { it.copy(message = uiText("No se pudo conservar acceso a los archivos", "Could not retain access to the files")) }; return
        }
        recordTimelineEdit()
        updateSelectedMaster { it.copy(tracks = it.tracks + imported) }
        _state.update {
            it.copy(
                workspace = Workspace.TRACK,
                trackWorkspace = if (it.settings.openTimelineAfterImport) TrackWorkspace.TIMELINE else TrackWorkspace.MIXER,
                selectedTimelineTrackId = it.selectedTimelineTrackId ?: imported.first().id,
                message = "${imported.size} ${uiText("stems agregados", "stems added")}",
            )
        }
        invalidateAudio(); saveNow()
        analyzeTracks(selectedMasterTrack())
    }

    fun replaceTrack(trackId: String, uri: Uri) {
        val selectedId = trackId
        val master = selectedMasterTrack() ?: return
        val existing = master.tracks.firstOrNull { it.id == selectedId } ?: return
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment ?: existing.name
        recordTimelineEdit()
        updateSelectedMaster { current ->
            current.copy(tracks = current.tracks.map { track ->
                if (track.id != selectedId) track else track.copy(name = name, sourceUri = uri.toString(), sourceMetadata = null)
            })
        }
        existing.sourceUri?.let(analysisCache::remove)
        _state.update { it.copy(message = uiText("Audio del stem reemplazado", "Stem audio replaced")) }
        invalidateAudio()
        saveNow()
        analyzeTracks(selectedMasterTrack())
    }

    fun createEmptyTrack(name: String, durationSeconds: Double) {
        val master = selectedMasterTrack() ?: return
        if (master.tracks.size >= 16) {
            _state.update { it.copy(message = uiText("Máximo de 16 stems", "Maximum of 16 stems")) }
            return
        }
        val durationFrames = (durationSeconds.coerceIn(0.001, 24.0 * 60.0 * 60.0) * TIMELINE_SAMPLE_RATE)
            .roundToLong().coerceAtLeast(1)
        val track = Track.create(
            UUID.randomUUID().toString(),
            name.trim().ifBlank { uiText("Stem vacío", "Empty stem") },
            _state.value.settings.defaultStemType,
        ).copy(sourceMetadata = SourceMetadata(1, TIMELINE_SAMPLE_RATE, durationFrames))
        recordTimelineEdit()
        updateSelectedMaster { it.copy(tracks = it.tracks + track) }
        _state.update {
            it.copy(
                workspace = Workspace.TRACK,
                trackWorkspace = TrackWorkspace.TIMELINE,
                selectedTimelineTrackId = track.id,
                message = "${uiText("Stem vacío agregado", "Empty stem added")} · ${"%.3f".format(durationSeconds)} s",
            )
        }
        invalidateAudio()
        saveNow()
    }

    fun removeTrack(trackId: String) {
        if (selectedMasterTrack()?.tracks?.none { it.id == trackId } != false) return
        recordTimelineEdit()
        updateSelectedMaster {
            it.copy(
                tracks = it.tracks.filterNot { track -> track.id == trackId },
                clickReferenceTrackId = it.clickReferenceTrackId.takeUnless { reference -> reference == trackId },
            )
        }
        _state.update { current ->
            current.copy(selectedTimelineTrackId = if (current.selectedTimelineTrackId == trackId) current.tracks.firstOrNull()?.id else current.selectedTimelineTrackId)
        }
        invalidateAudio(); saveNow()
    }

    fun selectTimelineTrack(trackId: String) {
        if (_state.value.tracks.any { it.id == trackId }) _state.update { it.copy(selectedTimelineTrackId = trackId) }
    }

    fun cycleTrackType(trackId: String) {
        updateSelectedMaster { master ->
            val updatedTracks = master.tracks.map { track ->
                if (track.id != trackId) track else {
                    val type = when (track.type) { TrackType.MUSIC -> TrackType.CLICK; TrackType.CLICK -> TrackType.CUE; else -> TrackType.MUSIC }
                    track.copy(type = type, mainSendDb = if (type == TrackType.MUSIC) 0f else SILENCE_DB, monitorSendDb = if (type == TrackType.MUSIC) -6f else 0f)
                }
            }
            master.copy(
                tracks = updatedTracks,
                clickReferenceTrackId = master.clickReferenceTrackId.takeUnless { it == trackId },
            )
        }
        invalidateAudio(); saveNow()
    }

    fun setTempoGridVisible(visible: Boolean) {
        updateSelectedMaster { it.copy(tempoGridVisible = visible) }
        scheduleSave()
    }

    fun toggleSelectedTrackAsClickReference() {
        val master = selectedMasterTrack() ?: return
        val selectedId = _state.value.selectedTimelineTrackId ?: return
        if (master.tracks.none { it.id == selectedId }) return
        recordTimelineEdit()
        val removing = master.clickReferenceTrackId == selectedId
        updateSelectedMaster { current ->
            current.copy(
                tracks = current.tracks.map { track ->
                    if (track.id != selectedId || removing) track else track.copy(
                        type = TrackType.CLICK,
                        mainSendDb = SILENCE_DB,
                        monitorSendDb = 0f,
                        muted = false,
                    )
                },
                clickReferenceTrackId = if (removing) null else selectedId,
            )
        }
        _state.update {
            it.copy(message = if (removing) {
                uiText("Referencia de click desactivada", "Click reference disabled")
            } else {
                uiText("Stem usado como referencia de click · sólo MONITOR", "Stem used as click reference · MONITOR only")
            })
        }
        invalidateAudio(); saveNow()
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
            _state.update { it.copy(message = uiText("Máximo de 16 stems", "Maximum of 16 stems")) }
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
            _state.update { it.copy(message = uiText("Ubica el cursor dentro del stem seleccionado", "Place the playhead inside the selected stem")) }
            return
        }
        recordTimelineEdit()
        val updated = master.tracks.toMutableList().apply {
            this[index] = split.first
            add(index + 1, split.second)
        }
        updateSelectedMaster { it.copy(tracks = updated) }
        _state.update { it.copy(selectedTimelineTrackId = split.second.id, message = uiText("Stem dividido en el cursor", "Stem split at playhead")) }
        invalidateAudio(); saveNow()
    }

    fun extractSelectedTrackToNewMaster(name: String) {
        val project = selectedProject() ?: return
        val sourceMasterId = _state.value.selectedMasterTrackId ?: return
        val selectedTrackId = _state.value.selectedTimelineTrackId ?: return
        val result = project.extractTrackAsMaster(
            sourceMasterId = sourceMasterId,
            trackId = selectedTrackId,
            newMasterId = UUID.randomUUID().toString(),
            newMasterName = name,
        ) ?: return
        recordTimelineEdit()
        val (updatedProject, newMaster) = result
        val projects = _state.value.projects.map { if (it.id == project.id) updatedProject else it }
        _state.update {
            it.copy(
                projects = projects,
                selectedMasterTrackId = newMaster.id,
                selectedTimelineTrackId = newMaster.tracks.single().id,
                timelineCursorFrames = 0,
                tracks = mixerTracks(newMaster, analysisCache),
                message = uiText("Clip movido a una pista master independiente", "Clip moved to an independent master track"),
            )
        }
        invalidateAudio(); saveNow(); updateTimelineHistoryAvailability()
    }

    fun createTimelineMarker(
        label: String,
        kind: TimelineMarkerKind,
        voiceCueEnabled: Boolean,
        voiceLeadBeats: Int,
    ) {
        val master = selectedMasterTrack() ?: return
        if (master.markers.size >= MAX_TIMELINE_MARKERS || label.isBlank()) return
        val marker = TimelineMarker(
            id = UUID.randomUUID().toString(),
            label = label.trim(),
            positionFrames = _state.value.timelineCursorFrames,
            kind = kind,
            voiceCueEnabled = voiceCueEnabled,
            voiceLeadBeats = voiceLeadBeats.coerceIn(0, 16),
        )
        recordTimelineEdit()
        updateSelectedMaster { it.copy(markers = (it.markers + marker).sortedBy(TimelineMarker::positionFrames)) }
        saveNow()
        if (voiceCueEnabled) renderVoiceCue(marker)
        _state.update { it.copy(message = "${uiText("Marca", "Marker")} ${marker.label} ${uiText("agregada", "added")}") }
    }

    fun updateTimelineMarker(
        markerId: String,
        label: String,
        kind: TimelineMarkerKind,
        voiceCueEnabled: Boolean,
        voiceLeadBeats: Int,
    ) {
        val previous = selectedMasterTrack()?.markers?.firstOrNull { it.id == markerId } ?: return
        if (label.isBlank()) return
        val updated = previous.copy(
            label = label.trim(),
            kind = kind,
            voiceCueEnabled = voiceCueEnabled,
            voiceLeadBeats = voiceLeadBeats.coerceIn(0, 16),
        )
        recordTimelineEdit()
        updateSelectedMaster { master -> master.copy(markers = master.markers.map { if (it.id == markerId) updated else it }) }
        if (voiceCueEnabled) renderVoiceCue(updated) else {
            voiceCueRenderer.remove(markerId)
            _state.update { it.copy(failedVoiceCueIds = it.failedVoiceCueIds - markerId) }
        }
        invalidateAudio(); saveNow()
    }

    fun setTimelineMarkerPosition(markerId: String, positionFrames: Long) {
        val master = selectedMasterTrack() ?: return
        val current = master.markers.firstOrNull { it.id == markerId } ?: return
        val durationFrames = (master.durationSeconds() * TIMELINE_SAMPLE_RATE).roundToLong()
        val next = positionFrames.coerceIn(0, durationFrames.coerceAtLeast(current.positionFrames))
        if (next == current.positionFrames) return
        recordTimelineEdit()
        updateSelectedMaster { value ->
            value.copy(markers = value.markers.map { if (it.id == markerId) it.copy(positionFrames = next) else it }
                .sortedBy(TimelineMarker::positionFrames))
        }
        invalidateAudio(); saveNow()
    }

    fun deleteTimelineMarker(markerId: String) {
        if (selectedMasterTrack()?.markers?.none { it.id == markerId } != false) return
        recordTimelineEdit()
        updateSelectedMaster { it.copy(markers = it.markers.filterNot { marker -> marker.id == markerId }) }
        voiceCueRenderer.remove(markerId)
        _state.update { it.copy(renderingVoiceCueIds = it.renderingVoiceCueIds - markerId, failedVoiceCueIds = it.failedVoiceCueIds - markerId) }
        invalidateAudio(); saveNow()
    }

    private fun renderVoiceCue(marker: TimelineMarker) {
        _state.update {
            it.copy(renderingVoiceCueIds = it.renderingVoiceCueIds + marker.id, failedVoiceCueIds = it.failedVoiceCueIds - marker.id)
        }
        viewModelScope.launch {
            runCatching { voiceCueRenderer.render(marker.id, marker.label, _state.value.settings.language) }
                .onSuccess {
                    _state.update { state -> state.copy(renderingVoiceCueIds = state.renderingVoiceCueIds - marker.id) }
                    invalidateAudio()
                }
                .onFailure { failure ->
                    _state.update { state ->
                        state.copy(
                            renderingVoiceCueIds = state.renderingVoiceCueIds - marker.id,
                            failedVoiceCueIds = state.failedVoiceCueIds + marker.id,
                            message = (failure as? VoiceCueException)?.message
                                ?: uiText("No se pudo generar el cue de voz", "Could not render the voice cue"),
                        )
                    }
                }
        }
    }

    fun undoTimelineEdit() {
        val target = undoTimelineEdits.pollLast() ?: return
        val current = timelineSnapshot() ?: return
        if (target.projectId != current.projectId) {
            clearTimelineHistory()
            return
        }
        pushTimelineSnapshot(redoTimelineEdits, current)
        restoreTimelineSnapshot(target, uiText("Deshacer aplicado", "Undo applied"))
    }

    fun redoTimelineEdit() {
        val target = redoTimelineEdits.pollLast() ?: return
        val current = timelineSnapshot() ?: return
        if (target.projectId != current.projectId) {
            clearTimelineHistory()
            return
        }
        pushTimelineSnapshot(undoTimelineEdits, current)
        restoreTimelineSnapshot(target, uiText("Rehacer aplicado", "Redo applied"))
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
            val exclusiveReady = if (play) performanceMode.activate(current.settings.exclusivePerformanceMode) else true
            if (!play) performanceMode.deactivate()
            audio.setToneEnabled(play)
            _state.update { it.copy(diagnostics = audio.diagnostics(requestedSampleRate), message = when {
                !play -> uiText("PAUSA", "PAUSED")
                !exclusiveReady -> uiText(
                    "REPRODUCIENDO · concede acceso a No molestar para el modo exclusivo",
                    "PLAYING · grant Do Not Disturb access for exclusive mode",
                )
                else -> uiText("REPRODUCIENDO", "PLAYING")
            }) }
            return
        }
        val project = selectedProject()
        val master = selectedMasterTrack()
        if (project == null || master == null) {
            _state.update { it.copy(workspace = Workspace.PLAYLIST, message = uiText("Selecciona una pista de la playlist", "Select a track from the playlist")) }; return
        }
        if (master.tracks.isEmpty()) {
            _state.update { it.copy(workspace = Workspace.TRACK, message = uiText("Importa al menos un stem de audio", "Import at least one audio stem")) }; return
        }
        _state.update { it.copy(openingOutput = true, message = "${uiText("Preparando", "Preparing")} ${master.tracks.size} stems...") }
        viewModelScope.launch(Dispatchers.IO) {
            // Complete DND/exclusive transitions before creating the Oboe stream.
            // Some Android builds stop an already-open stream for the transition.
            val exclusiveReady = performanceMode.activate(current.settings.exclusivePerformanceMode)
            requestedSampleRate = hardware.nativeOutputSampleRate()
            var diagnostics = audio.open(requestedSampleRate)
            var error: String? = if (!diagnostics.outputOpen) diagnostics.lastError else null
            var exclusiveWarning = !exclusiveReady
            val metadata = mutableMapOf<String, SourceMetadata>()
            val voiceMarkers = master.markers
                .filter(TimelineMarker::voiceCueEnabled)
                .sortedBy { it.voiceCueStartFrames(master.metronome(project.defaultMetronome)) }
                .take(MAX_TIMELINE_MARKERS)
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
                        error = "${track.name}: ${decodeError ?: uiText("audio no compatible (código $result)", "unsupported audio (code $result)")}"
                    }
                }
                voiceMarkers.forEachIndexed { cueIndex, marker ->
                    if (error != null) return@forEachIndexed
                    val file = voiceCueRenderer.cueFile(marker.id)
                    if (!file.isFile) {
                        error = uiText(
                            "Cue de voz ${marker.label}: genera o desactiva la voz antes de reproducir",
                            "Voice cue ${marker.label}: render or disable the voice before playback",
                        )
                        return@forEachIndexed
                    }
                    val index = master.tracks.size + cueIndex
                    val result = runCatching {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { audio.loadWavTrack(index, it.fd) }
                    }.getOrDefault(-9)
                    if (result != 0) error = uiText(
                        "Cue de voz ${marker.label}: audio no compatible (código $result)",
                        "Voice cue ${marker.label}: unsupported audio (code $result)",
                    )
                }
            }
            if (error == null) {
                val inspected = master.copy(tracks = master.tracks.map { track -> metadata[track.id]?.let { track.copy(sourceMetadata = it) } ?: track })
                replaceSelectedMaster(inspected)
                repository.replaceProjects(_state.value.projects)
                configureNativeMixer(project, inspected, voiceMarkers)
                val outputRate = diagnostics.actualSampleRate.takeIf { it > 0 } ?: requestedSampleRate
                if (outputRate > 0 && _state.value.timelineCursorFrames > 0) {
                    audio.seekTransport((_state.value.timelineCursorFrames.toDouble() * outputRate / TIMELINE_SAMPLE_RATE).roundToLong())
                }
                audio.setToneEnabled(true)
                loadedSelectionKey = selectionKey()
                diagnostics = audio.diagnostics(requestedSampleRate)
            } else {
                audio.resetTransport(); performanceMode.deactivate(); loadedSelectionKey = null
            }
            _state.update {
                it.copy(openingOutput = false, diagnostics = diagnostics,
                    safetyStatus = if (error == null) SafetyStatus.WARNING else SafetyStatus.UNSAFE,
                    message = error ?: if (exclusiveWarning) {
                        uiText(
                            "REPRODUCIENDO · concede acceso a No molestar para el modo exclusivo",
                            "PLAYING · grant Do Not Disturb access for exclusive mode",
                        )
                    } else "${uiText("REPRODUCIENDO", "PLAYING")} · ${master.name}")
            }
        }
    }

    fun stop() {
        audio.resetTransport()
        performanceMode.deactivate()
        _state.update { it.copy(diagnostics = audio.diagnostics(requestedSampleRate), timelineCursorFrames = 0,
            tracks = it.tracks.map { t -> t.copy(peak = 0f) }, message = "${uiText("DETENIDO", "STOPPED")} · 00:00") }
    }

    fun seekToFraction(fraction: Float) {
        val timelineDuration = ((selectedMasterTrack()?.durationSeconds() ?: 0.0) * TIMELINE_SAMPLE_RATE).roundToLong()
        setTimelineCursor((timelineDuration * fraction.coerceIn(0f, 1f)).roundToLong())
    }

    fun panic() {
        audio.panic()
        performanceMode.deactivate()
        _state.update { it.copy(diagnostics = audio.diagnostics(requestedSampleRate), safetyStatus = SafetyStatus.UNSAFE,
            message = uiText("MUTE ALL · revalidación requerida", "MUTE ALL · revalidation required")) }
    }

    fun setTrackGain(index: Int, gainDb: Float) { updateTrack(index) { it.copy(gainDb = gainDb) }; audio.setTrackGain(index, AudioMath.dbToLinear(gainDb)) }
    fun setTrackPan(index: Int, pan: Float) { updateTrack(index) { it.copy(pan = pan) }; audio.setTrackPan(index, pan) }
    fun toggleMute(index: Int) { val next = !_state.value.tracks.getOrNull(index)?.muted.orFalse(); updateTrack(index) { it.copy(muted = next) }; audio.setTrackMuted(index, next) }
    fun toggleSolo(index: Int) { val next = !_state.value.tracks.getOrNull(index)?.soloed.orFalse(); updateTrack(index) { it.copy(soloed = next) }; audio.setTrackSoloed(index, next) }

    fun setProjectGain(value: Float) { updateSelectedProject { it.copy(masterGainDb = value.coerceIn(-60f, 6f)) }; applyLiveMaster(); scheduleSave() }
    fun setProjectPan(value: Float) { updateSelectedProject { it.copy(masterPan = value.coerceIn(-1f, 1f)) }; applyLiveMaster(); scheduleSave() }
    fun setMasterGain(value: Float) { updateSelectedMaster { it.copy(gainDb = value.coerceIn(-60f, 6f)) }; applyLiveMaster(); scheduleSave() }
    fun setMasterPan(value: Float) { updateSelectedMaster { it.copy(pan = value.coerceIn(-1f, 1f)) }; applyLiveMaster(); scheduleSave() }

    fun setProjectGain(projectId: String, value: Float) {
        updateProject(projectId) { it.copy(masterGainDb = value.coerceIn(-60f, 6f)) }
        if (projectId == _state.value.selectedProjectId) applyLiveMaster()
        scheduleSave()
    }

    fun setProjectPan(projectId: String, value: Float) {
        updateProject(projectId) { it.copy(masterPan = value.coerceIn(-1f, 1f)) }
        if (projectId == _state.value.selectedProjectId) applyLiveMaster()
        scheduleSave()
    }

    fun setMasterGain(masterId: String, value: Float) {
        updateMaster(masterId) { it.copy(gainDb = value.coerceIn(-60f, 6f)) }
        if (masterId == _state.value.selectedMasterTrackId) applyLiveMaster()
        scheduleSave()
    }

    fun setMasterPan(masterId: String, value: Float) {
        updateMaster(masterId) { it.copy(pan = value.coerceIn(-1f, 1f)) }
        if (masterId == _state.value.selectedMasterTrackId) applyLiveMaster()
        scheduleSave()
    }

    fun setStereoSplit(enabled: Boolean) {
        audio.setOutputMode(enabled)
        _state.update { it.copy(stereoSplit = enabled, message = if (enabled) "STEREO SPLIT · L MAIN / R MONITOR" else "SINGLE MIX") }
    }

    fun setLanguage(value: AppLanguage) {
        if (_state.value.settings.language == value) return
        updateSettings { it.copy(language = value) }
        _state.update { it.copy(message = uiText("Idioma actualizado", "Language updated")) }
        _state.value.projects.asSequence()
            .flatMap { it.playlist.asSequence() }
            .flatMap { it.markers.asSequence() }
            .filter(TimelineMarker::voiceCueEnabled)
            .forEach(::renderVoiceCue)
    }
    fun setKeepScreenAwake(value: Boolean) = updateSettings { it.copy(keepScreenAwake = value) }
    fun setExclusivePerformanceMode(value: Boolean) {
        updateSettings { it.copy(exclusivePerformanceMode = value) }
        if (!value) performanceMode.deactivate()
        else if (!performanceMode.hasNotificationPolicyAccess) performanceMode.openNotificationPolicyAccessSettings()
        refreshProfessionalModeAccess()
    }

    fun refreshProfessionalModeAccess() = _state.update {
        it.copy(notificationPolicyAccessGranted = performanceMode.hasNotificationPolicyAccess)
    }
    fun setConfirmDestructiveActions(value: Boolean) = updateSettings { it.copy(confirmDestructiveActions = value) }
    fun setDefaultStemType(value: TrackType) = updateSettings { it.copy(defaultStemType = value) }
    fun setDefaultMonitorSendDb(value: Float) = updateSettings { it.copy(defaultMonitorSendDb = value.coerceIn(-60f, 0f)) }
    fun setOpenTimelineAfterImport(value: Boolean) = updateSettings { it.copy(openTimelineAfterImport = value) }
    fun setTimelineSnapEnabled(value: Boolean) = updateSettings { it.copy(timelineSnapEnabled = value) }
    fun setAutomaticUpdateChecks(value: Boolean) = updateSettings { it.copy(automaticUpdateChecks = value) }
    fun setIncludePrereleaseUpdates(value: Boolean) {
        updateSettings { it.copy(includePrereleaseUpdates = value) }
        checkForUpdates(manual = true)
    }

    fun checkForUpdates(manual: Boolean = true) {
        if (updateJob?.isActive == true) return
        _state.update { it.copy(appUpdateStatus = AppUpdateStatus.Checking) }
        updateJob = viewModelScope.launch {
            try {
                val release = releaseRepository.latest(_state.value.settings.includePrereleaseUpdates)
                val status = when {
                    release == null && manual -> AppUpdateStatus.Failed(UpdateFailure.NO_RELEASE)
                    release == null -> AppUpdateStatus.Idle
                    isNewerRelease(BuildConfig.VERSION_CODE.toLong(), release) -> AppUpdateStatus.Available(release)
                    else -> AppUpdateStatus.UpToDate(BuildConfig.VERSION_NAME)
                }
                _state.update { it.copy(appUpdateStatus = status) }
            } catch (failure: UpdateRepositoryException) {
                if (manual) {
                    _state.update {
                        it.copy(appUpdateStatus = AppUpdateStatus.Failed(failure.failure, failure.message))
                    }
                } else {
                    _state.update { it.copy(appUpdateStatus = AppUpdateStatus.Idle) }
                }
            } catch (failure: Exception) {
                if (manual) {
                    _state.update {
                        it.copy(appUpdateStatus = AppUpdateStatus.Failed(UpdateFailure.UNKNOWN, failure.message))
                    }
                } else {
                    _state.update { it.copy(appUpdateStatus = AppUpdateStatus.Idle) }
                }
            }
        }
    }

    fun downloadUpdate() {
        if (updateJob?.isActive == true) return
        val release = when (val status = _state.value.appUpdateStatus) {
            is AppUpdateStatus.Available -> status.release
            is AppUpdateStatus.Failed -> status.release
            else -> null
        } ?: return
        updateJob = viewModelScope.launch {
            try {
                _state.update {
                    it.copy(appUpdateStatus = AppUpdateStatus.Downloading(release, null, 0, null))
                }
                val file = updateInstaller.download(release) { progress ->
                    _state.update {
                        it.copy(
                            appUpdateStatus = AppUpdateStatus.Downloading(
                                release = release,
                                progress = progress.fraction,
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = progress.totalBytes,
                            ),
                        )
                    }
                }
                _state.update { it.copy(appUpdateStatus = AppUpdateStatus.Verifying(release)) }
                updateInstaller.validate(file, release)
                _state.update { it.copy(appUpdateStatus = AppUpdateStatus.ReadyToInstall(release)) }
            } catch (failure: UpdateInstallException) {
                _state.update {
                    it.copy(appUpdateStatus = AppUpdateStatus.Failed(failure.failure, failure.message, release))
                }
            } catch (failure: Exception) {
                _state.update {
                    it.copy(appUpdateStatus = AppUpdateStatus.Failed(UpdateFailure.UNKNOWN, failure.message, release))
                }
            }
        }
    }

    fun installUpdate() {
        val release = when (val status = _state.value.appUpdateStatus) {
            is AppUpdateStatus.ReadyToInstall -> status.release
            is AppUpdateStatus.InstallPermissionRequired -> status.release
            is AppUpdateStatus.InstallerOpened -> status.release
            else -> null
        } ?: return
        if (!updateInstaller.canRequestInstalls()) {
            _state.update { it.copy(appUpdateStatus = AppUpdateStatus.InstallPermissionRequired(release)) }
            return
        }
        runCatching { updateInstaller.openInstaller(release) }
            .onSuccess { _state.update { it.copy(appUpdateStatus = AppUpdateStatus.InstallerOpened(release)) } }
            .onFailure { failure ->
                val typed = failure as? UpdateInstallException
                _state.update {
                    it.copy(
                        appUpdateStatus = AppUpdateStatus.Failed(
                            typed?.failure ?: UpdateFailure.INSTALLER_UNAVAILABLE,
                            failure.message,
                            release,
                        ),
                    )
                }
            }
    }

    fun openInstallPermissionSettings() {
        val release = (_state.value.appUpdateStatus as? AppUpdateStatus.InstallPermissionRequired)?.release ?: return
        runCatching { updateInstaller.openInstallPermissionSettings() }.onFailure { failure ->
            val typed = failure as? UpdateInstallException
            _state.update {
                it.copy(
                    appUpdateStatus = AppUpdateStatus.Failed(
                        typed?.failure ?: UpdateFailure.INSTALLER_UNAVAILABLE,
                        failure.message,
                        release,
                    ),
                )
            }
        }
    }

    fun refreshInstallPermission() {
        val release = (_state.value.appUpdateStatus as? AppUpdateStatus.InstallPermissionRequired)?.release ?: return
        if (updateInstaller.canRequestInstalls()) {
            _state.update { it.copy(appUpdateStatus = AppUpdateStatus.ReadyToInstall(release)) }
        }
    }

    fun updateMasterMetronome(transform: (MetronomeSettings) -> MetronomeSettings) {
        val project = selectedProject() ?: return
        updateSelectedMaster { it.copy(metronomeOverride = transform(it.metronome(project.defaultMetronome))) }
        applyLiveMetronome(); scheduleSave()
    }

    fun updateDefaultMetronome(transform: (MetronomeSettings) -> MetronomeSettings) {
        updateSelectedProject { it.copy(defaultMetronome = transform(it.defaultMetronome)) }
        scheduleSave()
    }

    private fun configureNativeMixer(project: Project, master: MasterTrack, voiceMarkers: List<TimelineMarker> = emptyList()) {
        val outputRate = _state.value.diagnostics.actualSampleRate.takeIf { it > 0 } ?: requestedSampleRate.takeIf { it > 0 } ?: TIMELINE_SAMPLE_RATE
        master.tracks.forEachIndexed { index, track ->
            val clickReference = track.id == master.clickReferenceTrackId
            audio.setTrackGain(index, AudioMath.dbToLinear(track.gainDb)); audio.setTrackPan(index, track.pan)
            audio.setTrackMuted(index, track.muted); audio.setTrackSoloed(index, track.soloed)
            audio.setTrackSoloSafe(index, false)
            audio.setTrackSends(
                index,
                if (clickReference) 0f else AudioMath.dbToLinear(track.mainSendDb),
                if (clickReference) AudioMath.dbToLinear(track.monitorSendDb.coerceAtLeast(-60f)) else AudioMath.dbToLinear(track.monitorSendDb),
            )
            audio.setTrackStartOffset(index, (track.startSeconds() * outputRate).toLong())
            audio.setTrackSourceRange(index, track.sourceStartFrame, track.sourceEndFrameExclusive ?: -1)
        }
        val metronome = master.metronome(project.defaultMetronome)
        voiceMarkers.forEachIndexed { cueIndex, marker ->
            val index = master.tracks.size + cueIndex
            audio.setTrackGain(index, AudioMath.dbToLinear(VOICE_CUE_GAIN_DB))
            audio.setTrackPan(index, 0f)
            audio.setTrackMuted(index, false)
            audio.setTrackSoloed(index, false)
            audio.setTrackSoloSafe(index, true)
            audio.setTrackSends(index, 0f, 1f)
            val cueStart = marker.voiceCueStartFrames(metronome)
            audio.setTrackStartOffset(index, (cueStart.toDouble() * outputRate / TIMELINE_SAMPLE_RATE).roundToLong())
            audio.setTrackSourceRange(index, 0, -1)
        }
        audio.setTimelineDuration((master.durationSeconds() * outputRate).roundToLong())
        val combinedGainDb = (project.masterGainDb + master.gainDb).coerceIn(-120f, 6f)
        audio.setMasterGainPan(AudioMath.dbToLinear(combinedGainDb), (project.masterPan + master.pan).coerceIn(-1f, 1f))
        audio.setOutputMode(_state.value.stereoSplit)
        configureNativeMetronome(metronome, nativeClickAllowed = master.clickReferenceTrackId == null)
    }

    private fun configureNativeMetronome(value: MetronomeSettings, nativeClickAllowed: Boolean = true) = audio.configureMetronome(
        value.enabled && nativeClickAllowed, value.bpm, value.numerator, value.denominator, AudioMath.dbToLinear(value.gainDb), value.mainEnabled,
    )

    private fun applyLiveMaster() {
        if (loadedSelectionKey != selectionKey()) return
        val project = selectedProject() ?: return; val master = selectedMasterTrack() ?: return
        audio.setMasterGainPan(AudioMath.dbToLinear((project.masterGainDb + master.gainDb).coerceIn(-120f, 6f)), (project.masterPan + master.pan).coerceIn(-1f, 1f))
    }

    private fun applyLiveMetronome() {
        if (loadedSelectionKey != selectionKey()) return
        val project = selectedProject() ?: return; val master = selectedMasterTrack() ?: return
        configureNativeMetronome(master.metronome(project.defaultMetronome), nativeClickAllowed = master.clickReferenceTrackId == null)
    }

    private fun updateTrack(index: Int, transform: (Track) -> Track) {
        val id = _state.value.tracks.getOrNull(index)?.id ?: return
        updateSelectedTracks { if (it.id == id) transform(it) else it }; scheduleSave()
    }

    private fun updateSelectedTracks(transform: (Track) -> Track) = updateSelectedMaster { it.copy(tracks = it.tracks.map(transform)) }

    private fun updateProject(projectId: String, transform: (Project) -> Project) {
        _state.update { current ->
            val projects = current.projects.map { if (it.id == projectId) transform(it) else it }
            val selectedMaster = projects.firstOrNull { it.id == current.selectedProjectId }
                ?.playlist?.firstOrNull { it.id == current.selectedMasterTrackId }
            current.copy(projects = projects, tracks = mixerTracks(selectedMaster, analysisCache))
        }
    }

    private fun updateMaster(masterId: String, transform: (MasterTrack) -> MasterTrack) {
        updateSelectedProject { project ->
            project.copy(playlist = project.playlist.map { if (it.id == masterId) transform(it) else it })
        }
    }

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
        val project = selectedProject() ?: return null
        return TimelineEditSnapshot(
            projectId = projectId,
            playlist = project.playlist,
            selectedMasterTrackId = _state.value.selectedMasterTrackId,
            selectedTrackId = _state.value.selectedTimelineTrackId,
        )
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
        val project = selectedProject() ?: return
        val restoredProject = project.copy(playlist = snapshot.playlist)
        val selectedMaster = snapshot.selectedMasterTrackId
            ?.let { id -> restoredProject.playlist.firstOrNull { it.id == id } }
            ?: restoredProject.playlist.firstOrNull()
        val selectedId = snapshot.selectedTrackId?.takeIf { id -> selectedMaster?.tracks?.any { it.id == id } == true }
            ?: selectedMaster?.tracks?.firstOrNull()?.id
        val projects = _state.value.projects.map { if (it.id == snapshot.projectId) restoredProject else it }
        _state.update {
            it.copy(
                projects = projects,
                selectedMasterTrackId = selectedMaster?.id,
                selectedTimelineTrackId = selectedId,
                tracks = mixerTracks(selectedMaster, analysisCache),
                message = message,
            )
        }
        invalidateAudio()
        saveNow()
        analyzeTracks(selectedMasterTrack())
        selectedMaster?.markers?.filter(TimelineMarker::voiceCueEnabled)?.forEach(::renderVoiceCue)
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
                val analysis = analysisCache[uri]
                    ?: waveformCache.read(uri)?.let { WavAnalysis(it.metadata, it.peaks) }
                    ?: runCatching {
                    resolver.openFileDescriptor(Uri.parse(uri), "r")?.let { descriptor ->
                        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input -> WavAnalyzer.analyze(input) }
                    }
                }.recoverCatching {
                    FileInputStream(androidAudioDecoder.decodedWav(Uri.parse(uri))).use(WavAnalyzer::analyze)
                }.getOrNull()?.also { analyzed -> waveformCache.write(uri, analyzed.metadata, analyzed.peaks) }
                analysis?.also { analysisCache[uri] = it }
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

    private fun invalidateAudio(preservePerformanceMode: Boolean = false) {
        audio.resetTransport()
        if (!preservePerformanceMode) performanceMode.deactivate()
        audio.clearTracks()
        loadedSelectionKey = null
    }
    private fun uiText(spanish: String, english: String): String =
        if (_state.value.settings.language == AppLanguage.SPANISH) spanish else english

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
            audio.panic(); performanceMode.deactivate(); audio.close(); loadedSelectionKey = null
            _state.update { it.copy(devices = devices, diagnostics = EngineDiagnostics(requestedSampleRate = requestedSampleRate), safetyStatus = SafetyStatus.UNSAFE,
                message = uiText("RUTA CAMBIÓ · salida detenida", "ROUTE CHANGED · output stopped")) }
        } else _state.update { it.copy(devices = devices) }
    }

    override fun onCleared() { hardware.stop(); performanceMode.deactivate(); audio.close(); super.onCleared() }
}

private const val TIMELINE_HISTORY_LIMIT = 50
private const val MAX_TIMELINE_MARKERS = 32
private const val VOICE_CUE_GAIN_DB = -3f

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
        startOffsetFrames = track.startOffsetFrames, durationSeconds = track.durationSeconds(), hasAudioSource = track.sourceUri != null,
        isClickReference = track.id == master.clickReferenceTrackId,
        waveformPeaks = waveform,
    )
} ?: emptyList()

private fun Boolean?.orFalse() = this ?: false
