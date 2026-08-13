package com.thomrnowtea.livetracks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import com.thomrnowtea.livetracks.domain.MasterTrack
import com.thomrnowtea.livetracks.domain.AppUpdateStatus
import com.thomrnowtea.livetracks.domain.MetronomeSettings
import com.thomrnowtea.livetracks.domain.Project
import com.thomrnowtea.livetracks.domain.SafetyStatus
import com.thomrnowtea.livetracks.domain.snapTimelineFrames
import com.thomrnowtea.livetracks.domain.TIMELINE_SAMPLE_RATE
import com.thomrnowtea.livetracks.domain.TrackType
import com.thomrnowtea.livetracks.domain.TimelineMarker
import com.thomrnowtea.livetracks.domain.TimelineMarkerKind
import com.thomrnowtea.livetracks.domain.UpdateFailure
import com.thomrnowtea.livetracks.data.AppLanguage
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LiveTracksRoot() }
    }
}

private val Bg = Color(0xFF151617)
private val Panel = Color(0xFF202224)
private val Raised = Color(0xFF2A2C2F)
private val Border = Color(0xFF414448)
private val TextMain = Color(0xFFDEDFE0)
private val TextMuted = Color(0xFF8E9296)
private val Mint = Color(0xFF6E8FAF)
private val Amber = Color(0xFFC4A15A)
private val Red = Color(0xFFC96060)
private val Blue = Color(0xFF607A9A)
private val Silver = Color(0xFFB8BBBE)
private val MeterGreen = Color(0xFF7FA66A)

private val DawFont = FontFamily(
    Font(R.font.source_sans_3_regular, FontWeight.Normal),
    Font(R.font.source_sans_3_semibold, FontWeight.SemiBold),
    Font(R.font.source_sans_3_semibold, FontWeight.Bold),
)

private val DawTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = DawFont),
        displayMedium = base.displayMedium.copy(fontFamily = DawFont),
        displaySmall = base.displaySmall.copy(fontFamily = DawFont),
        headlineLarge = base.headlineLarge.copy(fontFamily = DawFont),
        headlineMedium = base.headlineMedium.copy(fontFamily = DawFont),
        headlineSmall = base.headlineSmall.copy(fontFamily = DawFont),
        titleLarge = base.titleLarge.copy(fontFamily = DawFont),
        titleMedium = base.titleMedium.copy(fontFamily = DawFont),
        titleSmall = base.titleSmall.copy(fontFamily = DawFont),
        bodyLarge = base.bodyLarge.copy(fontFamily = DawFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = DawFont),
        bodySmall = base.bodySmall.copy(fontFamily = DawFont),
        labelLarge = base.labelLarge.copy(fontFamily = DawFont),
        labelMedium = base.labelMedium.copy(fontFamily = DawFont),
        labelSmall = base.labelSmall.copy(fontFamily = DawFont),
    )
}

private val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SPANISH }

@Composable
private fun tr(spanish: String, english: String): String =
    if (LocalAppLanguage.current == AppLanguage.SPANISH) spanish else english

private val ConsoleColors = darkColorScheme(
    primary = Mint, secondary = Amber, background = Bg, surface = Panel, surfaceVariant = Raised,
    error = Red, onBackground = TextMain, onSurface = TextMain,
)

@Composable
fun LiveTracksRoot(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshInstallPermission()
        viewModel.refreshProfessionalModeAccess()
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { viewModel.importTracks(it) }
    var replacementTrackId by rememberSaveable { mutableStateOf<String?>(null) }
    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val trackId = replacementTrackId
        replacementTrackId = null
        if (uri != null && trackId != null) viewModel.replaceTrack(trackId, uri)
    }
    val view = LocalView.current
    SideEffect { view.keepScreenOn = state.settings.keepScreenAwake }
    MaterialTheme(colorScheme = ConsoleColors, typography = DawTypography) {
        val systemDensity = LocalDensity.current
        val readableDensity = remember(systemDensity.density, systemDensity.fontScale) {
            Density(systemDensity.density, max(systemDensity.fontScale, 1.18f))
        }
        CompositionLocalProvider(
            LocalAppLanguage provides state.settings.language,
            LocalDensity provides readableDensity,
        ) {
        Scaffold(containerColor = Bg) { insets ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(insets).background(Bg)) {
                val wide = maxWidth >= 700.dp
                val addAudio = { filePicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*")) }
                if (state.workspace == Workspace.PLAYLIST && state.playlistPerformanceMode) {
                    PerformancePlaylistScreen(state, viewModel)
                } else if (wide) Row(Modifier.fillMaxSize()) {
                    SideNavigation(state.workspace, viewModel::setWorkspace)
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        CompactContextBar(state, viewModel)
                        WorkspaceContent(state, viewModel, addAudio, { id -> replacementTrackId = id; replacePicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*")) }, Modifier.weight(1f))
                        if (state.workspace != Workspace.SETTINGS) CompactTransport(
                            state, viewModel::skipToPreviousMasterTrack, viewModel::playPause, viewModel::stop,
                            viewModel::skipToNextMasterTrack, viewModel::seekToFraction, viewModel::panic,
                            openTimeline = { viewModel.setWorkspace(Workspace.TRACK); viewModel.setTrackWorkspace(TrackWorkspace.TIMELINE) },
                            openMixer = { viewModel.setWorkspace(Workspace.TRACK); viewModel.setTrackWorkspace(TrackWorkspace.MIXER) },
                            openMetronome = { viewModel.setWorkspace(Workspace.METRONOME) },
                        )
                    }
                } else Column(Modifier.fillMaxSize()) {
                    CompactContextBar(state, viewModel)
                    WorkspaceContent(state, viewModel, addAudio, { id -> replacementTrackId = id; replacePicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*")) }, Modifier.weight(1f))
                    if (state.workspace != Workspace.SETTINGS) CompactTransport(
                        state, viewModel::skipToPreviousMasterTrack, viewModel::playPause, viewModel::stop,
                        viewModel::skipToNextMasterTrack, viewModel::seekToFraction, viewModel::panic,
                        openTimeline = { viewModel.setWorkspace(Workspace.TRACK); viewModel.setTrackWorkspace(TrackWorkspace.TIMELINE) },
                        openMixer = { viewModel.setWorkspace(Workspace.TRACK); viewModel.setTrackWorkspace(TrackWorkspace.MIXER) },
                        openMetronome = { viewModel.setWorkspace(Workspace.METRONOME) },
                    )
                    BottomNavigation(state.workspace, viewModel::setWorkspace)
                }
            }
        }
        }
    }
}

@Composable
private fun WorkspaceContent(state: MainUiState, vm: MainViewModel, addAudio: () -> Unit, replaceAudio: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clipToBounds().padding(horizontal = 12.dp, vertical = 8.dp)) {
        AnimatedContent(
            targetState = state.workspace,
            transitionSpec = {
                (fadeIn(tween(160)) + slideInVertically(tween(180)) { it / 18 }) togetherWith
                    (fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it / 22 })
            },
            modifier = Modifier.fillMaxSize(),
            label = "workspace",
        ) { workspace ->
            when (workspace) {
                Workspace.PROJECTS -> ProjectsScreen(state, vm)
                Workspace.PLAYLIST -> PlaylistScreen(state, vm)
                Workspace.TRACK -> TrackScreen(state, vm, addAudio, replaceAudio)
                Workspace.METRONOME -> MetronomeScreen(state, vm)
                Workspace.SETTINGS -> SettingsScreen(state, vm)
            }
        }
    }
}

