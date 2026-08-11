package com.thomrnowtea.livetracks.data

import com.thomrnowtea.livetracks.domain.MasterTrack
import com.thomrnowtea.livetracks.domain.MetronomeSettings
import com.thomrnowtea.livetracks.domain.Project
import com.thomrnowtea.livetracks.domain.SourceMetadata
import com.thomrnowtea.livetracks.domain.Track
import com.thomrnowtea.livetracks.domain.TrackType
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface ProjectRepository {
    suspend fun getProjects(): List<Project>
    suspend fun replaceProjects(projects: List<Project>)
}

class FileProjectRepository(
    private val target: File,
    private val codec: ProjectStoreCodec = ProjectStoreCodec(),
) : ProjectRepository {
    private val mutex = Mutex()

    override suspend fun getProjects(): List<Project> = mutex.withLock {
        withContext(Dispatchers.IO) { if (!target.exists()) emptyList() else codec.decode(target.readText()) }
    }

    override suspend fun replaceProjects(projects: List<Project>): Unit = mutex.withLock {
        withContext(Dispatchers.IO) {
            target.parentFile?.mkdirs()
            val temp = File(target.parentFile, "${target.name}.tmp")
            temp.writeText(codec.encode(projects))
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

class ProjectStoreCodec {
    fun encode(projects: List<Project>): String = buildString {
        appendLine("LIVETRACKS\t$SCHEMA_VERSION")
        projects.forEach { project ->
            appendLine(listOf("PROJECT", field(project.id), field(project.name), project.masterGainDb, project.masterPan) + encodeMetronome(project.defaultMetronome))
            project.playlist.forEach { master ->
                appendLine(
                    (listOf("MASTER", field(master.id), field(master.name), master.gainDb, master.pan, master.metronomeOverride != null) +
                        encodeMetronome(master.metronomeOverride ?: project.defaultMetronome)).joinToString("\t"),
                )
                master.tracks.forEach { appendLine(encodeTrack(it)) }
                appendLine("ENDMASTER")
            }
            appendLine("ENDPROJECT")
        }
    }

    private fun StringBuilder.appendLine(fields: List<Any>) = appendLine(fields.joinToString("\t"))

    fun decode(text: String): List<Project> {
        val lines = text.lineSequence().filter(String::isNotBlank).toList()
        val schema = lines.firstOrNull()?.substringAfter("LIVETRACKS\t", "")?.toIntOrNull()
            ?: throw IllegalArgumentException("Unsupported library schema")
        require(schema in 1..SCHEMA_VERSION) { "Unsupported library schema" }
        return if (schema >= 3) decodeModern(lines) else migrateLegacy(lines, schema)
    }

    private fun decodeModern(lines: List<String>): List<Project> {
        val projects = mutableListOf<Project>()
        var index = 1
        while (index < lines.size) {
            val projectFields = lines[index++].split('\t')
            require(projectFields.size == 11 && projectFields[0] == "PROJECT") { "Corrupt project record" }
            val playlist = mutableListOf<MasterTrack>()
            while (index < lines.size && lines[index] != "ENDPROJECT") {
                val masterFields = lines[index++].split('\t')
                require(masterFields.size == 12 && masterFields[0] == "MASTER") { "Corrupt master record" }
                val tracks = mutableListOf<Track>()
                while (index < lines.size && lines[index] != "ENDMASTER") tracks += decodeTrack(lines[index++])
                require(index < lines.size && lines[index++] == "ENDMASTER") { "Missing master terminator" }
                val hasOverride = masterFields[5].toBooleanStrict()
                playlist += MasterTrack(
                    id = unfield(masterFields[1]),
                    name = unfield(masterFields[2]),
                    tracks = tracks,
                    gainDb = masterFields[3].toFloat(),
                    pan = masterFields[4].toFloat(),
                    metronomeOverride = if (hasOverride) decodeMetronome(masterFields, 6) else null,
                )
            }
            require(index < lines.size && lines[index++] == "ENDPROJECT") { "Missing project terminator" }
            projects += Project(
                id = unfield(projectFields[1]),
                name = unfield(projectFields[2]),
                playlist = playlist,
                masterGainDb = projectFields[3].toFloat(),
                masterPan = projectFields[4].toFloat(),
                defaultMetronome = decodeMetronome(projectFields, 5),
            )
        }
        return projects
    }

    private fun migrateLegacy(lines: List<String>, schema: Int): List<Project> {
        val projects = mutableListOf<Project>()
        var index = 1
        while (index < lines.size) {
            val song = lines[index++].split('\t')
            val expected = if (schema == 1) 6 else 9
            require(song.size == expected && song[0] == "SONG") { "Corrupt song record" }
            val tracks = mutableListOf<Track>()
            while (index < lines.size && lines[index] != "END") tracks += decodeTrack(lines[index++])
            require(index < lines.size && lines[index++] == "END") { "Missing song terminator" }
            val legacyMetronome = MetronomeSettings(
                enabled = if (schema == 1) false else song[6].toBooleanStrict(),
                bpm = song[3].toDouble(),
                numerator = song[4].toInt(),
                denominator = song[5].toInt(),
                gainDb = if (schema == 1) -12f else song[7].toFloat(),
                mainEnabled = if (schema == 1) false else song[8].toBooleanStrict(),
            )
            val id = unfield(song[1])
            projects += Project(
                id = id,
                name = unfield(song[2]),
                playlist = listOf(MasterTrack("$id-master-1", "Pista 01", tracks, metronomeOverride = legacyMetronome)),
            )
        }
        return projects
    }

    private fun encodeMetronome(value: MetronomeSettings): List<Any> = listOf(
        value.enabled, value.bpm, value.numerator, value.denominator, value.gainDb, value.mainEnabled,
    )

    private fun decodeMetronome(fields: List<String>, start: Int) = MetronomeSettings(
        enabled = fields[start].toBooleanStrict(), bpm = fields[start + 1].toDouble(),
        numerator = fields[start + 2].toInt(), denominator = fields[start + 3].toInt(),
        gainDb = fields[start + 4].toFloat(), mainEnabled = fields[start + 5].toBooleanStrict(),
    )

    private fun encodeTrack(track: Track): String = listOf(
        "TRACK", field(track.id), field(track.name), optionalField(track.sourceUri),
        track.sourceMetadata?.channelCount ?: -1, track.sourceMetadata?.sampleRate ?: -1,
        track.sourceMetadata?.durationFrames ?: -1, track.sourceStartFrame, track.sourceEndFrameExclusive ?: -1,
        track.startOffsetFrames, track.gainDb, track.pan,
        track.muted, track.soloed, track.enabled, track.mainSendDb, track.monitorSendDb, track.type.name,
    ).joinToString("\t")

    private fun decodeTrack(line: String): Track {
        val value = line.split('\t')
        require(value.size in setOf(16, 18) && value[0] == "TRACK") { "Corrupt track record" }
        val channels = value[4].toInt()
        val modern = value.size == 18
        val offsetIndex = if (modern) 9 else 7
        return Track(
            id = unfield(value[1]), name = unfield(value[2]), sourceUri = optionalUnfield(value[3]),
            sourceMetadata = if (channels < 0) null else SourceMetadata(channels, value[5].toInt(), value[6].toLong()),
            sourceStartFrame = if (modern) value[7].toLong() else 0,
            sourceEndFrameExclusive = if (modern) value[8].toLong().takeIf { it >= 0 } else null,
            startOffsetFrames = value[offsetIndex].toLong(), gainDb = value[offsetIndex + 1].toFloat(), pan = value[offsetIndex + 2].toFloat(),
            muted = value[offsetIndex + 3].toBooleanStrict(), soloed = value[offsetIndex + 4].toBooleanStrict(), enabled = value[offsetIndex + 5].toBooleanStrict(),
            mainSendDb = value[offsetIndex + 6].toFloat(), monitorSendDb = value[offsetIndex + 7].toFloat(), type = TrackType.valueOf(value[offsetIndex + 8]),
        )
    }

    private fun field(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    private fun unfield(value: String) = Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)
    private fun optionalField(value: String?) = value?.let(::field) ?: "-"
    private fun optionalUnfield(value: String) = value.takeUnless { it == "-" }?.let(::unfield)

    companion object { const val SCHEMA_VERSION = 4 }
}