@Composable
private fun BottomNavigation(active: Workspace, select: (Workspace) -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).background(Color(0xFF1A1B1D)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Workspace.entries.forEach { item ->
            val selected = item == active
            val itemLabel = item.railLabel()
            Surface(
                onClick = { select(item) },
                modifier = Modifier.weight(1f).height(44.dp).semantics { contentDescription = itemLabel; role = Role.Button },
                color = if (selected) Mint.copy(alpha = .14f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    VectorIcon(item.iconRes(), null, if (selected) Mint else TextMuted, Modifier.size(23.dp))
                    if (selected) Box(Modifier.align(Alignment.BottomCenter).width(20.dp).height(2.dp).background(Mint, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

@Composable
private fun SideNavigation(active: Workspace, select: (Workspace) -> Unit) {
    Column(
        Modifier.width(64.dp).fillMaxHeight().background(Color(0xFF1A1B1D)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(Modifier.size(48.dp), color = Raised, shape = RoundedCornerShape(6.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Image(painterResource(R.drawable.ic_brand_mark), contentDescription = "LiveTracks", modifier = Modifier.padding(7.dp))
        }
        Spacer(Modifier.height(8.dp))
        Workspace.entries.filterNot { it == Workspace.SETTINGS }.forEach { item ->
            val selected = item == active
            val itemLabel = item.railLabel()
            Surface(
                onClick = { select(item) },
                modifier = Modifier.size(52.dp).semantics { contentDescription = itemLabel; role = Role.Button },
                color = if (selected) Mint.copy(alpha = .14f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { VectorIcon(item.iconRes(), null, if (selected) Mint else TextMuted, Modifier.size(25.dp)) }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.weight(1f))
        val settingsSelected = active == Workspace.SETTINGS
        val settingsLabel = tr("Ajustes", "Settings")
        Surface(onClick = { select(Workspace.SETTINGS) }, modifier = Modifier.size(52.dp).semantics { contentDescription = settingsLabel; role = Role.Button }, color = if (settingsSelected) Mint.copy(alpha = .14f) else Color.Transparent, shape = RoundedCornerShape(10.dp)) {
            Box(contentAlignment = Alignment.Center) { VectorIcon(R.drawable.ic_ui_settings, null, if (settingsSelected) Mint else TextMuted, Modifier.size(25.dp)) }
        }
    }
}

private fun Workspace.iconRes() = when (this) {
    Workspace.PROJECTS -> R.drawable.ic_ui_projects
    Workspace.PLAYLIST -> R.drawable.ic_ui_playlist
    Workspace.TRACK -> R.drawable.ic_ui_track
    Workspace.METRONOME -> R.drawable.ic_ui_metronome
    Workspace.SETTINGS -> R.drawable.ic_ui_settings
}
@Composable private fun Workspace.railLabel() = when (this) {
    Workspace.PROJECTS -> tr("SHOWS", "SHOWS")
    Workspace.PLAYLIST -> tr("LISTA", "SETLIST")
    Workspace.TRACK -> tr("PISTA", "TRACK")
    Workspace.METRONOME -> tr("CLICK", "CLICK")
    Workspace.SETTINGS -> tr("AJUSTES", "SETTINGS")
}

@Composable
private fun CompactContextBar(state: MainUiState, vm: MainViewModel) {
    val project = state.selectedProject(); val master = state.selectedMaster()
    val metro = master?.metronome(project?.defaultMetronome ?: MetronomeSettings())
    BoxWithConstraints(Modifier.fillMaxWidth().height(52.dp).background(Color(0xFF1A1B1D))) {
        val compact = maxWidth < 600.dp
        Row(
            Modifier.fillMaxSize().padding(horizontal = if (compact) 8.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(project?.name ?: tr("Sin proyecto", "No project"), Modifier.weight(1f, fill = false), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (master != null && !compact) {
                    Text("  /  ", color = TextMuted, fontSize = 12.sp)
                    Text(master.name, Modifier.weight(1f, fill = false), color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (state.workspace == Workspace.PROJECTS) {
                ContextModeButton(R.drawable.ic_ui_projects, tr("Lista de proyectos", "Project list"), state.projectWorkspace == ProjectWorkspace.LIST) { vm.setProjectWorkspace(ProjectWorkspace.LIST) }
                Spacer(Modifier.width(4.dp))
                ContextModeButton(R.drawable.ic_ui_mixer, tr("Mixer del proyecto", "Project mixer"), state.projectWorkspace == ProjectWorkspace.MIXER) { vm.setProjectWorkspace(ProjectWorkspace.MIXER) }
                Spacer(Modifier.width(if (compact) 6.dp else 12.dp))
            } else if (state.workspace == Workspace.PLAYLIST && project != null) {
                ContextModeButton(R.drawable.ic_ui_playlist, tr("Lista de pistas", "Track list"), state.playlistWorkspace == PlaylistWorkspace.LIST) { vm.setPlaylistWorkspace(PlaylistWorkspace.LIST) }
                Spacer(Modifier.width(4.dp))
                ContextModeButton(R.drawable.ic_ui_mixer, tr("Mixer de pista", "Track mixer"), state.playlistWorkspace == PlaylistWorkspace.MIXER) { vm.setPlaylistWorkspace(PlaylistWorkspace.MIXER) }
                Spacer(Modifier.width(if (compact) 6.dp else 12.dp))
            } else if (state.workspace == Workspace.TRACK && master != null) {
                ContextModeButton(R.drawable.ic_ui_timeline, tr("Timeline", "Timeline"), state.trackWorkspace == TrackWorkspace.TIMELINE) { vm.setTrackWorkspace(TrackWorkspace.TIMELINE) }
                Spacer(Modifier.width(4.dp))
                ContextModeButton(R.drawable.ic_ui_mixer, tr("Consola de stems", "Stem console"), state.trackWorkspace == TrackWorkspace.MIXER) { vm.setTrackWorkspace(TrackWorkspace.MIXER) }
                Spacer(Modifier.width(if (compact) 6.dp else 12.dp))
            }
            if (!compact && state.workspace != Workspace.METRONOME) {
                Text(metro?.let { "${formatBpm(it.bpm)} BPM  ·  ${it.numerator}/${it.denominator}" } ?: "— BPM", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.width(16.dp))
            }
            val live = state.diagnostics.toneEnabled
            Box(Modifier.size(8.dp).background(if (live) Mint else Amber, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(if (live) "LIVE" else "READY", color = if (live) Mint else Amber, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ContextModeButton(iconRes: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp).semantics { contentDescription = label; role = Role.Button },
        color = if (selected) Blue else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) Blue else Border),
        shape = RoundedCornerShape(7.dp),
    ) { Box(contentAlignment = Alignment.Center) { VectorIcon(iconRes, null, if (selected) Color.White else TextMuted, Modifier.size(22.dp)) } }
}

@Composable
private fun AppHeader(state: MainUiState, select: (Workspace) -> Unit) {
    val project = state.selectedProject()
    val master = state.selectedMaster()
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(150.dp)) {
                Text("LIVE TRACKS", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                Text("SHOW CONSOLE", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Workspace.entries.forEach { workspace ->
                    NavButton(workspace.label(), state.workspace == workspace) { select(workspace) }
                }
            }
            Column(Modifier.widthIn(min = 180.dp).padding(horizontal = 12.dp)) {
                Text(project?.name ?: tr("Sin proyecto", "No project"), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(master?.name ?: tr("Selecciona una pista", "Select a track"), fontSize = 10.sp, color = TextMuted, maxLines = 1)
            }
            val metro = master?.metronome(project?.defaultMetronome ?: MetronomeSettings())
            HeaderValue("BPM", metro?.bpm?.let(::formatBpm) ?: "—")
            HeaderValue("COMPAS", metro?.let { "${it.numerator}/${it.denominator}" } ?: "—")
            StatusPill(state)
        }
    }
}

@Composable
private fun NavButton(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.height(38.dp), contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (active) Mint else Raised, contentColor = if (active) Bg else TextMuted),
        shape = RoundedCornerShape(8.dp),
    ) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1) }
}

@Composable
private fun HeaderValue(label: String, value: String) {
    Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, color = TextMuted)
        Text(value, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPill(state: MainUiState) {
    val color = when { state.safetyStatus == SafetyStatus.UNSAFE -> Red; state.diagnostics.toneEnabled -> Mint; else -> Amber }
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .5f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(color, CircleShape)); Spacer(Modifier.width(6.dp))
            Text(if (state.diagnostics.toneEnabled) "LIVE" else "READY", color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun Workspace.label() = when (this) {
    Workspace.PROJECTS -> "PROYECTOS"; Workspace.PLAYLIST -> "PLAYLIST"; Workspace.TRACK -> "PISTA"; Workspace.METRONOME -> "CLICK"; Workspace.SETTINGS -> "AJUSTES"
}

@Composable
private fun ProjectsScreen(state: MainUiState, vm: MainViewModel) {
    var dialog by remember { mutableStateOf<ProjectDialog?>(null) }
    val selected = state.selectedProject()
    if (state.projectWorkspace == ProjectWorkspace.MIXER) {
        if (state.projects.isEmpty()) {
            EmptyState(tr("SIN PROYECTO", "NO PROJECT"), tr("Selecciona un proyecto para abrir su mixer.", "Select a project to open its mixer."), tr("VER PROYECTOS", "VIEW PROJECTS")) {
                vm.setProjectWorkspace(ProjectWorkspace.LIST)
            }
        } else ProjectBusMixer(state, vm)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Proyectos", "Projects"), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("  ${state.projects.size}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            if (selected != null) {
                DawIconButton(DawIcon.EDIT, tr("Renombrar proyecto", "Rename project")) { dialog = ProjectDialog.Rename }
                Spacer(Modifier.width(4.dp))
                DawIconButton(DawIcon.DELETE, tr("Eliminar proyecto", "Delete project"), danger = true) {
                    if (state.settings.confirmDestructiveActions) dialog = ProjectDialog.Delete else vm.deleteSelectedProject()
                }
                Spacer(Modifier.width(4.dp))
            }
            if (state.projects.isNotEmpty()) DawIconButton(DawIcon.ADD, tr("Nuevo proyecto", "New project"), selected = true) { dialog = ProjectDialog.Create }
        }
        Surface(Modifier.weight(1f).fillMaxWidth(), color = Panel, shape = RoundedCornerShape(10.dp)) {
            if (state.projects.isEmpty()) EmptyState(tr("Sin proyectos", "No projects"), tr("Crea un show para empezar.", "Create a show to get started."), tr("CREAR", "CREATE"), onClick = { dialog = ProjectDialog.Create })
            else LazyColumn(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(state.projects, key = { _, it -> it.id }) { index, project ->
                    val active = project.id == state.selectedProjectId
                    Surface(onClick = { vm.selectProject(project.id) }, color = if (active) Color(0xFF1B292B) else Color(0xFF121920), shape = RoundedCornerShape(8.dp)) {
                        BoxWithConstraints {
                            val compact = maxWidth < 600.dp
                            Row(Modifier.fillMaxWidth().height(if (compact) 72.dp else 62.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text((index + 1).toString().padStart(2, '0'), color = if (active) Mint else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(if (compact) 10.dp else 16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(project.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text("${project.playlist.size} ${tr("PISTAS", "TRACKS")}   ·   ${project.playlist.sumOf { it.tracks.size }} STEMS", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            }
                            if (!compact) {
                                ConsoleReadout("OUTPUT", "${formatDb(project.masterGainDb).replace(" dB", "")}  ${panLabel(project.masterPan)}", Mint)
                                DawIconButton(DawIcon.MIXER, tr("Abrir mixer del proyecto", "Open project mixer")) { vm.selectProject(project.id); vm.setProjectWorkspace(ProjectWorkspace.MIXER) }
                                Spacer(Modifier.width(4.dp))
                            }
                            DawIconButton(DawIcon.OPEN, tr("Abrir playlist", "Open setlist"), selected = true) { vm.selectProject(project.id); vm.setWorkspace(Workspace.PLAYLIST) }
                            }
                        }
                    }
                }
            }
        }
    }
    when (dialog) {
        ProjectDialog.Create -> NameDialog(tr("Nuevo proyecto", "New project"), tr("Nombre del show", "Show name"), { dialog = null }) { vm.createProject(it); dialog = null }
        ProjectDialog.Rename -> NameDialog(tr("Renombrar proyecto", "Rename project"), selected?.name.orEmpty(), { dialog = null }) { vm.renameSelectedProject(it); dialog = null }
        ProjectDialog.Delete -> ConfirmDialog(tr("Eliminar proyecto", "Delete project"), tr("Se quitará el proyecto y toda su configuración local.", "The project and all its local configuration will be removed."), { dialog = null }) { vm.deleteSelectedProject(); dialog = null }
        null -> Unit
    }
}

private enum class ProjectDialog { Create, Rename, Delete }

@Composable
private fun PlaylistScreen(state: MainUiState, vm: MainViewModel) {
    var addDialog by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var playlistMenuExpanded by remember { mutableStateOf(false) }
    val project = state.selectedProject()
    val selected = state.selectedMaster()
    if (project == null) {
        ConsolePanel(Modifier.fillMaxSize()) { EmptyState(tr("SIN PROYECTO", "NO PROJECT"), tr("Selecciona o crea un proyecto antes de armar la playlist.", "Select or create a project before building the setlist."), tr("IR A PROYECTOS", "GO TO PROJECTS")) { vm.setWorkspace(Workspace.PROJECTS) } }
        return
    }
    if (state.playlistWorkspace == PlaylistWorkspace.MIXER) {
        PlaylistBusMixer(project, selected, vm)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().animateContentSize(tween(180))) {
            AnimatedContent(
                targetState = state.playlistEditBarExpanded,
                transitionSpec = {
                    (fadeIn(tween(150)) + slideInVertically(tween(180)) { -it / 6 }) togetherWith
                        (fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it / 8 })
                },
                label = "playlistActions",
            ) { expanded ->
                if (expanded) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Playlist", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${project.playlist.size} cues", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                DawIconButton(DawIcon.STAGE, tr("Entrar al modo escenario", "Enter stage mode"), enabled = project.playlist.isNotEmpty(), selected = true) {
                    vm.setPlaylistPerformanceMode(true)
                }
                Spacer(Modifier.width(4.dp))
                DawIconButton(DawIcon.ADD, tr("Nueva pista master", "New master track")) { addDialog = true }
                Spacer(Modifier.width(4.dp))
                Box {
                    DawIconButton(DawIcon.MORE, tr("Más acciones de playlist", "More setlist actions")) { playlistMenuExpanded = true }
                    DropdownMenu(expanded = playlistMenuExpanded, onDismissRequest = { playlistMenuExpanded = false }) {
                        TimelineMenuItem(DawIcon.EDIT, tr("Renombrar pista", "Rename track"), selected != null) { playlistMenuExpanded = false; renameDialog = true }
                        TimelineMenuItem(DawIcon.DELETE, tr("Quitar pista master", "Remove master track"), selected != null, danger = true) {
                            playlistMenuExpanded = false
                            if (state.settings.confirmDestructiveActions) deleteDialog = true else vm.deleteSelectedMasterTrack()
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                CompactIconButton(R.drawable.ic_ui_arrow_up, tr("Minimizar acciones", "Collapse actions")) { vm.setPlaylistEditBarExpanded(false) }
            }
                } else Surface(onClick = { vm.setPlaylistEditBarExpanded(true) }, modifier = Modifier.fillMaxWidth().height(32.dp), color = Color(0xFF191B1D), shape = RoundedCornerShape(8.dp)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Playlist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("  ${project.playlist.size}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                VectorIcon(R.drawable.ic_ui_arrow_down, tr("Restaurar acciones", "Expand actions"), TextMuted, Modifier.size(18.dp))
            }
                }
            }
        }
        Surface(Modifier.fillMaxSize(), color = Panel, shape = RoundedCornerShape(10.dp)) {
            if (project.playlist.isEmpty()) {
                EmptyState(tr("Playlist vacía", "Empty setlist"), tr("Agrega la primera pista master del show.", "Add the first master track to the show."), tr("AGREGAR", "ADD"), onClick = { addDialog = true })
            } else LazyColumn(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(project.playlist, key = { _, item -> item.id }) { index, item ->
                    PlaylistRow(
                        index, item, project.defaultMetronome, item.id == state.selectedMasterTrackId,
                        select = { vm.selectMasterTrack(item.id) }, go = { vm.playMasterTrack(item.id) },
                        open = {
                            vm.selectMasterTrack(item.id)
                            vm.setTrackWorkspace(TrackWorkspace.TIMELINE)
                            vm.setWorkspace(Workspace.TRACK)
                        },
                        moveUp = { vm.moveMasterTrack(index, -1) }, moveDown = { vm.moveMasterTrack(index, 1) },
                    )
                }
            }
        }
    }
    if (addDialog) NameDialog(tr("Nueva pista master", "New master track"), tr("Nombre de la canción", "Song name"), { addDialog = false }) { vm.createMasterTrack(it); addDialog = false }
    if (renameDialog) NameDialog(tr("Renombrar pista", "Rename track"), selected?.name.orEmpty(), { renameDialog = false }) { vm.renameSelectedMasterTrack(it); renameDialog = false }
    if (deleteDialog) ConfirmDialog(tr("Quitar pista master", "Remove master track"), tr("Se quitará de la playlist con sus stems y ajustes.", "It will be removed from the setlist with its stems and settings."), { deleteDialog = false }) { vm.deleteSelectedMasterTrack(); deleteDialog = false }
}

@Composable
private fun PlaylistRow(index: Int, item: MasterTrack, defaultMetronome: MetronomeSettings, active: Boolean, select: () -> Unit, go: () -> Unit, open: () -> Unit, moveUp: () -> Unit, moveDown: () -> Unit) {
    val metro = item.metronome(defaultMetronome)
    val playLabel = tr("Reproducir pista", "Play track")
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = select, onDoubleClick = open),
        color = if (active) Color(0xFF1B2730) else Color(0xFF121920),
        shape = RoundedCornerShape(7.dp),
    ) {
        BoxWithConstraints {
            val compact = maxWidth < 600.dp
            Row(Modifier.fillMaxWidth().height(if (compact) 68.dp else 58.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(44.dp).fillMaxHeight().background(if (active) Blue else Color(0xFF202A33)), contentAlignment = Alignment.Center) {
                Text((index + 1).toString().padStart(2, '0'), color = if (active) Color.White else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text("${item.tracks.size} STEMS   ${timeText(item.durationSeconds())}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            ConsoleReadout("TEMPO", "${metro.bpm.roundToInt()}  ${metro.numerator}/${metro.denominator}", if (metro.enabled) Amber else TextMuted)
            if (!compact) {
                ConsoleReadout("MASTER", "${formatDb(item.gainDb).replace(" dB", "")}  ${panLabel(item.pan)}", Mint)
                TinyIconButton(R.drawable.ic_ui_arrow_up, tr("Mover arriba", "Move up"), moveUp)
                TinyIconButton(R.drawable.ic_ui_arrow_down, tr("Mover abajo", "Move down"), moveDown)
                DawIconButton(DawIcon.TIMELINE, tr("Abrir pista", "Open track"), onClick = open)
                Spacer(Modifier.width(6.dp))
            }
            Surface(onClick = go, modifier = Modifier.size(44.dp).semantics { contentDescription = playLabel; role = Role.Button }, color = if (active) Mint else Amber, shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) { VectorIcon(R.drawable.ic_ui_play, null, Bg, Modifier.size(21.dp)) }
            }
            Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun PerformancePlaylistScreen(state: MainUiState, vm: MainViewModel) {
    BackHandler { vm.setPlaylistPerformanceMode(false) }
    val project = state.selectedProject()
    val master = state.selectedMaster()
    if (project == null || project.playlist.isEmpty()) {
        LaunchedEffect(Unit) { vm.setPlaylistPerformanceMode(false) }
        return
    }
    val selectedIndex = project.playlist.indexOfFirst { it.id == master?.id }.coerceAtLeast(0)
    val live = state.diagnostics.toneEnabled
    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val wide = maxWidth >= 700.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(if (wide) 56.dp else 64.dp).background(Color(0xFF101419)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        tr("MODO ESCENARIO · DOBLE TOQUE REPRODUCE", "STAGE MODE · DOUBLE TAP PLAYS") + "  ·  ${selectedIndex + 1}/${project.playlist.size}",
                        color = if (live) Mint else Amber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.size(8.dp).background(if (live) Mint else Amber, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(if (live) "LIVE" else "READY", color = if (live) Mint else Amber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                DawIconButton(DawIcon.EDIT, tr("Volver a edición", "Return to edit mode")) { vm.setPlaylistPerformanceMode(false) }
            }
            if (wide) Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PerformanceCueList(project, master, live, vm, Modifier.weight(1f).fillMaxHeight(), wide = true)
                PerformanceTransportDeck(state, vm, Modifier.width(430.dp).fillMaxHeight(), wide = true)
            } else Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PerformanceCueList(project, master, live, vm, Modifier.weight(1f).fillMaxWidth(), wide = false)
                PerformanceTransportDeck(state, vm, Modifier.fillMaxWidth().height(244.dp), wide = false)
            }
        }
    }
}

@Composable
private fun PerformanceCueList(project: Project, selected: MasterTrack?, live: Boolean, vm: MainViewModel, modifier: Modifier, wide: Boolean) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border.copy(alpha = .65f))) {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(project.playlist, key = { _, item -> item.id }) { index, item ->
                val active = item.id == selected?.id
                val metro = item.metronome(project.defaultMetronome)
                Surface(
                    modifier = Modifier.fillMaxWidth().height(if (wide) 76.dp else 72.dp).combinedClickable(
                        onClick = { if (!active) vm.selectMasterTrack(item.id) },
                        onDoubleClick = { vm.playMasterTrack(item.id) },
                    ),
                    color = if (active) Blue.copy(alpha = .27f) else Color(0xFF151A1F),
                    border = BorderStroke(if (active) 2.dp else 1.dp, if (active) Mint else Border.copy(alpha = .45f)),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text((index + 1).toString().padStart(2, '0'), color = if (active) Mint else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${timeText(item.durationSeconds())}  ·  ${formatBpm(metro.bpm)} BPM  ·  ${metro.numerator}/${metro.denominator}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        if (active) {
                            Box(Modifier.size(8.dp).background(if (live) Mint else Amber, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(if (live) "LIVE" else tr("ARMADA", "ARMED"), color = if (live) Mint else Amber, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceTransportDeck(state: MainUiState, vm: MainViewModel, modifier: Modifier, wide: Boolean) {
    val project = state.selectedProject() ?: return
    val master = state.selectedMaster() ?: return
    val index = project.playlist.indexOfFirst { it.id == master.id }
    val canPrevious = index > 0
    val canNext = index in 0 until project.playlist.lastIndex
    val rate = state.diagnostics.actualSampleRate.takeIf { it > 0 } ?: 48_000
    val engineDuration = state.diagnostics.durationFrames.toDouble() / rate
    val total = maxOf(engineDuration, master.durationSeconds())
    val position = if (state.diagnostics.durationFrames > 0) state.diagnostics.renderedFrames.toDouble() / rate else 0.0
    val fraction = if (total > 0) (position / total).toFloat().coerceIn(0f, 1f) else 0f
    val nextName = project.playlist.getOrNull(index + 1)?.name
    Surface(modifier, color = Color(0xFF0D1217), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxSize().padding(if (wide) 20.dp else 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(master.name, Modifier.fillMaxWidth(), fontSize = if (wide) 20.sp else 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${timeText(position)} / ${timeText(total)}", color = Mint, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(fraction, vm::seekToFraction, enabled = state.diagnostics.durationFrames > 0, modifier = Modifier.fillMaxWidth().height(32.dp))
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                StageControlButton(R.drawable.ic_ui_previous, tr("Pista anterior", "Previous track"), 64, canPrevious, Raised, TextMain, vm::skipToPreviousMasterTrack)
                StageControlButton(if (state.diagnostics.toneEnabled) R.drawable.ic_ui_pause else R.drawable.ic_ui_play, tr("Reproducir o pausar", "Play or pause"), 88, !state.openingOutput, Mint, Bg, vm::playPause)
                StageControlButton(R.drawable.ic_ui_stop, tr("Detener", "Stop"), 72, true, Red, Color.White, vm::stop)
                StageControlButton(R.drawable.ic_ui_next, tr("Pista siguiente", "Next track"), 64, canNext, Raised, TextMain, vm::skipToNextMasterTrack)
            }
            Spacer(Modifier.weight(1f))
            Text(nextName?.let { tr("SIGUIENTE", "NEXT") + "  ·  $it" } ?: tr("FIN DE LA PLAYLIST", "END OF SETLIST"), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StageControlButton(iconRes: Int, label: String, size: Int, enabled: Boolean, color: Color, ink: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size.dp).semantics { contentDescription = label; role = Role.Button },
        color = if (enabled) color else Raised.copy(alpha = .35f),
        border = if (color == Raised) BorderStroke(1.dp, if (enabled) Border else Border.copy(alpha = .3f)) else null,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) { VectorIcon(iconRes, null, if (enabled) ink else TextMuted.copy(alpha = .35f), Modifier.size((size * .4f).dp)) }
    }
}

@Composable
private fun ConsoleReadout(label: String, value: String, color: Color) {
    Column(Modifier.width(92.dp)) {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TrackScreen(state: MainUiState, vm: MainViewModel, addAudio: () -> Unit, replaceAudio: (String) -> Unit) {
    var showStemOptions by remember { mutableStateOf(false) }
    var showEmptyStemDialog by remember { mutableStateOf(false) }
    val master = state.selectedMaster()
    if (master == null) {
        ConsolePanel(Modifier.fillMaxSize()) { EmptyState(
            tr("SIN PISTA MASTER", "NO MASTER TRACK"),
            tr("Selecciona una pista de la playlist antes de editar stems.", "Select a playlist track before editing stems."),
            tr("ABRIR PLAYLIST", "OPEN PLAYLIST"),
        ) { vm.setWorkspace(Workspace.PLAYLIST) } }
        return
    }
    AnimatedContent(
        targetState = state.trackWorkspace,
        transitionSpec = {
            (fadeIn(tween(160)) + slideInVertically(tween(180)) { it / 18 }) togetherWith
                (fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it / 22 })
        },
        modifier = Modifier.fillMaxSize(),
        label = "trackWorkspace",
    ) { workspace ->
        when (workspace) {
            TrackWorkspace.TIMELINE -> TimelineEditor(state, vm, replaceAudio) { showStemOptions = true }
            TrackWorkspace.MIXER -> MixerConsole(state.tracks, vm)
        }
    }
    if (showStemOptions) {
        StemSourceDialog(
            dismiss = { showStemOptions = false },
            importAudio = { showStemOptions = false; addAudio() },
            createEmpty = { showStemOptions = false; showEmptyStemDialog = true },
        )
    }
    if (showEmptyStemDialog) {
        EmptyStemDialog(
            dismiss = { showEmptyStemDialog = false },
            confirm = { name, duration -> vm.createEmptyTrack(name, duration); showEmptyStemDialog = false },
        )
    }
}

@Composable
private fun TimelineEditor(state: MainUiState, vm: MainViewModel, replaceAudio: (String) -> Unit, addStem: () -> Unit) {
    if (state.tracks.isEmpty()) {
        ConsolePanel(Modifier.fillMaxSize()) {
            EmptyState(
                tr("PISTA SIN STEMS", "TRACK WITHOUT STEMS"),
                tr("Importa audio o crea un stem vacío con duración. Luego define cada entrada en la timeline.", "Import audio or create an empty stem with a duration, then place it on the timeline."),
                tr("+ AGREGAR STEM", "+ ADD STEM"),
                addStem,
            )
        }
        return
    }
    val zoomLevels = remember { listOf(24.0, 54.0, 120.0, 280.0, 720.0, 1_200.0) }
    val masterId = state.selectedMasterTrackId
    var zoomIndex by rememberSaveable(masterId) { mutableIntStateOf(1) }
    val dpPerSecond = zoomLevels[zoomIndex]
    val density = LocalDensity.current
    val pxPerSecond = with(density) { dpPerSecond.dp.toPx() }
    val compactScreen = LocalConfiguration.current.screenWidthDp < 600
    var toolsExpanded by rememberSaveable(masterId) { mutableStateOf(true) }
    var labelPanelExpanded by rememberSaveable(masterId) { mutableStateOf(!compactScreen) }
    val labelWidth by animateDpAsState(
        targetValue = if (labelPanelExpanded) 188.dp else 42.dp,
        animationSpec = tween(180),
        label = "timelineStemPanelWidth",
    )
    val maxSeconds = maxOf(10.0, state.tracks.maxOf { it.startOffsetFrames.toDouble() / TIMELINE_SAMPLE_RATE + it.durationSeconds } + 1.0)
    var panSeconds by remember { mutableDoubleStateOf(0.0) }
    val grid = timelineGrid(dpPerSecond)
    val selected = state.tracks.firstOrNull { it.id == state.selectedTimelineTrackId }
    val selectedProject = state.selectedProject()
    val selectedMaster = state.selectedMaster()
    val metronome = selectedMaster?.metronome(selectedProject?.defaultMetronome ?: MetronomeSettings()) ?: MetronomeSettings()
    val markers = selectedMaster?.markers.orEmpty()
    var markerDialogVisible by remember { mutableStateOf(false) }
    var editingMarker by remember { mutableStateOf<TimelineMarker?>(null) }
    var extractDialogVisible by remember { mutableStateOf(false) }
    var toolsMenuExpanded by remember { mutableStateOf(false) }
    var activeSnapFrames by remember { mutableStateOf<Long?>(null) }
    val canSplit = selected?.let { track ->
        val end = track.startOffsetFrames + (track.durationSeconds * TIMELINE_SAMPLE_RATE).roundToLong()
        state.timelineCursorFrames > track.startOffsetFrames && state.timelineCursorFrames < end
    } == true
    var localCursorFrames by remember(masterId) { mutableLongStateOf(state.timelineCursorFrames) }
    var cursorDragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.timelineCursorFrames) {
        if (!cursorDragging) localCursorFrames = state.timelineCursorFrames
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val editorWidth = maxWidth
        val viewportWidthPx = with(density) { (maxWidth - labelWidth).coerceAtLeast(120.dp).toPx() }
        val visibleSeconds = (viewportWidthPx / pxPerSecond).toDouble()
        val maxPanSeconds = (maxSeconds - visibleSeconds).coerceAtLeast(0.0)
        fun changeZoom(nextIndex: Int) {
            val clampedIndex = nextIndex.coerceIn(0, zoomLevels.lastIndex)
            if (clampedIndex == zoomIndex) return
            val cursorSeconds = localCursorFrames.toDouble() / TIMELINE_SAMPLE_RATE
            val oldVisibleSeconds = visibleSeconds
            val anchor = if (oldVisibleSeconds > 0.0) {
                ((cursorSeconds - panSeconds) / oldVisibleSeconds).coerceIn(0.0, 1.0)
            } else 0.5
            val nextPxPerSecond = with(density) { zoomLevels[clampedIndex].dp.toPx() }
            val nextVisibleSeconds = (viewportWidthPx / nextPxPerSecond).toDouble()
            zoomIndex = clampedIndex
            panSeconds = (cursorSeconds - anchor * nextVisibleSeconds).coerceIn(
                0.0,
                (maxSeconds - nextVisibleSeconds).coerceAtLeast(0.0),
            )
        }
        LaunchedEffect(dpPerSecond, maxSeconds, viewportWidthPx) { panSeconds = panSeconds.coerceIn(0.0, maxPanSeconds) }
        LaunchedEffect(state.timelineCursorFrames, state.diagnostics.toneEnabled) {
            if (state.diagnostics.toneEnabled) {
                val cursorSeconds = state.timelineCursorFrames.toDouble() / TIMELINE_SAMPLE_RATE
                if (cursorSeconds < panSeconds || cursorSeconds > panSeconds + visibleSeconds * .86) {
                    panSeconds = (cursorSeconds - visibleSeconds * .18).coerceIn(0.0, maxPanSeconds)
                }
            }
        }
        val horizontalPan = rememberScrollableState { delta ->
            val previous = panSeconds
            panSeconds = (panSeconds - delta / pxPerSecond).coerceIn(0.0, maxPanSeconds)
            ((previous - panSeconds) * pxPerSecond).toFloat()
        }
        fun snapOffset(trackId: String, proposed: Long): Long {
            val tolerance = (12.0 / pxPerSecond * TIMELINE_SAMPLE_RATE).roundToLong().coerceAtLeast(48)
            val gridStepFrames = (grid.minorSeconds * TIMELINE_SAMPLE_RATE).roundToLong().coerceAtLeast(1)
            val candidates = buildList {
                add(0L)
                add(localCursorFrames)
                add((proposed.toDouble() / metronome.beatDurationFrames()).roundToLong() * metronome.beatDurationFrames())
                addAll(markers.map(TimelineMarker::positionFrames))
                state.tracks.filterNot { it.id == trackId }.forEach { other ->
                    add(other.startOffsetFrames)
                    add(other.startOffsetFrames + (other.durationSeconds * TIMELINE_SAMPLE_RATE).roundToLong())
                }
            }
            return snapTimelineFrames(
                proposedFrames = proposed,
                gridStepFrames = gridStepFrames,
                magneticTargets = candidates,
                magneticToleranceFrames = tolerance,
                enabled = state.settings.timelineSnapEnabled,
            )
        }

        Surface(Modifier.fillMaxSize(), color = Panel, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().animateContentSize(tween(180))) {
                    AnimatedContent(
                        targetState = toolsExpanded,
                        transitionSpec = {
                            (fadeIn(tween(150)) + slideInVertically(tween(180)) { -it / 6 }) togetherWith
                                (fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it / 8 })
                        },
                        label = "timelineTools",
                    ) { expanded ->
                        if (expanded) BoxWithConstraints(
                            Modifier.fillMaxWidth().height(48.dp).background(Raised),
                        ) {
                            val showScale = maxWidth >= 340.dp
                            val showPrimarySplit = maxWidth >= 400.dp
                            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                DawIconButton(DawIcon.ADD, tr("Agregar stem", "Add stem"), selected = true, onClick = addStem)
                                DawIconButton(DawIcon.UNDO, tr("Deshacer", "Undo"), enabled = state.canUndoTimeline, onClick = vm::undoTimelineEdit)
                                DawIconButton(DawIcon.REDO, tr("Rehacer", "Redo"), enabled = state.canRedoTimeline, onClick = vm::redoTimelineEdit)
                                if (showPrimarySplit) DawIconButton(DawIcon.SPLIT, tr("Dividir stem en el cursor", "Split stem at playhead"), enabled = canSplit, onClick = vm::splitSelectedTrackAtCursor)
                                Spacer(Modifier.weight(1f))
                                if (showScale) Text(
                                    if (state.settings.timelineSnapEnabled) grid.scaleLabel else tr("LIBRE", "FREE"),
                                    color = if (state.settings.timelineSnapEnabled) Mint else TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.width(56.dp),
                                )
                                DawIconButton(DawIcon.ZOOM_OUT, tr("Alejar", "Zoom out"), enabled = zoomIndex > 0) {
                                    changeZoom(zoomIndex - 1)
                                }
                                DawIconButton(DawIcon.ZOOM_IN, tr("Acercar", "Zoom in"), enabled = zoomIndex < zoomLevels.lastIndex) {
                                    changeZoom(zoomIndex + 1)
                                }
                                Box {
                                    DawIconButton(DawIcon.MORE, tr("Más herramientas", "More tools"), onClick = { toolsMenuExpanded = true })
                                    DropdownMenu(expanded = toolsMenuExpanded, onDismissRequest = { toolsMenuExpanded = false }) {
                                        if (!showPrimarySplit) TimelineMenuItem(DawIcon.SPLIT, tr("Dividir stem en el cursor", "Split stem at playhead"), canSplit) {
                                            toolsMenuExpanded = false; vm.splitSelectedTrackAtCursor()
                                        }
                                        TimelineMenuItem(DawIcon.EXTRACT, tr("Enviar a pista independiente", "Send to independent track"), selected != null) {
                                            toolsMenuExpanded = false; extractDialogVisible = true
                                        }
                                        TimelineMenuItem(DawIcon.MARKER, tr("Agregar marca", "Add marker"), true) {
                                            toolsMenuExpanded = false; editingMarker = null; markerDialogVisible = true
                                        }
                                        TimelineSwitchMenuItem(
                                            DawIcon.SNAP,
                                            tr("Snap · ${grid.scaleLabel}", "Snap · ${grid.scaleLabel}"),
                                            state.settings.timelineSnapEnabled,
                                        ) { vm.setTimelineSnapEnabled(it) }
                                        TimelineSwitchMenuItem(
                                            DawIcon.METRONOME,
                                            tr("Grilla de tempo", "Tempo grid"),
                                            selectedMaster?.tempoGridVisible == true,
                                            enabled = selectedMaster != null,
                                        ) { vm.setTempoGridVisible(it) }
                                        TimelineMenuItem(
                                            DawIcon.METRONOME,
                                            if (selected?.isClickReference == true) {
                                                tr("Quitar referencia de click", "Remove click reference")
                                            } else {
                                                tr("Usar stem como referencia de click", "Use stem as click reference")
                                            },
                                            selected != null,
                                            active = selected?.isClickReference == true,
                                        ) {
                                            toolsMenuExpanded = false
                                            vm.toggleSelectedTrackAsClickReference()
                                        }
                                        TimelineMenuItem(DawIcon.DELETE, tr("Quitar stem", "Remove stem"), selected != null, danger = true) {
                                            toolsMenuExpanded = false; selected?.let { vm.removeTrack(it.id) }
                                        }
                                    }
                                }
                                DawIconButton(DawIcon.COLLAPSE, tr("Minimizar herramientas", "Collapse tools")) { toolsExpanded = false }
                            }
                        } else Surface(
                            onClick = { toolsExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            color = Color(0xFF191B1D),
                        ) {
                            Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                VectorIcon(R.drawable.ic_ui_timeline, null, TextMuted, Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("HERRAMIENTAS", "TOOLS"), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(selected?.name.orEmpty(), Modifier.weight(1f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                VectorIcon(R.drawable.ic_ui_arrow_down, tr("Restaurar herramientas", "Expand tools"), TextMuted, Modifier.size(19.dp))
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.height(76.dp).fillMaxWidth()) {
                            Box(Modifier.width(labelWidth).fillMaxHeight().background(Color(0xFF111315)).padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart) {
                                if (labelPanelExpanded) {
                                    Column {
                                        Text(tr("CURSOR", "PLAYHEAD"), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(timelineTimecode(localCursorFrames), color = Amber, fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    CompactIconButton(
                                        R.drawable.ic_ui_collapse_left,
                                        tr("Minimizar panel de stems", "Collapse stem panel"),
                                        Modifier.align(Alignment.CenterEnd),
                                    ) { labelPanelExpanded = false }
                                } else {
                                    CompactIconButton(
                                        R.drawable.ic_ui_expand_right,
                                        tr("Restaurar panel de stems", "Expand stem panel"),
                                        Modifier.align(Alignment.Center),
                                    ) { labelPanelExpanded = true }
                                }
                            }
                            Column(Modifier.weight(1f).fillMaxHeight().scrollable(horizontalPan, Orientation.Horizontal)) {
                                MarkerRuler(
                                    markers = markers,
                                    panSeconds = panSeconds,
                                    visibleSeconds = visibleSeconds,
                                    pxPerSecond = pxPerSecond,
                                    maxFrames = (maxSeconds * TIMELINE_SAMPLE_RATE).roundToLong(),
                                    renderingIds = state.renderingVoiceCueIds,
                                    failedIds = state.failedVoiceCueIds,
                                    edit = { marker ->
                                        localCursorFrames = marker.positionFrames
                                        vm.setTimelineCursor(marker.positionFrames)
                                        editingMarker = marker
                                        markerDialogVisible = true
                                    },
                                    move = vm::setTimelineMarkerPosition,
                                    modifier = Modifier.fillMaxWidth().height(34.dp),
                                )
                                TimelineRuler(
                                    panSeconds, visibleSeconds, pxPerSecond, grid, metronome,
                                    selectedMaster?.tempoGridVisible == true,
                                    Modifier.fillMaxWidth().weight(1f),
                                ) { seconds ->
                                    val frame = (seconds * TIMELINE_SAMPLE_RATE).roundToLong().coerceAtLeast(0)
                                    localCursorFrames = frame
                                    vm.setTimelineCursor(frame)
                                }
                            }
                        }
                        LazyColumn(Modifier.weight(1f)) {
                            itemsIndexed(state.tracks, key = { _, it -> it.id }) { index, track ->
                                Row(Modifier.fillMaxWidth().height(68.dp)) {
                                    TimelineLaneHeader(track, index, track.id == state.selectedTimelineTrackId, labelWidth, !labelPanelExpanded, replaceAudio) {
                                        vm.selectTimelineTrack(track.id)
                                    }
                                    TimelineLaneViewport(
                                        track = track,
                                        index = index,
                                        selected = track.id == state.selectedTimelineTrackId,
                                        panSeconds = panSeconds,
                                        visibleSeconds = visibleSeconds,
                                        pxPerSecond = pxPerSecond,
                                        grid = grid,
                                        metronome = metronome,
                                        showTempoGrid = selectedMaster?.tempoGridVisible == true,
                                        horizontalPan = horizontalPan,
                                        snapOffset = { snapOffset(track.id, it) },
                                        previewSnap = {
                                            activeSnapFrames = if (state.settings.timelineSnapEnabled) it else null
                                        },
                                        select = { vm.selectTimelineTrack(track.id) },
                                        replace = { replaceAudio(track.id) },
                                        commitOffset = { vm.setTrackOffset(track.id, it) },
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                                HorizontalDivider(color = Border.copy(alpha = .5f))
                            }
                        }
                    }
                    val labelWidthPx = with(density) { labelWidth.toPx() }
                    val cursorSeconds = localCursorFrames.toDouble() / TIMELINE_SAMPLE_RATE
                    val cursorX = labelWidthPx + ((cursorSeconds - panSeconds) * pxPerSecond).toFloat()
                    Canvas(Modifier.matchParentSize()) {
                        markers.forEach { marker ->
                            val markerX = labelWidthPx + ((marker.positionFrames.toDouble() / TIMELINE_SAMPLE_RATE - panSeconds) * pxPerSecond).toFloat()
                            if (markerX in labelWidthPx..size.width) {
                                drawLine(Blue.copy(alpha = .72f), androidx.compose.ui.geometry.Offset(markerX, 34.dp.toPx()),
                                    androidx.compose.ui.geometry.Offset(markerX, size.height), 1.dp.toPx())
                            }
                        }
                        activeSnapFrames?.let { snapFrames ->
                            val snapX = labelWidthPx + ((snapFrames.toDouble() / TIMELINE_SAMPLE_RATE - panSeconds) * pxPerSecond).toFloat()
                            if (snapX in labelWidthPx..size.width) {
                                drawLine(
                                    Mint.copy(alpha = .92f),
                                    androidx.compose.ui.geometry.Offset(snapX, 76.dp.toPx()),
                                    androidx.compose.ui.geometry.Offset(snapX, size.height),
                                    2.dp.toPx(),
                                )
                            }
                        }
                        if (cursorX in labelWidthPx..size.width) {
                            drawLine(Amber, androidx.compose.ui.geometry.Offset(cursorX, 0f),
                                androidx.compose.ui.geometry.Offset(cursorX, size.height), 2.dp.toPx())
                        }
                    }
                    if (cursorX in labelWidthPx..with(density) { editorWidth.toPx() }) {
                        Box(
                            Modifier.offset { IntOffset((cursorX - with(density) { 20.dp.toPx() }).roundToInt(), 0) }
                                .size(width = 40.dp, height = 76.dp)
                                .pointerInput(pxPerSecond, maxSeconds) {
                                    detectDragGestures(
                                        onDragStart = { cursorDragging = true },
                                        onDragEnd = { cursorDragging = false },
                                        onDragCancel = { cursorDragging = false },
                                    ) { change, amount ->
                                        change.consume()
                                        val deltaFrames = (amount.x / pxPerSecond * TIMELINE_SAMPLE_RATE).roundToLong()
                                        val oneMillisecond = TIMELINE_SAMPLE_RATE / 1_000L
                                        val next = (((localCursorFrames + deltaFrames).coerceAtLeast(0) + oneMillisecond / 2) /
                                            oneMillisecond * oneMillisecond).coerceAtMost((maxSeconds * TIMELINE_SAMPLE_RATE).roundToLong())
                                        localCursorFrames = next
                                        vm.setTimelineCursor(next)
                                    }
                                },
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val center = size.width / 2f
                                val path = Path().apply {
                                    moveTo(center - 9.dp.toPx(), 4.dp.toPx())
                                    lineTo(center + 9.dp.toPx(), 4.dp.toPx())
                                    lineTo(center, 15.dp.toPx())
                                    close()
                                }
                                drawPath(path, Amber)
                            }
                        }
                    }
                }
            }
        }
    }
    if (extractDialogVisible) {
        NameDialog(
            title = tr("Enviar a pista independiente", "Send to independent track"),
            initial = selected?.name?.substringBeforeLast('.')?.let { tr("$it separado", "$it extract") }.orEmpty(),
            dismiss = { extractDialogVisible = false },
            confirm = { vm.extractSelectedTrackToNewMaster(it); extractDialogVisible = false },
        )
    }
    if (markerDialogVisible) {
        TimelineMarkerDialog(
            marker = editingMarker,
            dismiss = { markerDialogVisible = false },
            save = { label, kind, voice, lead ->
                val marker = editingMarker
                if (marker == null) vm.createTimelineMarker(label, kind, voice, lead)
                else vm.updateTimelineMarker(marker.id, label, kind, voice, lead)
                markerDialogVisible = false
            },
            delete = editingMarker?.let { marker -> { vm.deleteTimelineMarker(marker.id); markerDialogVisible = false } },
        )
    }
}

private data class TimelineGrid(val majorSeconds: Double, val minorSeconds: Double, val scaleLabel: String)

private fun timelineGrid(dpPerSecond: Double): TimelineGrid = when {
    dpPerSecond >= 700 -> TimelineGrid(.1, .01, "10 ms")
    dpPerSecond >= 250 -> TimelineGrid(.25, .05, "50 ms")
    dpPerSecond >= 100 -> TimelineGrid(.5, .1, "100 ms")
    dpPerSecond >= 45 -> TimelineGrid(1.0, .25, "250 ms")
    else -> TimelineGrid(5.0, 1.0, "1 s")
}

@Composable
private fun TimelineRuler(
    panSeconds: Double,
    visibleSeconds: Double,
    pxPerSecond: Float,
    grid: TimelineGrid,
    metronome: MetronomeSettings,
    showTempoGrid: Boolean,
    modifier: Modifier,
    seek: (Double) -> Unit,
) {
    Box(
        modifier.background(Color(0xFF111315)).clipToBounds()
            .pointerInput(panSeconds, pxPerSecond) {
                detectTapGestures { point -> seek(panSeconds + point.x / pxPerSecond) }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val firstTick = floor(panSeconds / grid.minorSeconds).toLong()
            val lastTick = ((panSeconds + visibleSeconds) / grid.minorSeconds).toLong() + 1
            val majorEvery = (grid.majorSeconds / grid.minorSeconds).roundToInt().coerceAtLeast(1)
            for (tick in firstTick..lastTick) {
                val seconds = tick * grid.minorSeconds
                val x = ((seconds - panSeconds) * pxPerSecond).toFloat()
                val major = tick % majorEvery == 0L
                drawLine(
                    if (major) Silver.copy(alpha = .38f) else Border.copy(alpha = .22f),
                    androidx.compose.ui.geometry.Offset(x, if (major) 18.dp.toPx() else 27.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(x, size.height),
                    if (major) 1.dp.toPx() else .75.dp.toPx(),
                )
            }
            if (showTempoGrid) drawBeatLines(panSeconds, visibleSeconds, pxPerSecond, metronome, ruler = true)
        }
        var labelTime = floor(panSeconds / grid.majorSeconds) * grid.majorSeconds
        while (labelTime <= panSeconds + visibleSeconds + grid.majorSeconds) {
            val x = ((labelTime - panSeconds) * pxPerSecond).toFloat()
            Text(rulerLabel(labelTime, grid.majorSeconds),
                Modifier.offset { IntOffset(x.roundToInt() + 4, 2) }, color = TextMuted,
                fontFamily = FontFamily.Monospace, fontSize = 8.sp, maxLines = 1)
            labelTime += grid.majorSeconds
        }
    }
}

@Composable
private fun MarkerRuler(
    markers: List<TimelineMarker>,
    panSeconds: Double,
    visibleSeconds: Double,
    pxPerSecond: Float,
    maxFrames: Long,
    renderingIds: Set<String>,
    failedIds: Set<String>,
    edit: (TimelineMarker) -> Unit,
    move: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color(0xFF0D1012)).clipToBounds()) {
        Text(tr("MARCAS", "MARKERS"), Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), color = TextMuted.copy(alpha = .45f),
            fontSize = 7.sp, fontWeight = FontWeight.Bold)
        markers.filter { it.positionFrames.toDouble() / TIMELINE_SAMPLE_RATE in (panSeconds - 1.0)..(panSeconds + visibleSeconds) }
            .forEach { marker ->
                var localFrame by remember(marker.id, marker.positionFrames) { mutableLongStateOf(marker.positionFrames) }
                val x = ((localFrame.toDouble() / TIMELINE_SAMPLE_RATE - panSeconds) * pxPerSecond).toFloat()
                Surface(
                    onClick = { edit(marker.copy(positionFrames = localFrame)) },
                    modifier = Modifier.offset { IntOffset(x.roundToInt(), 3) }.height(28.dp).widthIn(max = 138.dp)
                        .pointerInput(marker.id, pxPerSecond, maxFrames) {
                            detectDragGestures(
                                onDragEnd = { move(marker.id, localFrame) },
                                onDragCancel = { localFrame = marker.positionFrames },
                            ) { change, amount ->
                                change.consume()
                                val beatFrames = TIMELINE_SAMPLE_RATE / 1_000L
                                val raw = (localFrame + amount.x / pxPerSecond * TIMELINE_SAMPLE_RATE).roundToLong().coerceIn(0, maxFrames)
                                localFrame = ((raw + beatFrames / 2) / beatFrames) * beatFrames
                            }
                        },
                    color = Blue,
                    shape = RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp),
                ) {
                    Row(Modifier.padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        VectorIcon(
                            if (marker.voiceCueEnabled) R.drawable.ic_ui_voice else R.drawable.ic_ui_marker,
                            null,
                            when {
                                marker.id in failedIds -> Red
                                marker.id in renderingIds -> Amber
                                else -> Color.White
                            },
                            Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(marker.label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
    }
}

private fun rulerLabel(seconds: Double, majorSeconds: Double): String = when {
    majorSeconds >= 1.0 -> "${seconds.roundToInt()}s"
    majorSeconds >= .5 -> "%.1fs".format(seconds)
    else -> "%.2fs".format(seconds)
}

private fun DrawScope.drawBeatLines(
    panSeconds: Double,
    visibleSeconds: Double,
    pxPerSecond: Float,
    metronome: MetronomeSettings,
    ruler: Boolean,
) {
    val beatSeconds = metronome.beatDurationSeconds()
    val showEveryBeat = beatSeconds * pxPerSecond >= 8f
    val step = if (showEveryBeat) beatSeconds else metronome.barDurationSeconds()
    val first = floor(panSeconds / step).toLong()
    val last = ((panSeconds + visibleSeconds) / step).toLong() + 1
    for (tick in first..last) {
        val seconds = tick * step
        val x = ((seconds - panSeconds) * pxPerSecond).toFloat()
        val beatIndex = if (showEveryBeat) tick else tick * metronome.numerator
        val downbeat = beatIndex % metronome.numerator == 0L
        val color = if (downbeat) {
            Amber.copy(alpha = if (ruler) .96f else .48f)
        } else {
            Silver.copy(alpha = if (ruler) .82f else .32f)
        }
        val startY = if (ruler) size.height * if (downbeat) .34f else .52f else 0f
        drawLine(
            color,
            androidx.compose.ui.geometry.Offset(x, startY),
            androidx.compose.ui.geometry.Offset(x, size.height),
            if (downbeat) 1.75.dp.toPx() else 1.dp.toPx(),
        )
    }
}

@Composable
private fun TimelineLaneHeader(track: MixerTrackUi, index: Int, selected: Boolean, width: androidx.compose.ui.unit.Dp, compact: Boolean, replace: (String) -> Unit, select: () -> Unit) {
    Surface(modifier = Modifier.width(width).fillMaxHeight()
        .combinedClickable(onClick = select, onDoubleClick = { replace(track.id) }),
        color = if (selected) Raised else if (index % 2 == 0) Panel else Color(0xFF191B1D)) {
        Row(Modifier.fillMaxSize().padding(horizontal = if (compact) 5.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (compact) {
                Box(Modifier.width(4.dp).height(44.dp).background(Color(track.colorArgb), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(5.dp))
                Text(if (track.isClickReference) "REF" else (index + 1).toString().padStart(2, '0'), color = if (track.isClickReference) Amber else if (selected) Color(track.colorArgb) else TextMuted,
                    fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            } else {
                Box(Modifier.width(4.dp).height(44.dp).background(Color(track.colorArgb), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (if (track.isClickReference) "REF" else (index + 1).toString().padStart(2, '0')) +
                            "  ·  ${if (track.isClickReference) "" else "IN "}${timelineTimecode(track.startOffsetFrames)}",
                        color = if (track.isClickReference) Amber else if (selected) Color(track.colorArgb) else TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineLaneViewport(
    track: MixerTrackUi,
    index: Int,
    selected: Boolean,
    panSeconds: Double,
    visibleSeconds: Double,
    pxPerSecond: Float,
    grid: TimelineGrid,
    metronome: MetronomeSettings,
    showTempoGrid: Boolean,
    horizontalPan: androidx.compose.foundation.gestures.ScrollableState,
    snapOffset: (Long) -> Long,
    previewSnap: (Long?) -> Unit,
    select: () -> Unit,
    replace: () -> Unit,
    commitOffset: (Long) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    var localStartFrames by remember(track.id, track.startOffsetFrames) { mutableLongStateOf(track.startOffsetFrames) }
    var dragOriginFrames by remember(track.id) { mutableLongStateOf(track.startOffsetFrames) }
    var dragDistancePx by remember(track.id) { mutableFloatStateOf(0f) }
    var snapTargetFrames by remember(track.id) { mutableLongStateOf(track.startOffsetFrames) }
    val currentSnapOffset by rememberUpdatedState(snapOffset)
    val currentPreviewSnap by rememberUpdatedState(previewSnap)
    val currentCommitOffset by rememberUpdatedState(commitOffset)
    val clipStart = localStartFrames.toDouble() / TIMELINE_SAMPLE_RATE
    val clipEnd = clipStart + track.durationSeconds
    val visibleStart = max(clipStart, panSeconds)
    val visibleEnd = min(clipEnd, panSeconds + visibleSeconds)
    Box(modifier.background(if (index % 2 == 0) Color(0xFF17191B) else Color(0xFF121416)).clipToBounds()
        .scrollable(horizontalPan, Orientation.Horizontal)) {
        Canvas(Modifier.matchParentSize()) {
            val firstTick = floor(panSeconds / grid.minorSeconds).toLong()
            val lastTick = ((panSeconds + visibleSeconds) / grid.minorSeconds).toLong() + 1
            val majorEvery = (grid.majorSeconds / grid.minorSeconds).roundToInt().coerceAtLeast(1)
            for (tick in firstTick..lastTick) {
                val x = ((tick * grid.minorSeconds - panSeconds) * pxPerSecond).toFloat()
                drawLine(if (tick % majorEvery == 0L) Border.copy(alpha = .38f) else Border.copy(alpha = .14f),
                    androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
            }
            if (showTempoGrid) drawBeatLines(panSeconds, visibleSeconds, pxPerSecond, metronome, ruler = false)
        }
        if (visibleEnd > visibleStart && track.durationSeconds > 0) {
            val startPx = ((visibleStart - panSeconds) * pxPerSecond).toFloat()
            val widthPx = ((visibleEnd - visibleStart) * pxPerSecond).toFloat().coerceAtLeast(with(density) { 4.dp.toPx() })
            val startFraction = ((visibleStart - clipStart) / track.durationSeconds).coerceIn(0.0, 1.0)
            val endFraction = ((visibleEnd - clipStart) / track.durationSeconds).coerceIn(startFraction, 1.0)
            val clipColor = Color(track.colorArgb)
            Box(
                Modifier.offset { IntOffset(startPx.roundToInt(), 0) }.padding(vertical = 8.dp)
                    .width(with(density) { widthPx.toDp() }).fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp)).background(clipColor.copy(alpha = if (selected) .78f else .58f))
                    .then(if (selected) Modifier.border(BorderStroke(2.dp, Silver), RoundedCornerShape(4.dp)) else Modifier)
                    .combinedClickable(onClick = select, onDoubleClick = replace)
                    .pointerInput(track.id, pxPerSecond) {
                        detectDragGestures(
                            onDragStart = {
                                select()
                                dragOriginFrames = track.startOffsetFrames
                                dragDistancePx = 0f
                                snapTargetFrames = track.startOffsetFrames
                                currentPreviewSnap(snapTargetFrames)
                            },
                            onDragEnd = {
                                localStartFrames = snapTargetFrames
                                currentPreviewSnap(null)
                                currentCommitOffset(snapTargetFrames)
                            },
                            onDragCancel = {
                                localStartFrames = track.startOffsetFrames
                                currentPreviewSnap(null)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDistancePx += amount.x
                                val raw = (dragOriginFrames.toDouble() + dragDistancePx.toDouble() / pxPerSecond * TIMELINE_SAMPLE_RATE)
                                    .roundToLong().coerceAtLeast(0)
                                localStartFrames = raw
                                snapTargetFrames = currentSnapOffset(raw)
                                currentPreviewSnap(snapTargetFrames)
                            },
                        )
                    },
            ) {
                if (track.waveformPeaks.isNotEmpty()) {
                    Canvas(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp)) {
                        val middle = size.height / 2f
                        val first = (startFraction * track.waveformPeaks.size).toInt().coerceIn(0, track.waveformPeaks.lastIndex)
                        val last = (endFraction * track.waveformPeaks.size).toInt().coerceIn(first + 1, track.waveformPeaks.size)
                        val columns = (size.width / 2.dp.toPx()).roundToInt().coerceAtLeast(1)
                        repeat(columns) { column ->
                            val fraction = column.toFloat() / columns
                            val sample = (first + fraction * (last - first - 1)).roundToInt().coerceIn(first, last - 1)
                            val amplitude = sqrt(track.waveformPeaks[sample].coerceIn(0f, 1f)) * middle * .9f
                            val x = column * size.width / columns
                            drawLine(Color.White.copy(alpha = .86f), androidx.compose.ui.geometry.Offset(x, middle - amplitude),
                                androidx.compose.ui.geometry.Offset(x, middle + amplitude), 1.25.dp.toPx(), StrokeCap.Round)
                        }
                    }
                } else {
                    Text(
                        if (track.hasAudioSource) tr("Onda no disponible", "Waveform unavailable") else tr("Región vacía", "Empty region"),
                        Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
                        color = Color.White.copy(alpha = .72f), fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MixerConsole(tracks: List<MixerTrackUi>, vm: MainViewModel) {
    if (tracks.isEmpty()) {
        ConsolePanel(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(tr("CONSOLA VACÍA", "EMPTY CONSOLE"), fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(tr("Agrega stems desde la timeline.", "Add stems from the timeline."), color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                DawIconButton(DawIcon.TIMELINE, tr("Abrir timeline", "Open timeline"), selected = true) {
                    vm.setTrackWorkspace(TrackWorkspace.TIMELINE)
                }
            }
        }
        return
    }
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    Surface(Modifier.fillMaxSize(), color = Bg, shape = RoundedCornerShape(2.dp)) {
        if (landscape) {
            LazyColumn(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(tracks, key = { _, it -> it.id }) { index, track ->
                    ChannelStrip(index, track, { vm.setTrackGain(index, it) }, { vm.setTrackPan(index, it) }, { vm.toggleMute(index) }, { vm.toggleSolo(index) })
                }
            }
        } else {
            LazyRow(Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(tracks, key = { _, it -> it.id }) { index, track ->
                    ChannelStrip(index, track, { vm.setTrackGain(index, it) }, { vm.setTrackPan(index, it) }, { vm.toggleMute(index) }, { vm.toggleSolo(index) })
                }
            }
        }
    }
}

private data class BusChannelUi(
    val id: String,
    val name: String,
    val detail: String,
    val gainDb: Float,
    val pan: Float,
)

@Composable
private fun ProjectBusMixer(state: MainUiState, vm: MainViewModel) {
    BusMixerConsole(
        channels = state.projects.map { project ->
            BusChannelUi(
                id = project.id,
                name = project.name,
                detail = "${project.playlist.size} ${tr("PISTAS", "TRACKS")} · ${project.playlist.sumOf { it.tracks.size }} STEMS",
                gainDb = project.masterGainDb,
                pan = project.masterPan,
            )
        },
        selectedId = state.selectedProjectId,
        select = vm::selectProject,
        setGain = vm::setProjectGain,
        setPan = vm::setProjectPan,
    )
}

@Composable
private fun PlaylistBusMixer(project: Project, selected: MasterTrack?, vm: MainViewModel) {
    BusMixerConsole(
        channels = project.playlist.map { master ->
            BusChannelUi(
                id = master.id,
                name = master.name,
                detail = "${master.tracks.size} STEMS · ${timeText(master.durationSeconds())}",
                gainDb = master.gainDb,
                pan = master.pan,
            )
        },
        selectedId = selected?.id,
        select = vm::selectMasterTrack,
        setGain = vm::setMasterGain,
        setPan = vm::setMasterPan,
    )
}

@Composable
private fun BusMixerConsole(
    channels: List<BusChannelUi>,
    selectedId: String?,
    select: (String) -> Unit,
    setGain: (String, Float) -> Unit,
    setPan: (String, Float) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    Surface(Modifier.fillMaxSize(), color = Bg, shape = RoundedCornerShape(2.dp)) {
        if (landscape) {
            LazyColumn(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                    LandscapeBusChannel(index, channel, channel.id == selectedId, select, setGain, setPan)
                }
            }
        } else {
            LazyRow(Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                    PortraitBusChannel(index, channel, channel.id == selectedId, select, setGain, setPan)
                }
            }
        }
    }
}

@Composable
private fun LandscapeBusChannel(
    index: Int,
    channel: BusChannelUi,
    selected: Boolean,
    select: (String) -> Unit,
    setGain: (String, Float) -> Unit,
    setPan: (String, Float) -> Unit,
) {
    val channelColor = busChannelColor(index)
    Surface(
        Modifier.fillMaxWidth().height(142.dp).clickable { select(channel.id) },
        color = if (index % 2 == 0) Panel else Color(0xFF1C1E20),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, if (selected) channelColor else Border),
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(channelColor))
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(channel.name, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(channel.detail, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                    }
                    Text((index + 1).toString().padStart(2, '0'), color = if (selected) channelColor else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Border)
                Row(
                    Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        LabelValue(tr("NIVEL", "LEVEL"), formatDb(channel.gainDb), channelColor)
                        HorizontalConsoleFader(channel.gainDb, { setGain(channel.id, it) }, channelColor, Modifier.fillMaxWidth())
                    }
                    Column(Modifier.width(82.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PAN", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RotaryKnob(channel.pan, -1f..1f, { setPan(channel.id, it) }, channelColor, Modifier.size(48.dp))
                            Text(panLabel(channel.pan), color = channelColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitBusChannel(
    index: Int,
    channel: BusChannelUi,
    selected: Boolean,
    select: (String) -> Unit,
    setGain: (String, Float) -> Unit,
    setPan: (String, Float) -> Unit,
) {
    val channelColor = busChannelColor(index)
    Surface(
        Modifier.width(208.dp).fillMaxHeight().clickable { select(channel.id) },
        color = if (index % 2 == 0) Panel else Color(0xFF1C1E20),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, if (selected) channelColor else Border),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(channelColor))
            Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(channel.name, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text((index + 1).toString().padStart(2, '0'), color = if (selected) channelColor else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Text(channel.detail, Modifier.fillMaxWidth().padding(horizontal = 4.dp), color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(Modifier.padding(top = 4.dp), color = Border)
            VerticalFader(channel.gainDb, { setGain(channel.id, it) }, channelColor, Modifier.weight(1f).fillMaxWidth())
            Text(formatDb(channel.gainDb), color = channelColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
                RotaryKnob(channel.pan, -1f..1f, { setPan(channel.id, it) }, channelColor, Modifier.size(52.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("PAN", color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                    Text(panLabel(channel.pan), color = TextMain, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                if (selected) tr("SELECCIONADO", "SELECTED") else tr("TOCAR PARA SELECCIONAR", "TAP TO SELECT"),
                color = if (selected) channelColor else TextMuted,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.fillMaxWidth().height(4.dp).background(channelColor))
        }
    }
}

private fun busChannelColor(index: Int): Color = listOf(
    Color(0xFF43D3B3),
    Color(0xFF5C8DFF),
    Color(0xFFB778FF),
    Color(0xFFF4B64A),
    Color(0xFFE95A64),
    Color(0xFF55C98A),
)[index % 6]

@Composable
private fun ChannelStrip(index: Int, track: MixerTrackUi, gain: (Float) -> Unit, pan: (Float) -> Unit, mute: () -> Unit, solo: () -> Unit) {
    val stripColor = Color(track.colorArgb)
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    Surface(
        if (landscape) Modifier.fillMaxWidth().height(142.dp) else Modifier.width(208.dp).fillMaxHeight(),
        color = if (index % 2 == 0) Panel else Color(0xFF1C1E20),
        shape = RoundedCornerShape(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        if (landscape) {
            LandscapeChannelStrip(index, track, stripColor, gain, pan, mute, solo)
        } else {
            PortraitChannelStrip(index, track, stripColor, gain, pan, mute, solo)
        }
    }
}

@Composable
private fun LandscapeChannelStrip(
    index: Int,
    track: MixerTrackUi,
    stripColor: Color,
    gain: (Float) -> Unit,
    pan: (Float) -> Unit,
    mute: () -> Unit,
    solo: () -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(stripColor))
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(track.name, Modifier.weight(1f), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (track.isClickReference) "REF" else (index + 1).toString().padStart(2, '0'),
                    color = if (track.isClickReference) Amber else TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(color = Border)
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PeakMeter(track.peak, stripColor, Modifier.width(10.dp).fillMaxHeight(.82f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    LabelValue(tr("NIVEL", "LEVEL"), formatDb(track.gainDb), stripColor)
                    HorizontalConsoleFader(track.gainDb, gain, stripColor, Modifier.fillMaxWidth())
                }
                Column(Modifier.width(82.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PAN", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RotaryKnob(track.pan, -1f..1f, pan, stripColor, Modifier.size(48.dp))
                        Text(panLabel(track.pan), color = stripColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.width(112.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToggleButton("M", track.muted, Red, mute, Modifier.weight(1f))
                    ToggleButton("S", track.soloed, Amber, solo, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PortraitChannelStrip(
    index: Int,
    track: MixerTrackUi,
    stripColor: Color,
    gain: (Float) -> Unit,
    pan: (Float) -> Unit,
    mute: () -> Unit,
    solo: () -> Unit,
) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(stripColor))
            Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(track.name, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (track.isClickReference) "REF" else (index + 1).toString().padStart(2, '0'),
                    color = if (track.isClickReference) Amber else TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = if (track.isClickReference) FontWeight.Bold else FontWeight.Normal,
                )
            }
            HorizontalDivider(color = Border)
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PeakMeter(track.peak, stripColor, Modifier.width(10.dp).fillMaxHeight())
                Spacer(Modifier.width(8.dp))
                VerticalFader(track.gainDb, gain, stripColor, Modifier.weight(1f).fillMaxHeight())
            }
            Text(formatDb(track.gainDb), color = stripColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
                RotaryKnob(track.pan, -1f..1f, pan, stripColor, Modifier.size(52.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("PAN", color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                    Text(panLabel(track.pan), color = TextMain, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToggleButton("M", track.muted, Red, mute, Modifier.weight(1f))
                ToggleButton("S", track.soloed, Amber, solo, Modifier.weight(1f))
            }
            Box(Modifier.fillMaxWidth().height(4.dp).background(stripColor))
        }
}

@Composable
private fun HorizontalConsoleFader(value: Float, change: (Float) -> Unit, color: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.height(44.dp)) {
        val capWidth = 40.dp
        val normalized = ((value + 60f) / 66f).coerceIn(0f, 1f)
        Slider(
            value = value,
            onValueChange = change,
            valueRange = -60f..6f,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = color,
                inactiveTrackColor = Border,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth - capWidth) * normalized)
                .size(width = capWidth, height = 22.dp)
                .background(Silver, RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF73777A), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(24.dp).height(2.dp).background(color, RoundedCornerShape(1.dp)))
        }
    }
}

@Composable
private fun VerticalFader(value: Float, change: (Float) -> Unit, color: Color, modifier: Modifier = Modifier) {
    val currentValue by rememberUpdatedState(value)
    Canvas(
        modifier.pointerInput(Unit) {
            var workingValue = currentValue
            detectVerticalDragGestures(
                onDragStart = { workingValue = currentValue },
                onVerticalDrag = { event, dragAmount ->
                    event.consume()
                    val delta = if (size.height > 0) -dragAmount / size.height * 66f else 0f
                    workingValue = (workingValue + delta).coerceIn(-60f, 6f)
                    change(workingValue)
                },
            )
        },
    ) {
        val x = size.width * .52f
        val top = 5.dp.toPx()
        val bottom = size.height - 5.dp.toPx()
        drawLine(Color(0xFF111214), androidx.compose.ui.geometry.Offset(x, top), androidx.compose.ui.geometry.Offset(x, bottom), 8.dp.toPx(), StrokeCap.Round)
        listOf(0f, .18f, .36f, .55f, .73f, 1f).forEach { fraction ->
            val y = top + (bottom - top) * fraction
            drawLine(TextMuted.copy(alpha = .45f), androidx.compose.ui.geometry.Offset(x - 18.dp.toPx(), y), androidx.compose.ui.geometry.Offset(x + 18.dp.toPx(), y), 1.dp.toPx())
        }
        val normalized = (6f - value) / 66f
        val y = top + (bottom - top) * normalized
        drawLine(color.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset(x, bottom), 8.dp.toPx(), StrokeCap.Round)
        drawRoundRect(color = Color(0xFF777B7E), topLeft = androidx.compose.ui.geometry.Offset(x - 24.dp.toPx(), y - 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(48.dp.toPx(), 12.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        drawLine(color, androidx.compose.ui.geometry.Offset(x - 17.dp.toPx(), y), androidx.compose.ui.geometry.Offset(x + 17.dp.toPx(), y), 2.dp.toPx())
    }
}

@Composable
private fun RotaryKnob(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    change: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val currentValue by rememberUpdatedState(value)
    Canvas(
        modifier.pointerInput(Unit) {
            var working = currentValue
            detectDragGestures(
                onDragStart = { working = currentValue },
                onDrag = { event, amount ->
                    event.consume()
                    val span = range.endInclusive - range.start
                    val delta = (amount.x - amount.y) / (minOf(size.width, size.height).coerceAtLeast(1)) * span * .7f
                    working = (working + delta).coerceIn(range.start, range.endInclusive)
                    change(working)
                },
            )
        },
    ) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * .34f
        drawCircle(Color(0xFF121315), radius * 1.25f, center)
        drawArc(Border, 135f, 270f, false, topLeft = androidx.compose.ui.geometry.Offset(center.x - radius * 1.18f, center.y - radius * 1.18f), size = androidx.compose.ui.geometry.Size(radius * 2.36f, radius * 2.36f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val normalized = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        drawArc(color, 135f, 270f * normalized, false, topLeft = androidx.compose.ui.geometry.Offset(center.x - radius * 1.18f, center.y - radius * 1.18f), size = androidx.compose.ui.geometry.Size(radius * 2.36f, radius * 2.36f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color(0xFF56595C), radius, center)
        drawCircle(Color.White.copy(alpha = .08f), radius * .78f, center - androidx.compose.ui.geometry.Offset(radius * .13f, radius * .13f))
        val degrees = 135f + 270f * normalized
        val radians = degrees / 180f * PI.toFloat()
        val end = androidx.compose.ui.geometry.Offset(center.x + cos(radians) * radius * .72f, center.y + sin(radians) * radius * .72f)
        drawLine(color, center, end, 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun KnobControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    change: (Float) -> Unit,
    color: Color,
    readout: String,
    size: androidx.compose.ui.unit.Dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        RotaryKnob(value, range, change, color, Modifier.size(size))
        Text(readout, color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetronomeScreen(state: MainUiState, vm: MainViewModel) {
    val project = state.selectedProject(); val master = state.selectedMaster()
    if (project == null) {
        ConsolePanel(Modifier.fillMaxSize()) { EmptyState(
            tr("SIN PROYECTO", "NO PROJECT"),
            tr("El click y el ruteo pertenecen a un proyecto.", "Click and routing belong to a project."),
            tr("IR A PROYECTOS", "GO TO PROJECTS"),
        ) { vm.setWorkspace(Workspace.PROJECTS) } }; return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp && maxWidth > maxHeight
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Amber.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                        VectorIcon(R.drawable.ic_ui_metronome, null, Amber, Modifier.size(25.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tr("CLICK Y TEMPO", "CLICK AND TEMPO"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(master?.name ?: tr("Selecciona una pista", "Select a track"), color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
            item {
                MasterMetronomePanel(
                    project,
                    master,
                    vm,
                    Modifier.fillMaxWidth().height(if (wide) 430.dp else 740.dp),
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    VectorIcon(R.drawable.ic_ui_routing, null, Mint, Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(tr("RUTEO DE SALIDA", "OUTPUT ROUTING"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(tr("MAIN, monitor y hardware actual", "MAIN, monitor and current hardware"), color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
            if (wide) item {
                Row(Modifier.fillMaxWidth().height(168.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RouteCard("SINGLE MIX", "MAIN L/R", tr("Mezcla estéreo principal", "Main stereo mix"), !state.stereoSplit, { vm.setStereoSplit(false) }, Modifier.weight(1f))
                    RouteCard("STEREO SPLIT", "L MAIN · R MON", tr("Click aislado en monitor", "Click isolated in monitor"), state.stereoSplit, { vm.setStereoSplit(true) }, Modifier.weight(1f))
                }
            } else {
                item { RouteCard("SINGLE MIX", "MAIN L/R", tr("Mezcla estéreo principal", "Main stereo mix"), !state.stereoSplit, { vm.setStereoSplit(false) }, Modifier.fillMaxWidth().height(148.dp)) }
                item { RouteCard("STEREO SPLIT", "L MAIN · R MON", tr("Click aislado en monitor", "Click isolated in monitor"), state.stereoSplit, { vm.setStereoSplit(true) }, Modifier.fillMaxWidth().height(148.dp)) }
            }
            item { RoutingHardwareModule(state, vm, Modifier.fillMaxWidth().height(if (wide) 84.dp else 112.dp)) }
        }
    }
}

@Composable
private fun MasterSectionStrip(
    section: MasterSection,
    select: (MasterSection) -> Unit,
    modifier: Modifier,
) {
    Surface(modifier.height(52.dp), color = Panel, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Border.copy(alpha = .55f))) {
        Row(Modifier.fillMaxSize().padding(3.dp)) {
            MasterSection.entries.forEach { item ->
                MasterSectionButton(
                    item,
                    section == item,
                    Modifier.weight(1f).fillMaxHeight().padding(3.dp),
                ) { select(item) }
            }
        }
    }
}

@Composable
private fun MasterSectionRail(
    section: MasterSection,
    select: (MasterSection) -> Unit,
    modifier: Modifier,
) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Border.copy(alpha = .55f))) {
        Column(Modifier.fillMaxSize().padding(3.dp)) {
            MasterSection.entries.forEach { item ->
                MasterSectionButton(
                    item,
                    section == item,
                    Modifier.weight(1f).fillMaxWidth().padding(3.dp),
                ) { select(item) }
            }
        }
    }
}

@Composable
private fun MasterSectionButton(item: MasterSection, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    val title = item.title()
    Surface(
        onClick = click,
        modifier = modifier.semantics { contentDescription = title; role = Role.Button },
        color = if (selected) Mint.copy(alpha = .15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            VectorIcon(item.iconRes, null, if (selected) Mint else TextMuted, Modifier.size(23.dp))
        }
    }
}

@Composable
private fun MasterSectionContent(
    section: MasterSection,
    project: Project,
    master: MasterTrack?,
    state: MainUiState,
    vm: MainViewModel,
    modifier: Modifier,
) {
    AnimatedContent(
        targetState = section,
        transitionSpec = {
            (fadeIn(tween(170)) + slideInVertically(tween(180)) { it / 12 }) togetherWith
                (fadeOut(tween(100)) + slideOutVertically(tween(120)) { -it / 16 })
        },
        modifier = modifier,
        label = "masterSection",
    ) { activeSection ->
        when (activeSection) {
            MasterSection.PROJECT -> ProjectMasterPanel(project, vm)
            MasterSection.TRACK -> MasterTrackPanel(master, vm)
            MasterSection.METRONOME -> MasterMetronomePanel(project, master, vm)
            MasterSection.ROUTING -> RoutingPanel(state, vm)
        }
    }
}

private enum class MasterSection(val label: String, val iconRes: Int) {
    PROJECT("PROYECTO", R.drawable.ic_ui_projects),
    TRACK("PISTA MASTER", R.drawable.ic_ui_track),
    METRONOME("METRONOMO", R.drawable.ic_ui_metronome),
    ROUTING("RUTEO", R.drawable.ic_ui_routing),
}

@Composable
private fun MasterSection.title() = when (this) {
    MasterSection.PROJECT -> tr("Salida del show", "Show output")
    MasterSection.TRACK -> tr("Salida de canción", "Song output")
    MasterSection.METRONOME -> tr("Click y tempo", "Click and tempo")
    MasterSection.ROUTING -> tr("Ruteo de salida", "Output routing")
}

@Composable
private fun ProjectMasterPanel(project: Project, vm: MainViewModel) {
    val summaryRows = listOf(
        tr("Canciones", "Songs") to project.playlist.size.toString(),
        tr("Stems", "Stems") to project.playlist.sumOf { it.tracks.size }.toString(),
        tr("Plantilla de click", "Click template") to
            "${formatBpm(project.defaultMetronome.bpm)} BPM · ${project.defaultMetronome.numerator}/${project.defaultMetronome.denominator}",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MasterOutputModule(
                    title = tr("SALIDA DEL SHOW", "SHOW OUTPUT"),
                    gainDb = project.masterGainDb,
                    pan = project.masterPan,
                    setGain = vm::setProjectGain,
                    setPan = vm::setProjectPan,
                    footer = tr("Afecta toda la playlist", "Controls the entire setlist"),
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
                MasterSummaryModule(tr("RESUMEN DEL SHOW", "SHOW SUMMARY"), summaryRows, Modifier.fillMaxWidth().height(220.dp))
            }
        } else Row(Modifier.fillMaxSize().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MasterOutputModule(
                title = tr("SALIDA DEL SHOW", "SHOW OUTPUT"),
                gainDb = project.masterGainDb,
                pan = project.masterPan,
                setGain = vm::setProjectGain,
                setPan = vm::setProjectPan,
                footer = tr("Afecta toda la playlist", "Controls the entire setlist"),
                modifier = Modifier.width(400.dp).fillMaxHeight(),
            )
            MasterSummaryModule(tr("RESUMEN DEL SHOW", "SHOW SUMMARY"), summaryRows, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun MasterMetronomePanel(project: Project, master: MasterTrack?, vm: MainViewModel, modifier: Modifier = Modifier.fillMaxSize()) {
    var projectTemplate by rememberSaveable(master?.id) { mutableStateOf(false) }
    BoxWithConstraints(modifier.padding(top = 4.dp)) {
        val wide = maxWidth >= 680.dp
        if (wide) {
            val inherited = master?.metronomeOverride == null
            val clickReference = master?.clickReferenceTrack()
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetronomeWorkspaceRail(
                    projectTemplate = projectTemplate,
                    inherited = inherited,
                    hasSelectedSong = master != null,
                    clickReferenceName = clickReference?.name,
                    selectTemplate = { projectTemplate = true },
                    selectInherited = {
                        projectTemplate = false
                        vm.setMasterUsesDefault(true)
                    },
                    selectCustom = {
                        projectTemplate = false
                        vm.setMasterUsesDefault(false)
                    },
                    modifier = Modifier.width(170.dp).fillMaxHeight(),
                )
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (projectTemplate) {
                        DefaultMetronomeModule(project, vm, Modifier.fillMaxSize())
                    } else if (master == null) {
                        MissingMasterMetronome(vm)
                    } else {
                        MetronomeControlCard(
                            value = master.metronome(project.defaultMetronome),
                            enabled = !inherited,
                            usingReferenceStem = clickReference != null,
                            vm = vm,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else {
            val inherited = master?.metronomeOverride == null
            val clickReference = master?.clickReferenceTrack()
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MetronomeWorkspaceStrip(
                    projectTemplate = projectTemplate,
                    inherited = inherited,
                    hasSelectedSong = master != null,
                    selectTemplate = { projectTemplate = true },
                    selectInherited = {
                        projectTemplate = false
                        vm.setMasterUsesDefault(true)
                    },
                    selectCustom = {
                        projectTemplate = false
                        vm.setMasterUsesDefault(false)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (projectTemplate) {
                        DefaultMetronomeModule(project, vm, Modifier.fillMaxSize())
                    } else if (master == null) {
                        MissingMasterMetronome(vm)
                    } else {
                        MetronomeControlCard(
                            value = master.metronome(project.defaultMetronome),
                            enabled = !inherited,
                            usingReferenceStem = clickReference != null,
                            vm = vm,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetronomeWorkspaceStrip(
    projectTemplate: Boolean,
    inherited: Boolean,
    hasSelectedSong: Boolean,
    selectTemplate: () -> Unit,
    selectInherited: () -> Unit,
    selectCustom: () -> Unit,
    modifier: Modifier,
) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Border.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactMetronomeModeChoice(
                tr("PLANTILLA", "TEMPLATE"),
                projectTemplate,
                modifier = Modifier.weight(1f),
                click = selectTemplate,
            )
            CompactMetronomeModeChoice(
                tr("HEREDA", "INHERITS"),
                !projectTemplate && inherited,
                enabled = hasSelectedSong,
                modifier = Modifier.weight(1f),
                click = selectInherited,
            )
            CompactMetronomeModeChoice(
                tr("PROPIO", "CUSTOM"),
                !projectTemplate && !inherited,
                enabled = hasSelectedSong,
                modifier = Modifier.weight(1f),
                click = selectCustom,
            )
        }
    }
}

@Composable
private fun MetronomeWorkspaceRail(
    projectTemplate: Boolean,
    inherited: Boolean,
    hasSelectedSong: Boolean,
    clickReferenceName: String?,
    selectTemplate: () -> Unit,
    selectInherited: () -> Unit,
    selectCustom: () -> Unit,
    modifier: Modifier,
) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Border.copy(alpha = .7f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ConsoleSectionLabel(tr("RELOJ", "CLOCK"), Mint)
            CompactMetronomeModeChoice(tr("PLANTILLA", "TEMPLATE"), projectTemplate, modifier = Modifier.fillMaxWidth(), click = selectTemplate)
            CompactMetronomeModeChoice(
                tr("HEREDA", "INHERITS"),
                !projectTemplate && inherited,
                enabled = hasSelectedSong,
                modifier = Modifier.fillMaxWidth(),
                click = selectInherited,
            )
            CompactMetronomeModeChoice(
                tr("PERSONALIZADO", "CUSTOM"),
                !projectTemplate && !inherited,
                enabled = hasSelectedSong,
                modifier = Modifier.fillMaxWidth(),
                click = selectCustom,
            )
            clickReferenceName?.let {
                Text(it, color = Amber, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MasterOutputModule(
    title: String,
    gainDb: Float,
    pan: Float,
    setGain: (Float) -> Unit,
    setPan: (Float) -> Unit,
    footer: String,
    modifier: Modifier,
) {
    ConsolePanel(modifier, padding = 0.dp) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Eyebrow(title)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                KnobControl("VOLUME", gainDb, -60f..6f, setGain, Mint, formatDb(gainDb), 92.dp)
                KnobControl("PAN", pan, -1f..1f, setPan, Amber, panLabel(pan), 76.dp)
            }
            Text(footer, color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MasterSummaryModule(
    title: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    ConsolePanel(modifier, padding = 0.dp) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Eyebrow(title) }
                action?.invoke()
            }
            Spacer(Modifier.height(8.dp))
            rows.forEach { (label, value) -> SummaryLine(label, value) }
        }
    }
}

@Composable
private fun DefaultMetronomeModule(project: Project, vm: MainViewModel, modifier: Modifier) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(8.dp)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Amber.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    VectorIcon(R.drawable.ic_ui_metronome, null, Amber, Modifier.size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("PLANTILLA DEL PROYECTO", "PROJECT TEMPLATE"), color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(tr("Click predeterminado", "Default click"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(tr("No reproduce globalmente; cada canción hereda estos valores.", "It never plays globally; each song inherits these values."), color = TextMuted, fontSize = 10.sp)
                }
            }
            MetronomeControls(project.defaultMetronome, true, usingReferenceStem = false) { transform ->
                vm.updateDefaultMetronome(transform)
            }
        }
    }
}

@Composable
private fun MasterTrackPanel(master: MasterTrack?, vm: MainViewModel) {
    if (master == null) { EmptyState(
        tr("SIN PISTA SELECCIONADA", "NO TRACK SELECTED"),
        tr("Selecciona una pista de la playlist.", "Select a track from the playlist."),
        tr("ABRIR PLAYLIST", "OPEN PLAYLIST"),
    ) { vm.setWorkspace(Workspace.PLAYLIST); vm.setPlaylistWorkspace(PlaylistWorkspace.LIST) }; return }
    val summaryRows = listOf(
        tr("Stems", "Stems") to master.tracks.size.toString(),
        tr("Duración", "Duration") to timeText(master.durationSeconds()),
        tr("Última entrada", "Last entry") to
            timeText(master.tracks.maxOfOrNull { it.startOffsetFrames.toDouble() / TIMELINE_SAMPLE_RATE } ?: 0.0),
    )
    val output: @Composable (Modifier) -> Unit = { modifier ->
        MasterOutputModule(
            title = tr("SALIDA DE CANCIÓN", "SONG OUTPUT") + " · ${master.name}",
            gainDb = master.gainDb,
            pan = master.pan,
            setGain = vm::setMasterGain,
            setPan = vm::setMasterPan,
            footer = tr("Afecta todos los stems de esta canción", "Controls every stem in this song"),
            modifier = modifier,
        )
    }
    val summary: @Composable (Modifier) -> Unit = { modifier ->
        MasterSummaryModule(
            title = tr("RESUMEN DE SEÑAL", "SIGNAL SUMMARY"),
            rows = summaryRows,
            modifier = modifier,
            action = {
                DawIconButton(
                    DawIcon.MIXER,
                    tr("Abrir consola de mezcla", "Open mix console"),
                ) { vm.setWorkspace(Workspace.TRACK); vm.setTrackWorkspace(TrackWorkspace.MIXER) }
            },
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                output(Modifier.fillMaxWidth().height(220.dp))
                summary(Modifier.fillMaxWidth().height(220.dp))
            }
        } else {
            Row(Modifier.fillMaxSize().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                output(Modifier.width(400.dp).fillMaxHeight())
                summary(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun MissingMasterMetronome(vm: MainViewModel) {
    EmptyState(
        tr("SIN PISTA SELECCIONADA", "NO TRACK SELECTED"),
        tr("El metrónomo siempre pertenece a una pista master.", "The metronome always belongs to a master track."),
        tr("ABRIR PLAYLIST", "OPEN PLAYLIST"),
    ) { vm.setWorkspace(Workspace.PLAYLIST); vm.setPlaylistWorkspace(PlaylistWorkspace.LIST) }
}

@Composable
private fun CompactMetronomeModeChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    click: () -> Unit,
) {
    Surface(
        onClick = click,
        enabled = enabled,
        modifier = modifier.height(36.dp),
        color = if (selected) Blue.copy(alpha = .2f) else Raised,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, if (selected) Mint else Border),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(12.dp).then(
                    if (selected) Modifier.background(Mint, CircleShape)
                    else Modifier.border(1.dp, TextMuted, CircleShape),
                ),
            )
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (!enabled) TextMuted.copy(alpha = .4f) else if (selected) TextMain else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun MetronomeControlCard(
    value: MetronomeSettings,
    enabled: Boolean,
    usingReferenceStem: Boolean,
    vm: MainViewModel,
    modifier: Modifier,
) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Border.copy(alpha = .7f))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            MetronomeControls(value, enabled, usingReferenceStem) { transform -> vm.updateMasterMetronome(transform) }
            if (usingReferenceStem) {
                Spacer(Modifier.height(12.dp))
                SettingsNotice(tr(
                    "El stem de referencia reemplaza el click nativo y sale sólo por MONITOR. BPM y compás siguen controlando la grilla y las marcas.",
                    "The reference stem replaces the native click and routes to MONITOR only. BPM and meter still control the grid and markers.",
                ))
            }
        }
    }
}

@Composable
private fun MetronomeControls(
    value: MetronomeSettings,
    enabled: Boolean,
    usingReferenceStem: Boolean,
    update: ((MetronomeSettings) -> MetronomeSettings) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetronomeClockConsole(value, enabled, update, Modifier.fillMaxWidth())
        NativeClickControl(value, enabled, usingReferenceStem, update, Modifier.fillMaxWidth())
        MainAuditionControl(value, enabled, update, Modifier.fillMaxWidth())
    }
}

@Composable
private fun NativeClickControl(
    value: MetronomeSettings,
    enabled: Boolean,
    usingReferenceStem: Boolean,
    update: ((MetronomeSettings) -> MetronomeSettings) -> Unit,
    modifier: Modifier,
) {
    Surface(modifier, color = Raised, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("CLICK NATIVO", "NATIVE CLICK"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (usingReferenceStem) tr("Suspendido por el stem de referencia", "Suspended by the reference stem")
                        else tr("Salida MONITOR protegida", "Protected MONITOR output"),
                        color = if (usingReferenceStem) Amber else TextMuted,
                        fontSize = 9.sp,
                    )
                }
                Switch(
                    checked = value.enabled && !usingReferenceStem,
                    onCheckedChange = { update { old -> old.copy(enabled = it) } },
                    enabled = enabled && !usingReferenceStem,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Bg,
                        checkedTrackColor = Mint,
                        checkedBorderColor = Mint,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Panel,
                        uncheckedBorderColor = Border,
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            LabelValue(tr("NIVEL DEL CLICK", "CLICK LEVEL"), formatDb(value.gainDb), Amber)
            Slider(value.gainDb, { gain -> update { it.copy(gainDb = gain) } }, valueRange = -60f..0f, enabled = enabled)
        }
    }
}

@Composable
private fun MainAuditionControl(
    value: MetronomeSettings,
    enabled: Boolean,
    update: ((MetronomeSettings) -> MetronomeSettings) -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier,
        color = Red.copy(alpha = .06f),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, if (value.mainEnabled) Red.copy(alpha = .7f) else Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("AUDICIÓN EN MAIN", "AUDITION ON MAIN"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(tr("Sólo para prueba; apagado por seguridad", "Testing only; off for safety"), color = Red, fontSize = 9.sp)
            }
            Switch(
                checked = value.mainEnabled,
                onCheckedChange = { main -> update { it.copy(mainEnabled = main) } },
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Bg,
                    checkedTrackColor = Red,
                    checkedBorderColor = Red,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Panel,
                    uncheckedBorderColor = Border,
                ),
            )
        }
    }
}

@Composable
private fun MetronomeClockConsole(
    value: MetronomeSettings,
    enabled: Boolean,
    update: ((MetronomeSettings) -> MetronomeSettings) -> Unit,
    modifier: Modifier,
) {
    val configuration = LocalConfiguration.current
    val portrait = configuration.screenHeightDp > configuration.screenWidthDp
    Surface(modifier, color = Raised, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Border)) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(14.dp)) {
            val stacked = portrait || maxWidth < 230.dp
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TempoConsoleModule(value, enabled, update, Modifier.fillMaxWidth())
                Box(Modifier.fillMaxWidth().height(1.dp).background(Border.copy(alpha = .7f)))
                MeterConsoleModule(value, enabled, update, Modifier.fillMaxWidth())
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                TempoConsoleModule(value, enabled, update, Modifier.weight(1f))
                Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(116.dp).background(Border.copy(alpha = .7f)))
                MeterConsoleModule(value, enabled, update, Modifier.weight(1.08f))
            }
        }
    }
}

@Composable
private fun TempoConsoleModule(
    value: MetronomeSettings,
    enabled: Boolean,
    update: ((MetronomeSettings) -> MetronomeSettings) -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        ConsoleSectionLabel("TEMPO", Mint)
        Spacer(Modifier.height(7.dp))
        EditableBpmField(value.bpm, enabled, Modifier.fillMaxWidth().semantics { contentDescription = "BPM" }) { bpm ->
            update { it.copy(bpm = bpm) }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetronomeStepButton(R.drawable.ic_ui_remove, tr("Bajar tempo", "Decrease tempo"), enabled, Modifier.weight(1f)) {
                update { it.copy(bpm = (it.bpm - 1.0).coerceAtLeast(20.0)) }
            }
            MetronomeStepButton(R.drawable.ic_ui_add, tr("Subir tempo", "Increase tempo"), enabled, Modifier.weight(1f)) {
                update { it.copy(bpm = (it.bpm + 1.0).coerceAtMost(400.0)) }
            }
        }
    }
}

@Composable
private fun MeterConsoleModule(
    value: MetronomeSettings,
    enabled: Boolean,
    update: ((MetronomeSettings) -> MetronomeSettings) -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        ConsoleSectionLabel(tr("COMPÁS", "METER"), Amber)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            EditableMeterPart(
                label = tr("Numerador", "Numerator"),
                value = value.numerator,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { numerator -> update { it.copy(numerator = numerator) } }
            Text(
                "/",
                modifier = Modifier.height(58.dp).wrapContentHeight(Alignment.CenterVertically),
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
            )
            EditableMeterPart(
                label = tr("Denominador", "Denominator"),
                value = value.denominator,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { denominator -> update { it.copy(denominator = denominator) } }
        }
    }
}

@Composable
private fun ConsoleSectionLabel(label: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(10.dp).background(accent, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(7.dp))
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, maxLines = 1)
    }
}

private fun formatBpm(value: Double): String = if (value % 1.0 == 0.0) {
    value.roundToInt().toString()
} else {
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

@Composable
private fun EditableBpmField(value: Double, enabled: Boolean, modifier: Modifier, change: (Double) -> Unit) {
    var text by rememberSaveable { mutableStateOf(formatBpm(value)) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun commit() {
        val next = text.replace(',', '.').toDoubleOrNull()?.coerceIn(20.0, 400.0) ?: value
        text = formatBpm(next)
        if (next != value) change(next)
    }

    LaunchedEffect(value, focused) {
        if (!focused) text = formatBpm(value)
    }

    ConsoleNumericReadout(
        value = text,
        onValueChange = { candidate ->
            if (candidate.length <= 6 && candidate.count { it == '.' || it == ',' } <= 1 && candidate.all { it.isDigit() || it == '.' || it == ',' }) {
                text = candidate
            }
        },
        modifier = modifier,
        enabled = enabled,
        accent = Mint,
        suffix = null,
        textSize = 27,
        keyboardType = KeyboardType.Decimal,
        focused = focused,
        onFocusChanged = { isFocused ->
            if (focused && !isFocused) commit()
            focused = isFocused
        },
        onDone = { commit(); focusManager.clearFocus() },
    )
}

@Composable
private fun ConsoleNumericReadout(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    accent: Color,
    modifier: Modifier,
    suffix: String? = null,
    textSize: Int = 20,
    keyboardType: KeyboardType,
    focused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    Surface(
        modifier = modifier.height(58.dp),
        color = Bg,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, if (focused) accent else Border.copy(alpha = .85f)),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize().onFocusChanged { state -> onFocusChanged(state.isFocused) },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = if (enabled) accent else TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = textSize.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            cursorBrush = SolidColor(accent),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        innerTextField()
                    }
                    suffix?.let {
                        Text(
                            it,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 5.dp),
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = .8.sp,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun EditableMeterPart(label: String, value: Int, enabled: Boolean, modifier: Modifier, change: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun commit() {
        val next = text.toIntOrNull()?.coerceIn(1, 32) ?: value
        text = next.toString()
        if (next != value) change(next)
    }

    fun step(delta: Int) {
        val next = ((text.toIntOrNull() ?: value) + delta).coerceIn(1, 32)
        text = next.toString()
        change(next)
    }

    LaunchedEffect(value, focused) {
        if (!focused) text = value.toString()
    }

    Column(modifier) {
        ConsoleNumericReadout(
            value = text,
            onValueChange = { candidate ->
                val parsed = candidate.toIntOrNull()
                if (candidate.isEmpty() || (candidate.length <= 2 && parsed != null && parsed in 1..32)) text = candidate
            },
            enabled = enabled,
            accent = Amber,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            textSize = 25,
            keyboardType = KeyboardType.Number,
            focused = focused,
            onFocusChanged = { isFocused ->
                if (focused && !isFocused) commit()
                focused = isFocused
            },
            onDone = { commit(); focusManager.clearFocus() },
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MetronomeStepButton(R.drawable.ic_ui_remove, tr("Disminuir $label", "Decrease $label"), enabled, Modifier.weight(1f)) { step(-1) }
            MetronomeStepButton(R.drawable.ic_ui_add, tr("Aumentar $label", "Increase $label"), enabled, Modifier.weight(1f)) { step(1) }
        }
    }
}

@Composable
private fun MetronomeStepButton(
    iconRes: Int,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    click: () -> Unit,
) {
    Surface(
        onClick = click,
        enabled = enabled,
        modifier = modifier.height(38.dp).semantics { contentDescription = label; role = Role.Button },
        color = Panel,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            VectorIcon(iconRes, null, if (enabled) TextMain else TextMuted.copy(alpha = .35f), Modifier.size(17.dp))
        }
    }
}

@Composable
private fun RoutingPanel(state: MainUiState, vm: MainViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (compact) {
                RouteCard(
                    "SINGLE MIX",
                    "MAIN L/R",
                    tr("Mezcla estéreo principal", "Main stereo mix"),
                    !state.stereoSplit,
                    { vm.setStereoSplit(false) },
                    Modifier.fillMaxWidth().height(148.dp),
                )
                RouteCard(
                    "STEREO SPLIT",
                    "L MAIN · R MON",
                    tr("Click aislado en monitor", "Click isolated on monitor"),
                    state.stereoSplit,
                    { vm.setStereoSplit(true) },
                    Modifier.fillMaxWidth().height(148.dp),
                )
            } else {
                Row(Modifier.fillMaxWidth().height(168.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RouteCard("SINGLE MIX", "MAIN L/R", tr("Mezcla estéreo principal", "Main stereo mix"), !state.stereoSplit, { vm.setStereoSplit(false) }, Modifier.weight(1f))
                    RouteCard("STEREO SPLIT", "L MAIN · R MON", tr("Click aislado en monitor", "Click isolated on monitor"), state.stereoSplit, { vm.setStereoSplit(true) }, Modifier.weight(1f))
                }
            }
            RoutingHardwareModule(state, vm, Modifier.fillMaxWidth().height(if (compact) 104.dp else 84.dp))
        }
    }
}

@Composable
private fun RoutingHardwareModule(state: MainUiState, vm: MainViewModel, modifier: Modifier) {
    ConsolePanel(modifier, padding = 0.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow(tr("HARDWARE ACTUAL", "CURRENT HARDWARE"))
                Text(state.devices.joinToString { it.name }.ifBlank { tr("Salida Android", "Android output") }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${state.diagnostics.actualSampleRate.takeIf { it > 0 } ?: 0} Hz · ${state.diagnostics.actualChannels} ${tr("canales", "channels")} · XRuns ${state.diagnostics.xRuns}",
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = vm::panic, colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White), shape = RoundedCornerShape(8.dp)) {
                Text("MUTE ALL", fontWeight = FontWeight.Black)
            }
        }
    }
}

private enum class SettingsSection { GENERAL, STEMS, ABOUT }

@Composable
private fun SettingsScreen(state: MainUiState, vm: MainViewModel) {
    var section by remember { mutableStateOf(SettingsSection.GENERAL) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(42.dp).background(Panel).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsSection.entries.forEach { item ->
                val label = when (item) {
                    SettingsSection.GENERAL -> tr("GENERAL", "GENERAL")
                    SettingsSection.STEMS -> "STEMS"
                    SettingsSection.ABOUT -> tr("ACERCA DE", "ABOUT")
                }
                SegmentButton(label, section == item) { section = item }
            }
        }
        Spacer(Modifier.height(4.dp))
        Surface(Modifier.fillMaxSize(), color = Panel, shape = RoundedCornerShape(2.dp)) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 700.dp
                AnimatedContent(
                    targetState = section,
                    transitionSpec = {
                        (fadeIn(tween(160)) + slideInVertically(tween(180)) { it / 14 }) togetherWith fadeOut(tween(100))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "settingsSection",
                ) { activeSection ->
                    Column(
                        Modifier.fillMaxHeight().then(if (wide) Modifier.widthIn(max = 820.dp).align(Alignment.TopCenter) else Modifier.fillMaxWidth())
                            .verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 24.dp else 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (activeSection) {
                            SettingsSection.GENERAL -> GeneralSettings(state, vm)
                            SettingsSection.STEMS -> StemSettings(state, vm)
                            SettingsSection.ABOUT -> AboutSettings(state, vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettings(state: MainUiState, vm: MainViewModel) {
    SettingsHeading(tr("Ajustes generales", "General settings"), tr("Preferencias de operación de la aplicación.", "Application operation preferences."))
    SettingsBlock {
        Text(tr("IDIOMA", "LANGUAGE"), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsChoice("ESPAÑOL", state.settings.language == AppLanguage.SPANISH, Modifier.weight(1f)) { vm.setLanguage(AppLanguage.SPANISH) }
            SettingsChoice("ENGLISH", state.settings.language == AppLanguage.ENGLISH, Modifier.weight(1f)) { vm.setLanguage(AppLanguage.ENGLISH) }
        }
    }
    SettingsToggle(
        tr("Mantener pantalla activa", "Keep screen awake"),
        tr("Evita que el dispositivo se suspenda durante el show.", "Prevents the device from sleeping during a show."),
        state.settings.keepScreenAwake,
        vm::setKeepScreenAwake,
    )
    SettingsToggle(
        tr("Modo exclusivo", "Exclusive performance mode"),
        if (state.notificationPolicyAccessGranted) {
            tr("Durante la reproducción toma foco de audio y silencia interrupciones; al pausar restaura el estado anterior.",
                "During playback it takes audio focus and silences interruptions; pausing restores the previous state.")
        } else {
            tr("Requiere conceder acceso a No molestar en Android.", "Requires Do Not Disturb access in Android.")
        },
        state.settings.exclusivePerformanceMode,
        vm::setExclusivePerformanceMode,
    )
    SettingsToggle(
        tr("Confirmar acciones destructivas", "Confirm destructive actions"),
        tr("Solicita confirmación antes de quitar proyectos o pistas.", "Asks before removing projects or playlist tracks."),
        state.settings.confirmDestructiveActions,
        vm::setConfirmDestructiveActions,
    )
    SettingsToggle(
        tr("Buscar actualizaciones automáticamente", "Check for updates automatically"),
        tr("Consulta GitHub al iniciar sin descargar ni instalar nada.", "Checks GitHub at startup without downloading or installing anything."),
        state.settings.automaticUpdateChecks,
        vm::setAutomaticUpdateChecks,
    )
    SettingsToggle(
        tr("Incluir versiones alpha y beta", "Include alpha and beta releases"),
        tr("Recomendado mientras LiveTracks está en pruebas.", "Recommended while LiveTracks is being tested."),
        state.settings.includePrereleaseUpdates,
        vm::setIncludePrereleaseUpdates,
    )
}

@Composable
private fun StemSettings(state: MainUiState, vm: MainViewModel) {
    SettingsHeading(tr("Importación de stems", "Stem imports"), tr("Valores iniciales para archivos nuevos; no altera stems existentes.", "Defaults for new files; existing stems are unchanged."))
    SettingsBlock {
        Text(tr("TIPO PREDETERMINADO", "DEFAULT TYPE"), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(TrackType.MUSIC to tr("MÚSICA", "MUSIC"), TrackType.CLICK to "CLICK", TrackType.CUE to "CUE").forEach { (type, label) ->
                SettingsChoice(label, state.settings.defaultStemType == type, Modifier.weight(1f)) { vm.setDefaultStemType(type) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("ENVÍO MONITOR", "MONITOR SEND"), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(-12f, -6f, 0f).forEach { db ->
                SettingsChoice("${db.toInt()} dB", state.settings.defaultMonitorSendDb == db, Modifier.weight(1f)) { vm.setDefaultMonitorSendDb(db) }
            }
        }
    }
    SettingsToggle(
        tr("Abrir Timeline después de importar", "Open Timeline after import"),
        tr("Si está desactivado, abre directamente la consola de mezcla.", "When disabled, opens the Mix Console directly."),
        state.settings.openTimelineAfterImport,
        vm::setOpenTimelineAfterImport,
    )
    SettingsNotice(tr("Los nombres que contengan “click” o “cue” conservan ruteo seguro y tienen prioridad sobre el tipo predeterminado.", "Names containing “click” or “cue” keep safe routing and override the default type."))
}

@Composable
private fun AboutSettings(state: MainUiState, vm: MainViewModel) {
    val uriHandler = LocalUriHandler.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(84.dp), color = Raised, shape = RoundedCornerShape(2.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Image(painterResource(R.drawable.ic_brand_mark), contentDescription = "LiveTracks", modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text("LiveTracks", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("v${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text(tr("Consola multipista para vivo", "Multitrack live console"), color = Mint, fontSize = 11.sp)
        }
    }
    AppUpdateCard(state.appUpdateStatus, vm)
    OutlinedButton(
        onClick = { uriHandler.openUri("https://github.com/thomrnowtea/livetracks/releases") },
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(8.dp),
    ) { Text(tr("HISTORIAL DE RELEASES", "RELEASE HISTORY"), fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); VectorIcon(R.drawable.ic_ui_external_link, null, TextMain, Modifier.size(19.dp)) }
    SettingsBlock {
        AboutLinkLine(tr("CRÉDITOS", "CREDITS"), "thomrnowtea") { uriHandler.openUri("https://github.com/thomrnowtea/livetracks") }
        HorizontalDivider(color = Border)
        AboutLine(tr("MOTOR", "ENGINE"), "Oboe / C++")
        HorizontalDivider(color = Border)
        AboutLine(tr("PLATAFORMA", "PLATFORM"), "Android 8+")
        HorizontalDivider(color = Border)
        AboutLine(tr("ALMACENAMIENTO", "STORAGE"), tr("Local y versionado", "Local and versioned"))
    }
    SettingsNotice(tr("El ruteo físico y la estabilidad de shows largos deben validarse con el hardware de escenario antes de uso profesional.", "Physical routing and long-show stability must be validated with the stage hardware before professional use."))
}

@Composable
private fun AppUpdateCard(status: AppUpdateStatus, vm: MainViewModel) {
    val accent = when (status) {
        is AppUpdateStatus.Available, is AppUpdateStatus.ReadyToInstall -> Mint
        is AppUpdateStatus.InstallPermissionRequired -> Amber
        is AppUpdateStatus.Failed -> if (status.failure == UpdateFailure.NO_RELEASE) TextMuted else Amber
        else -> TextMuted
    }
    val title = when (status) {
        AppUpdateStatus.Idle -> tr("Actualizaciones", "Software updates")
        AppUpdateStatus.Checking -> tr("Buscando actualizaciones", "Checking for updates")
        is AppUpdateStatus.UpToDate -> tr("LiveTracks está actualizado", "LiveTracks is up to date")
        is AppUpdateStatus.Available -> tr("Actualización disponible", "Update available")
        is AppUpdateStatus.Downloading -> tr("Descargando actualización", "Downloading update")
        is AppUpdateStatus.Verifying -> tr("Verificando APK", "Verifying APK")
        is AppUpdateStatus.ReadyToInstall -> tr("Actualización verificada", "Verified update")
        is AppUpdateStatus.InstallPermissionRequired -> tr("Falta autorizar instalaciones", "Install permission required")
        is AppUpdateStatus.InstallerOpened -> tr("Instalador abierto", "Installer opened")
        is AppUpdateStatus.Failed -> if (status.failure == UpdateFailure.NO_RELEASE) {
            tr("Sin releases disponibles", "No releases available")
        } else {
            tr("No se pudo actualizar", "Update could not be completed")
        }
    }
    val detail = when (status) {
        AppUpdateStatus.Idle -> tr("Comprueba GitHub manualmente cuando quieras.", "Check GitHub manually whenever you want.")
        AppUpdateStatus.Checking -> tr("Consultando releases oficiales…", "Checking official releases…")
        is AppUpdateStatus.UpToDate -> tr("Versión instalada: ${status.installedVersion}", "Installed version: ${status.installedVersion}")
        is AppUpdateStatus.Available -> tr("${status.release.version} está lista para descargar.", "${status.release.version} is ready to download.")
        is AppUpdateStatus.Downloading -> formatDownloadProgress(status.downloadedBytes, status.totalBytes)
        is AppUpdateStatus.Verifying -> tr("Validando SHA-256, paquete y certificado.", "Validating SHA-256, package, and certificate.")
        is AppUpdateStatus.ReadyToInstall -> tr("${status.release.version} superó todas las validaciones.", "${status.release.version} passed every validation.")
        is AppUpdateStatus.InstallPermissionRequired -> tr("Android necesita que habilites LiveTracks como fuente de instalación.", "Android needs you to allow LiveTracks as an install source.")
        is AppUpdateStatus.InstallerOpened -> tr("Confirma la actualización en el instalador de Android.", "Confirm the update in Android's package installer.")
        is AppUpdateStatus.Failed -> updateFailureMessage(status.failure)
    }
    SettingsBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VectorIcon(
                if (status is AppUpdateStatus.ReadyToInstall || status is AppUpdateStatus.UpToDate) R.drawable.ic_ui_verified else R.drawable.ic_ui_refresh,
                null,
                accent,
                Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = TextMuted, fontSize = 10.sp)
            }
        }
        when (status) {
            AppUpdateStatus.Checking, is AppUpdateStatus.Verifying -> {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint, trackColor = Border)
            }
            is AppUpdateStatus.Downloading -> {
                Spacer(Modifier.height(12.dp))
                status.progress?.let { progress ->
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = Border)
                } ?: LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint, trackColor = Border)
            }
            else -> Unit
        }
        Spacer(Modifier.height(12.dp))
        when (status) {
            AppUpdateStatus.Idle, is AppUpdateStatus.UpToDate -> UpdateActionButton(R.drawable.ic_ui_refresh, tr("BUSCAR ACTUALIZACIONES", "CHECK FOR UPDATES")) { vm.checkForUpdates() }
            is AppUpdateStatus.Available -> UpdateActionButton(R.drawable.ic_ui_download, tr("DESCARGAR Y VERIFICAR", "DOWNLOAD AND VERIFY"), vm::downloadUpdate)
            is AppUpdateStatus.ReadyToInstall -> UpdateActionButton(R.drawable.ic_ui_install, tr("INSTALAR ACTUALIZACIÓN", "INSTALL UPDATE"), vm::installUpdate)
            is AppUpdateStatus.InstallPermissionRequired -> UpdateActionButton(R.drawable.ic_ui_settings, tr("HABILITAR INSTALACIONES", "ALLOW INSTALLS"), vm::openInstallPermissionSettings)
            is AppUpdateStatus.InstallerOpened -> UpdateActionButton(R.drawable.ic_ui_install, tr("ABRIR INSTALADOR OTRA VEZ", "OPEN INSTALLER AGAIN"), vm::installUpdate)
            is AppUpdateStatus.Failed -> {
                val retryDownload = status.release != null && status.failure in setOf(UpdateFailure.DOWNLOAD, UpdateFailure.CHECKSUM)
                UpdateActionButton(
                    if (retryDownload) R.drawable.ic_ui_download else R.drawable.ic_ui_refresh,
                    if (retryDownload) tr("REINTENTAR DESCARGA", "RETRY DOWNLOAD") else tr("VOLVER A COMPROBAR", "CHECK AGAIN"),
                    if (retryDownload) vm::downloadUpdate else ({ vm.checkForUpdates() }),
                )
            }
            AppUpdateStatus.Checking, is AppUpdateStatus.Downloading, is AppUpdateStatus.Verifying -> Unit
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("La instalación nunca es silenciosa: Android siempre pide confirmación.", "Installation is never silent: Android always asks for confirmation."), color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun UpdateActionButton(iconRes: Int, label: String, click: () -> Unit) {
    Button(
        onClick = click,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Bg),
        shape = RoundedCornerShape(8.dp),
    ) {
        VectorIcon(iconRes, null, Bg, Modifier.size(21.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun updateFailureMessage(failure: UpdateFailure): String = when (failure) {
    UpdateFailure.NETWORK -> tr("No se pudo conectar con GitHub.", "Could not connect to GitHub.")
    UpdateFailure.RATE_LIMITED -> tr("GitHub limitó temporalmente las consultas.", "GitHub temporarily rate-limited checks.")
    UpdateFailure.NO_RELEASE -> tr("Todavía no hay releases publicadas en este canal.", "No releases are published in this channel yet.")
    UpdateFailure.INVALID_METADATA -> tr("La metadata de la release no es confiable.", "Release metadata is not trusted.")
    UpdateFailure.DOWNLOAD -> tr("La descarga no pudo completarse.", "The download could not be completed.")
    UpdateFailure.CHECKSUM -> tr("El SHA-256 descargado no coincide.", "The downloaded SHA-256 does not match.")
    UpdateFailure.INVALID_PACKAGE -> tr("El APK no corresponde a esta versión de LiveTracks.", "The APK does not match this LiveTracks release.")
    UpdateFailure.SIGNATURE -> tr("La firma del APK no coincide con la aplicación instalada.", "The APK signature does not match the installed app.")
    UpdateFailure.INSTALLER_UNAVAILABLE -> tr("Android no pudo abrir el instalador.", "Android could not open the package installer.")
    UpdateFailure.UNKNOWN -> tr("Ocurrió un error inesperado.", "An unexpected error occurred.")
}

private fun formatDownloadProgress(downloaded: Long, total: Long?): String {
    val downloadedMb = downloaded / (1024.0 * 1024.0)
    return total?.let { "%.1f / %.1f MB".format(downloadedMb, it / (1024.0 * 1024.0)) }
        ?: "%.1f MB".format(downloadedMb)
}

@Composable
private fun SettingsHeading(title: String, subtitle: String) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Text(subtitle, color = TextMuted, fontSize = 11.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsBlock(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Raised, shape = RoundedCornerShape(2.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsToggle(title: String, detail: String, checked: Boolean, change: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Raised, shape = RoundedCornerShape(2.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = TextMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = change,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Bg,
                    checkedTrackColor = Mint,
                    checkedBorderColor = Mint,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Panel,
                    uncheckedBorderColor = Border,
                ),
            )
        }
    }
}

@Composable
private fun SettingsChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier.height(40.dp), color = if (selected) Mint else Panel, shape = RoundedCornerShape(2.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Mint else Border)) {
        Box(contentAlignment = Alignment.Center) { Text(label, color = if (selected) Bg else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SettingsNotice(text: String) {
    Row(Modifier.fillMaxWidth().background(Bg).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(34.dp).background(Amber))
        Spacer(Modifier.width(12.dp))
        Text(text, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AboutLinkLine(label: String, value: String, click: () -> Unit) {
    Surface(onClick = click, color = Color.Transparent, shape = RoundedCornerShape(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            VectorIcon(R.drawable.ic_ui_external_link, null, Mint, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CompactTransport(
    state: MainUiState,
    previous: () -> Unit,
    playPause: () -> Unit,
    stop: () -> Unit,
    next: () -> Unit,
    seek: (Float) -> Unit,
    panic: () -> Unit,
    openTimeline: () -> Unit,
    openMixer: () -> Unit,
    openMetronome: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val project = state.selectedProject()
    val master = state.selectedMaster()
    val masterIndex = project?.playlist?.indexOfFirst { it.id == master?.id } ?: -1
    val canPrevious = masterIndex > 0
    val canNext = project != null && masterIndex >= 0 && masterIndex < project.playlist.lastIndex
    val rate = state.diagnostics.actualSampleRate.takeIf { it > 0 } ?: 48_000
    val engineDuration = state.diagnostics.durationFrames.toDouble() / rate
    val total = maxOf(engineDuration, master?.durationSeconds() ?: 0.0)
    val position = if (state.diagnostics.durationFrames > 0) state.diagnostics.renderedFrames.toDouble() / rate else 0.0
    val fraction = if (total > 0) (position / total).toFloat().coerceIn(0f, 1f) else 0f
    Surface(color = Color(0xFF0C1116)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 600.dp
            val transportHeight by animateDpAsState(if (expanded) 94.dp else if (compact) 82.dp else 60.dp, tween(180), label = "transportHeight")
            Column(Modifier.fillMaxWidth().height(transportHeight).padding(horizontal = 8.dp, vertical = 4.dp)) {
                if (!expanded) {
                    if (compact) {
                        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TransportIconButton(R.drawable.ic_ui_previous, tr("Pista anterior", "Previous track"), canPrevious, previous)
                            Spacer(Modifier.width(4.dp))
                            Button(onClick = playPause, enabled = !state.openingOutput, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (state.diagnostics.toneEnabled) Amber else Mint, contentColor = Bg), shape = CircleShape) {
                                VectorIcon(if (state.diagnostics.toneEnabled) R.drawable.ic_ui_pause else R.drawable.ic_ui_play, tr("Reproducir o pausar", "Play or pause"), Bg, Modifier.size(25.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            TransportIconButton(R.drawable.ic_ui_next, tr("Pista siguiente", "Next track"), canNext, next)
                            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                Text(master?.name ?: tr("Sin pista", "No track"), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${timeText(position)} / ${timeText(total)}", color = Mint, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Box {
                                TransportIconButton(R.drawable.ic_ui_more, tr("Más opciones de transporte", "More transport options"), true) { menuExpanded = true }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    TimelineMenuItem(DawIcon.STOP, tr("Detener", "Stop"), true) { menuExpanded = false; stop() }
                                    TimelineMenuItem(DawIcon.TIMELINE, tr("Abrir timeline", "Open timeline"), master != null) { menuExpanded = false; openTimeline() }
                                    TimelineMenuItem(DawIcon.MIXER, tr("Abrir consola", "Open mix console"), master != null) { menuExpanded = false; openMixer() }
                                    TimelineMenuItem(DawIcon.METRONOME, tr("Abrir click y ruteo", "Open click and routing"), project != null) { menuExpanded = false; openMetronome() }
                                    TimelineMenuItem(DawIcon.PANIC, "PANIC", true, danger = true) { menuExpanded = false; panic() }
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                            TransportIconButton(R.drawable.ic_ui_arrow_up, tr("Expandir transporte", "Expand transport"), true) { expanded = true }
                        }
                        Slider(
                            fraction,
                            seek,
                            enabled = state.diagnostics.durationFrames > 0,
                            colors = SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint, inactiveTrackColor = Border),
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                        )
                    } else {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        TransportIconButton(R.drawable.ic_ui_previous, tr("Pista anterior", "Previous track"), canPrevious, previous)
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = playPause, enabled = !state.openingOutput, modifier = Modifier.size(52.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (state.diagnostics.toneEnabled) Amber else Mint, contentColor = Bg), shape = CircleShape) {
                            VectorIcon(if (state.diagnostics.toneEnabled) R.drawable.ic_ui_pause else R.drawable.ic_ui_play, tr("Reproducir o pausar", "Play or pause"), Bg, Modifier.size(27.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        TransportIconButton(R.drawable.ic_ui_next, tr("Pista siguiente", "Next track"), canNext, next)
                        Spacer(Modifier.width(8.dp))
                        Slider(fraction, seek, enabled = state.diagnostics.durationFrames > 0, modifier = Modifier.weight(1f).height(30.dp))
                        Text("${timeText(position)}/${timeText(total)}", Modifier.width(if (compact) 86.dp else 108.dp), color = Mint, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1)
                        Spacer(Modifier.width(4.dp))
                        Box {
                            TransportIconButton(R.drawable.ic_ui_more, tr("Más opciones de transporte", "More transport options"), true) { menuExpanded = true }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                TimelineMenuItem(DawIcon.STOP, tr("Detener", "Stop"), true) { menuExpanded = false; stop() }
                                TimelineMenuItem(DawIcon.TIMELINE, tr("Abrir timeline", "Open timeline"), master != null) { menuExpanded = false; openTimeline() }
                                TimelineMenuItem(DawIcon.MIXER, tr("Abrir consola", "Open mix console"), master != null) { menuExpanded = false; openMixer() }
                                TimelineMenuItem(DawIcon.METRONOME, tr("Abrir click y ruteo", "Open click and routing"), project != null) { menuExpanded = false; openMetronome() }
                                TimelineMenuItem(DawIcon.PANIC, "PANIC", true, danger = true) { menuExpanded = false; panic() }
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        TransportIconButton(R.drawable.ic_ui_arrow_up, tr("Expandir transporte", "Expand transport"), true) { expanded = true }
                    }
                    }
                } else {
                Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(master?.name ?: tr("Sin pista", "No track"), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!compact) Text("${masterIndex + 1}/${project?.playlist?.size ?: 0}  ·  ${if (state.diagnostics.toneEnabled) "LIVE" else "READY"}", color = if (state.diagnostics.toneEnabled) Mint else TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    TransportIconButton(R.drawable.ic_ui_previous, tr("Pista anterior", "Previous track"), canPrevious, previous)
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = playPause, enabled = !state.openingOutput, modifier = Modifier.size(52.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (state.diagnostics.toneEnabled) Amber else Mint, contentColor = Bg), shape = CircleShape) {
                        VectorIcon(if (state.diagnostics.toneEnabled) R.drawable.ic_ui_pause else R.drawable.ic_ui_play, tr("Reproducir o pausar", "Play or pause"), Bg, Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    TransportIconButton(R.drawable.ic_ui_next, tr("Pista siguiente", "Next track"), canNext, next)
                    if (!compact) {
                        Spacer(Modifier.width(4.dp))
                        TransportIconButton(R.drawable.ic_ui_stop, tr("Detener", "Stop"), true, stop)
                    }
                    Spacer(Modifier.width(4.dp))
                    Box {
                        TransportIconButton(R.drawable.ic_ui_more, tr("Más opciones de transporte", "More transport options"), true) { menuExpanded = true }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (compact) TimelineMenuItem(DawIcon.STOP, tr("Detener", "Stop"), true) { menuExpanded = false; stop() }
                            TimelineMenuItem(DawIcon.TIMELINE, tr("Abrir timeline", "Open timeline"), master != null) { menuExpanded = false; openTimeline() }
                            TimelineMenuItem(DawIcon.MIXER, tr("Abrir consola", "Open mix console"), master != null) { menuExpanded = false; openMixer() }
                            TimelineMenuItem(DawIcon.METRONOME, tr("Abrir click y ruteo", "Open click and routing"), project != null) { menuExpanded = false; openMetronome() }
                            TimelineMenuItem(DawIcon.PANIC, "PANIC", true, danger = true) { menuExpanded = false; panic() }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    TransportIconButton(R.drawable.ic_ui_arrow_down, tr("Minimizar transporte", "Collapse transport"), true) { expanded = false }
                }
                Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                    Slider(fraction, seek, enabled = state.diagnostics.durationFrames > 0, modifier = Modifier.weight(1f).height(30.dp))
                    Text("${timeText(position)}/${timeText(total)}", Modifier.width(if (compact) 92.dp else 108.dp), color = Mint, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    if (!compact) Text("  XR ${state.diagnostics.xRuns}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                }
            }
        }
    }
}

@Composable
private fun TransportIconButton(iconRes: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp).semantics { contentDescription = label; role = Role.Button },
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (enabled) Border else Border.copy(alpha = .35f)),
        shape = RoundedCornerShape(9.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            VectorIcon(iconRes, null, if (enabled) TextMain else TextMuted.copy(alpha = .35f), Modifier.size(21.dp))
        }
    }
}

@Composable
private fun ConsolePanel(modifier: Modifier = Modifier, padding: androidx.compose.ui.unit.Dp = 14.dp, color: Color = Panel, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxSize().padding(padding), content = content)
    }
}

@Composable private fun SectionHeader(title: String, subtitle: String, action: @Composable () -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Eyebrow(title); Text(subtitle, color = TextMuted, fontSize = 9.sp) }; action() }
@Composable private fun Eyebrow(text: String) = Text(text, color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
@Composable
private fun VectorIcon(iconRes: Int, description: String?, tint: Color, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val rotateConsoleIcon = configuration.screenWidthDp > configuration.screenHeightDp &&
        (iconRes == R.drawable.ic_ui_mixer || iconRes == R.drawable.ic_ui_master)
    Image(
        painterResource(iconRes),
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint),
        modifier = if (rotateConsoleIcon) modifier.rotate(90f) else modifier,
    )
}
@Composable private fun PrimarySmall(text: String, onClick: () -> Unit) = Button(onClick = onClick, modifier = Modifier.height(40.dp), contentPadding = PaddingValues(horizontal = 14.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Bg), shape = RoundedCornerShape(8.dp)) { VectorIcon(R.drawable.ic_ui_add, null, Bg, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(text, fontSize = 10.sp, fontWeight = FontWeight.Black) }

@Composable
private fun TinyIconButton(iconRes: Int, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp).semantics { contentDescription = label; role = Role.Button },
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
    ) { Box(contentAlignment = Alignment.Center) { VectorIcon(iconRes, null, TextMuted, Modifier.size(23.dp)) } }
}

@Composable
private fun CompactIconButton(iconRes: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(30.dp).semantics { contentDescription = label; role = Role.Button },
        color = Color.Transparent,
        shape = RoundedCornerShape(6.dp),
    ) { Box(contentAlignment = Alignment.Center) { VectorIcon(iconRes, null, TextMuted, Modifier.size(18.dp)) } }
}

@Composable
private fun EmptyState(title: String, body: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(64.dp).background(Mint.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) { VectorIcon(R.drawable.ic_ui_add, null, Mint, Modifier.size(32.dp)) }
        Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp); Text(body, color = TextMuted, textAlign = TextAlign.Center, fontSize = 11.sp, modifier = Modifier.widthIn(max = 430.dp).padding(vertical = 8.dp))
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Bg)) { Text(action, fontWeight = FontWeight.Black, fontSize = 10.sp) }
    }
}

@Composable private fun MetricCard(label: String, value: String, detail: String, modifier: Modifier) = Surface(modifier, color = Raised, shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(14.dp)) { Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(value, fontFamily = FontFamily.Monospace, fontSize = 19.sp, fontWeight = FontWeight.Black); Text(detail, color = Mint, fontSize = 9.sp) } }
@Composable private fun StepChip(number: String, label: String, ready: Boolean) { Box(Modifier.size(24.dp).background(if (ready) Mint else Border, CircleShape), contentAlignment = Alignment.Center) { if (ready) VectorIcon(R.drawable.ic_ui_check, null, Bg, Modifier.size(15.dp)) else Text(number, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(6.dp)); Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
private enum class DawIcon(val iconRes: Int) {
    ADD(R.drawable.ic_ui_add),
    EDIT(R.drawable.ic_ui_edit),
    OPEN(R.drawable.ic_ui_open),
    MASTER(R.drawable.ic_ui_master),
    MORE(R.drawable.ic_ui_more),
    STAGE(R.drawable.ic_ui_stage),
    PANIC(R.drawable.ic_ui_panic),
    STOP(R.drawable.ic_ui_stop),
    TIMELINE(R.drawable.ic_ui_timeline),
    MIXER(R.drawable.ic_ui_mixer),
    SPLIT(R.drawable.ic_ui_split),
    EXTRACT(R.drawable.ic_ui_extract),
    MARKER(R.drawable.ic_ui_marker),
    METRONOME(R.drawable.ic_ui_metronome),
    DELETE(R.drawable.ic_ui_delete),
    UNDO(R.drawable.ic_ui_undo),
    REDO(R.drawable.ic_ui_redo),
    ZOOM_IN(R.drawable.ic_ui_zoom_in),
    ZOOM_OUT(R.drawable.ic_ui_zoom_out),
    SNAP(R.drawable.ic_ui_snap),
    COLLAPSE(R.drawable.ic_ui_arrow_up),
}

@Composable
private fun TimelineMenuItem(
    icon: DawIcon,
    label: String,
    enabled: Boolean,
    danger: Boolean = false,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 12.sp, color = when { danger && enabled -> Red; active -> Amber; else -> TextMain }) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = { VectorIcon(icon.iconRes, null, when { danger && enabled -> Red; active -> Amber; else -> TextMuted }, Modifier.size(20.dp)) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun TimelineSwitchMenuItem(
    icon: DawIcon,
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    change: (Boolean) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 12.sp, color = if (enabled) TextMain else TextMuted.copy(alpha = .45f)) },
        onClick = { if (enabled) change(!checked) },
        enabled = enabled,
        leadingIcon = { VectorIcon(icon.iconRes, null, if (checked && enabled) Amber else TextMuted, Modifier.size(20.dp)) },
        trailingIcon = {
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) change else null,
                enabled = enabled,
                modifier = Modifier.size(width = 48.dp, height = 32.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Bg,
                    checkedTrackColor = Amber,
                    checkedBorderColor = Amber,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Panel,
                    uncheckedBorderColor = Border,
                ),
            )
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
    )
}

@Composable
private fun DawIconButton(
    icon: DawIcon,
    label: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val ink = when {
        !enabled -> TextMuted.copy(alpha = .35f)
        danger -> Red
        selected -> TextMain
        else -> TextMuted
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp).semantics {
            contentDescription = label
            role = Role.Button
        },
        color = when {
            selected -> Blue
            danger && enabled -> Red.copy(alpha = .08f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(9.dp),
        border = if (selected) BorderStroke(1.dp, Blue) else null,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VectorIcon(icon.iconRes, null, ink, Modifier.size(21.dp))
        }
    }
}

@Composable private fun SegmentButton(label: String, active: Boolean, click: () -> Unit) = Button(click, modifier = Modifier.height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) Blue else Raised, contentColor = if (active) Color.White else TextMuted), contentPadding = PaddingValues(horizontal = 12.dp), shape = RoundedCornerShape(6.dp)) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black) }
@Composable private fun LabelValue(label: String, value: String, color: Color) = Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold); Text(value, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
@Composable
private fun PeakMeter(peak: Float, color: Color, modifier: Modifier) {
    Box(modifier.background(Color(0xFF06090C), RoundedCornerShape(3.dp)), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(peak.coerceIn(0f, 1f)).background(if (peak > .9f) Red else color, RoundedCornerShape(3.dp)))
    }
}

@Composable private fun ToggleButton(label: String, active: Boolean, color: Color, click: () -> Unit, modifier: Modifier) = Button(click, modifier.height(40.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) color else Raised, contentColor = if (active) Bg else TextMuted), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(6.dp)) { Text(label, fontWeight = FontWeight.Black, fontSize = 12.sp) }

@Composable
private fun MasterFader(label: String, value: Float, change: (Float) -> Unit, modifier: Modifier) = ConsolePanel(modifier, color = Raised) {
    Eyebrow(label); Text(formatDb(value), fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Black)
    VerticalFader(value, change, Mint, Modifier.weight(1f).fillMaxWidth())
}

@Composable private fun ControlCard(label: String, value: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) = ConsolePanel(modifier, color = Raised) { Eyebrow(label); Text(value, fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(10.dp)); content() }
@Composable private fun SummaryLine(label: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) { Text(label, Modifier.weight(1f), color = TextMuted, fontSize = 10.sp); Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp) }

@Composable
private fun NumberStepper(label: String, value: Int, enabled: Boolean, modifier: Modifier, denominator: Boolean = false, change: (Int) -> Unit) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(7.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) {
            Text(label, color = TextMuted, fontSize = 7.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value.toString(), Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                TextButton(onClick = { if (enabled) change(if (denominator) when (value) { 16 -> 8; 8 -> 4; 4 -> 2; else -> 2 } else (value - 1).coerceAtLeast(1)) }, enabled = enabled, modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp)) { VectorIcon(R.drawable.ic_ui_remove, tr("Disminuir", "Decrease"), TextMuted, Modifier.size(20.dp)) }
                TextButton(onClick = { if (enabled) change(if (denominator) when (value) { 2 -> 4; 4 -> 8; 8 -> 16; else -> 16 } else (value + 1).coerceAtMost(16)) }, enabled = enabled, modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp)) { VectorIcon(R.drawable.ic_ui_add, tr("Aumentar", "Increase"), TextMuted, Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun RouteCard(title: String, route: String, detail: String, active: Boolean, click: () -> Unit, modifier: Modifier) {
    Surface(onClick = click, modifier = modifier.fillMaxHeight(), color = if (active) Mint.copy(alpha = .1f) else Panel, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Mint else Border.copy(alpha = .5f))) {
        Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).then(if (active) Modifier.background(Mint, CircleShape) else Modifier.border(1.dp, TextMuted, CircleShape))); Spacer(Modifier.width(7.dp)); Text(if (active) tr("ACTIVO", "ACTIVE") else tr("DISPONIBLE", "AVAILABLE"), color = if (active) Mint else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp); Text(route, color = Amber, fontFamily = FontFamily.Monospace, fontSize = 11.sp); Text(detail, color = TextMuted, fontSize = 9.sp) }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, dismiss: () -> Unit, confirm: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("Nombre", "Name")) }) }, confirmButton = { TextButton(onClick = { confirm(value) }, enabled = value.isNotBlank()) { Text(tr("GUARDAR", "SAVE")) } }, dismissButton = { TextButton(onClick = dismiss) { Text(tr("CANCELAR", "CANCEL")) } })
}

@Composable
private fun StemSourceDialog(dismiss: () -> Unit, importAudio: () -> Unit, createEmpty: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Agregar stem", "Add stem")) },
        text = {
            Text(
                tr(
                    "Importa un archivo de audio o crea una región vacía para reservar tiempo y posición.",
                    "Import an audio file or create an empty region to reserve time and position.",
                ),
                color = TextMuted,
            )
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = importAudio, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(tr("IMPORTAR AUDIO", "IMPORT AUDIO"), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = createEmpty, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text(tr("STEM VACÍO", "EMPTY STEM"), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(tr("CANCELAR", "CANCEL")) } },
    )
}

@Composable
private fun EmptyStemDialog(dismiss: () -> Unit, confirm: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    val seconds = duration.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && seconds != null && seconds in 0.001..86_400.0
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Nuevo stem vacío", "New empty stem")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(tr("Nombre", "Name")) },
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(tr("Duración en segundos", "Duration in seconds")) },
                    supportingText = { Text(tr("0,001 s a 24 h", "0.001 s to 24 h")) },
                    isError = duration.isNotBlank() && !valid,
                )
            }
        },
        confirmButton = {
            Button(onClick = { seconds?.let { confirm(name, it) } }, enabled = valid) { Text(tr("CREAR", "CREATE")) }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(tr("CANCELAR", "CANCEL")) } },
    )
}

@Composable
private fun TimelineMarkerDialog(
    marker: TimelineMarker?,
    dismiss: () -> Unit,
    save: (String, TimelineMarkerKind, Boolean, Int) -> Unit,
    delete: (() -> Unit)?,
) {
    var label by remember(marker?.id) { mutableStateOf(marker?.label.orEmpty()) }
    var kind by remember(marker?.id) { mutableStateOf(marker?.kind ?: TimelineMarkerKind.CUSTOM) }
    var voiceEnabled by remember(marker?.id) { mutableStateOf(marker?.voiceCueEnabled ?: true) }
    var leadBeats by remember(marker?.id) { mutableIntStateOf(marker?.voiceLeadBeats ?: 2) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (marker == null) tr("Nueva marca", "New marker") else tr("Editar marca", "Edit marker")) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(48) },
                    singleLine = true,
                    label = { Text(tr("Texto hablado y visible", "Visible and spoken text")) },
                    supportingText = { Text(tr("Ej.: Estribillo, Puente, Solo", "E.g. Chorus, Bridge, Solo")) },
                )
                Text(tr("TIPO DE SECCIÓN", "SECTION TYPE"), color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 360.dp) 3 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimelineMarkerKind.entries.chunked(columns).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { item ->
                                    MarkerKindChoice(
                                        label = markerKindLabel(item),
                                        selected = kind == item,
                                        modifier = Modifier.weight(1f),
                                    ) { kind = item }
                                }
                                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Surface(
                    Modifier.fillMaxWidth(),
                    color = Panel,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (voiceEnabled) Mint.copy(alpha = .55f) else Border),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(Mint.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
                            VectorIcon(R.drawable.ic_ui_voice, null, if (voiceEnabled) Mint else TextMuted, Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Cue de voz", "Voice cue"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(tr("Aviso previo · sólo MONITOR", "Advance cue · MONITOR only"), color = TextMuted, fontSize = 9.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { voiceEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Bg,
                                checkedTrackColor = Mint,
                                checkedBorderColor = Mint,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Raised,
                                uncheckedBorderColor = Border,
                            ),
                        )
                    }
                }
                if (voiceEnabled) {
                    NumberStepper(
                        label = tr("GOLPES DE ANTICIPACIÓN", "LEAD BEATS"),
                        value = leadBeats,
                        enabled = true,
                        modifier = Modifier.fillMaxWidth(),
                        change = { leadBeats = it.coerceIn(0, 16) },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { save(label, kind, voiceEnabled, leadBeats) }, enabled = label.isNotBlank()) {
                Text(tr("GUARDAR", "SAVE"))
            }
        },
        dismissButton = {
            Row {
                if (delete != null) TextButton(onClick = delete, colors = ButtonDefaults.textButtonColors(contentColor = Red)) {
                    Text(tr("ELIMINAR", "DELETE"))
                }
                TextButton(onClick = dismiss) { Text(tr("CANCELAR", "CANCEL")) }
            }
        },
    )
}

@Composable
private fun MarkerKindChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, click: () -> Unit) {
    Surface(
        onClick = click,
        modifier = modifier.height(44.dp),
        color = if (selected) Blue else Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) Blue else Border),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) Color.White else TextMuted,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun markerKindLabel(value: TimelineMarkerKind): String = when (value) {
    TimelineMarkerKind.INTRO -> tr("INTRO", "INTRO")
    TimelineMarkerKind.VERSE -> tr("VERSO", "VERSE")
    TimelineMarkerKind.PRE_CHORUS -> tr("PRE", "PRE")
    TimelineMarkerKind.CHORUS -> tr("ESTRIBILLO", "CHORUS")
    TimelineMarkerKind.BRIDGE -> tr("PUENTE", "BRIDGE")
    TimelineMarkerKind.SOLO -> "SOLO"
    TimelineMarkerKind.BREAKDOWN -> "BREAKDOWN"
    TimelineMarkerKind.OUTRO -> "OUTRO"
    TimelineMarkerKind.CUSTOM -> tr("OTRO", "CUSTOM")
}

@Composable
private fun ConfirmDialog(title: String, body: String, dismiss: () -> Unit, confirm: () -> Unit) = AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(body) }, confirmButton = { TextButton(onClick = confirm, colors = ButtonDefaults.textButtonColors(contentColor = Red)) { Text(tr("ELIMINAR", "DELETE")) } }, dismissButton = { TextButton(onClick = dismiss) { Text(tr("CANCELAR", "CANCEL")) } })

private fun MainUiState.selectedProject(): Project? = projects.firstOrNull { it.id == selectedProjectId }
private fun MainUiState.selectedMaster(): MasterTrack? = selectedProject()?.playlist?.firstOrNull { it.id == selectedMasterTrackId }
private fun TrackType.shortLabel() = when (this) { TrackType.MUSIC -> "MUSIC"; TrackType.CLICK -> "CLICK"; TrackType.CUE -> "CUE"; TrackType.VIDEO_AUDIO_DISABLED -> "VIDEO OFF"; TrackType.OTHER -> "OTHER" }
private fun formatDb(value: Float) = if (value <= -59.5f) "-∞ dB" else "%+.1f dB".format(value)
private fun panLabel(value: Float) = when { value < -.02f -> "L${(-value * 100).roundToInt()}"; value > .02f -> "R${(value * 100).roundToInt()}"; else -> "C" }
private fun timeText(seconds: Double): String { val safe = seconds.coerceAtLeast(0.0).roundToInt(); return "%02d:%02d".format(safe / 60, safe % 60) }
private fun timelineTimecode(frames: Long): String {
    val milliseconds = (frames.coerceAtLeast(0) * 1_000L / TIMELINE_SAMPLE_RATE)
    val minutes = milliseconds / 60_000L
    val seconds = milliseconds / 1_000L % 60L
    val millis = milliseconds % 1_000L
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}
