package com.thomrnowtea.livetracks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomrnowtea.livetracks.domain.MasterTrack
import com.thomrnowtea.livetracks.domain.MetronomeSettings
import com.thomrnowtea.livetracks.domain.Project
import com.thomrnowtea.livetracks.domain.SafetyStatus
import com.thomrnowtea.livetracks.domain.TIMELINE_SAMPLE_RATE
import com.thomrnowtea.livetracks.domain.TrackType
import com.thomrnowtea.livetracks.data.AppLanguage
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
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { viewModel.importTracks(it) }
    val view = LocalView.current
    SideEffect { view.keepScreenOn = state.settings.keepScreenAwake }
    MaterialTheme(colorScheme = ConsoleColors, typography = DawTypography) {
        val systemDensity = LocalDensity.current
        val readableDensity = remember(systemDensity.density, systemDensity.fontScale) {
            Density(systemDensity.density, max(systemDensity.fontScale, 1.12f))
        }
        CompositionLocalProvider(
            LocalAppLanguage provides state.settings.language,
            LocalDensity provides readableDensity,
        ) {
        Scaffold(containerColor = Bg) { insets ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(insets).background(Bg)) {
                val wide = maxWidth >= 700.dp
                val addAudio = { filePicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*")) }
                if (wide) Row(Modifier.fillMaxSize()) {
                    SideNavigation(state.workspace, viewModel::setWorkspace)
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        CompactContextBar(state)
                        WorkspaceContent(state, viewModel, addAudio, Modifier.weight(1f))
                        if (state.workspace != Workspace.SETTINGS) CompactTransport(state, viewModel::playPause, viewModel::stop, viewModel::seekToFraction, viewModel::panic)
                    }
                } else Column(Modifier.fillMaxSize()) {
                    CompactContextBar(state)
                    WorkspaceContent(state, viewModel, addAudio, Modifier.weight(1f))
                    if (state.workspace != Workspace.SETTINGS) CompactTransport(state, viewModel::playPause, viewModel::stop, viewModel::seekToFraction, viewModel::panic)
                    BottomNavigation(state.workspace, viewModel::setWorkspace)
                }
            }
        }
        }
    }
}

@Composable
private fun WorkspaceContent(state: MainUiState, vm: MainViewModel, addAudio: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clipToBounds().padding(horizontal = 8.dp, vertical = 4.dp)) {
        when (state.workspace) {
            Workspace.PROJECTS -> ProjectsScreen(state, vm)
            Workspace.PLAYLIST -> PlaylistScreen(state, vm)
            Workspace.TRACK -> TrackScreen(state, vm, addAudio)
            Workspace.MASTER -> MasterScreen(state, vm)
            Workspace.SETTINGS -> SettingsScreen(state, vm)
        }
    }
}

@Composable
private fun BottomNavigation(active: Workspace, select: (Workspace) -> Unit) {
    Row(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFF1A1B1D)).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Workspace.entries.forEach { item ->
            val selected = item == active
            Surface(onClick = { select(item) }, modifier = Modifier.weight(1f).height(56.dp), color = if (selected) Mint.copy(alpha = .14f) else Color.Transparent, shape = RoundedCornerShape(9.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    VectorIcon(item.iconRes(), null, if (selected) Mint else TextMuted, Modifier.size(24.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(item.railLabel(), color = if (selected) TextMain else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SideNavigation(active: Workspace, select: (Workspace) -> Unit) {
    Column(
        Modifier.width(80.dp).fillMaxHeight().background(Color(0xFF1A1B1D)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(Modifier.size(48.dp), color = Raised, shape = RoundedCornerShape(6.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Image(painterResource(R.drawable.ic_brand_mark), contentDescription = "LiveTracks", modifier = Modifier.padding(7.dp))
        }
        Spacer(Modifier.height(8.dp))
        Workspace.entries.filterNot { it == Workspace.SETTINGS }.forEach { item ->
            val selected = item == active
            Surface(
                onClick = { select(item) },
                modifier = Modifier.width(72.dp).height(60.dp),
                color = if (selected) Mint.copy(alpha = .14f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    VectorIcon(item.iconRes(), null, if (selected) Mint else TextMuted, Modifier.size(27.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(item.railLabel(), color = if (selected) TextMain else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(2.dp))
        }
        Spacer(Modifier.weight(1f))
        val settingsSelected = active == Workspace.SETTINGS
        Surface(onClick = { select(Workspace.SETTINGS) }, modifier = Modifier.width(72.dp).height(60.dp), color = if (settingsSelected) Mint.copy(alpha = .14f) else Color.Transparent, shape = RoundedCornerShape(10.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                VectorIcon(R.drawable.ic_ui_settings, null, if (settingsSelected) Mint else TextMuted, Modifier.size(27.dp))
                Spacer(Modifier.height(4.dp))
                Text(tr("AJUSTES", "SETTINGS"), color = if (settingsSelected) TextMain else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("0.1", color = TextMuted.copy(alpha = .55f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun Workspace.iconRes() = when (this) {
    Workspace.PROJECTS -> R.drawable.ic_ui_projects
    Workspace.PLAYLIST -> R.drawable.ic_ui_playlist
    Workspace.TRACK -> R.drawable.ic_ui_track
    Workspace.MASTER -> R.drawable.ic_ui_master
    Workspace.SETTINGS -> R.drawable.ic_ui_settings
}
@Composable private fun Workspace.railLabel() = when (this) {
    Workspace.PROJECTS -> tr("SHOWS", "SHOWS")
    Workspace.PLAYLIST -> tr("LISTA", "SETLIST")
    Workspace.TRACK -> tr("PISTA", "TRACK")
    Workspace.MASTER -> "MASTER"
    Workspace.SETTINGS -> tr("AJUSTES", "SETTINGS")
}

@Composable
private fun CompactContextBar(state: MainUiState) {
    val project = state.selectedProject(); val master = state.selectedMaster()
    val metro = master?.metronome(project?.defaultMetronome ?: MetronomeSettings())
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(Color(0xFF1A1B1D)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(project?.name ?: tr("Sin proyecto", "No project"), Modifier.weight(1f, fill = false), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (master != null) {
                Text("  /  ", color = TextMuted, fontSize = 12.sp)
                Text(master.name, Modifier.weight(1f, fill = false), color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(metro?.let { "${it.bpm.roundToInt()} BPM  ·  ${it.numerator}/${it.denominator}" } ?: "— BPM", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.width(16.dp))
        val live = state.diagnostics.toneEnabled
        Box(Modifier.size(8.dp).background(if (live) Mint else Amber, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(if (live) "LIVE" else "READY", color = if (live) Mint else Amber, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
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
                Text(project?.name ?: "Sin proyecto", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(master?.name ?: "Selecciona una pista", fontSize = 10.sp, color = TextMuted, maxLines = 1)
            }
            val metro = master?.metronome(project?.defaultMetronome ?: MetronomeSettings())
            HeaderValue("BPM", metro?.bpm?.let { "%.0f".format(it) } ?: "—")
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
    Workspace.PROJECTS -> "PROYECTOS"; Workspace.PLAYLIST -> "PLAYLIST"; Workspace.TRACK -> "PISTA"; Workspace.MASTER -> "MASTER"; Workspace.SETTINGS -> "AJUSTES"
}

@Composable
private fun ProjectsScreen(state: MainUiState, vm: MainViewModel) {
    var dialog by remember { mutableStateOf<ProjectDialog?>(null) }
    val selected = state.selectedProject()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Proyectos", "Projects"), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("  ${state.projects.size}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            if (selected != null) {
                TextButton(onClick = { dialog = ProjectDialog.Rename }) { Text(tr("RENOMBRAR", "RENAME"), fontSize = 8.sp) }
                TextButton(onClick = { if (state.settings.confirmDestructiveActions) dialog = ProjectDialog.Delete else vm.deleteSelectedProject() }, colors = ButtonDefaults.textButtonColors(contentColor = Red)) { Text(tr("ELIMINAR", "DELETE"), fontSize = 8.sp) }
            }
            PrimarySmall(tr("NUEVO", "NEW")) { dialog = ProjectDialog.Create }
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
                                Text(project.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                Text("${project.playlist.size} ${tr("PISTAS", "TRACKS")}   ·   ${project.playlist.sumOf { it.tracks.size }} STEMS", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
                            }
                            if (!compact) {
                                ConsoleReadout("OUTPUT", "${formatDb(project.masterGainDb).replace(" dB", "")}  ${panLabel(project.masterPan)}", Mint)
                                OutlinedButton(onClick = { vm.selectProject(project.id); vm.setWorkspace(Workspace.MASTER) }, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("MASTER", fontSize = 8.sp) }
                                Spacer(Modifier.width(6.dp))
                            }
                            Button(onClick = { vm.selectProject(project.id); vm.setWorkspace(Workspace.PLAYLIST) }, modifier = Modifier.height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Bg), shape = RoundedCornerShape(8.dp)) { Text(tr("ABRIR", "OPEN"), fontSize = 9.sp, fontWeight = FontWeight.Black) }
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
    val project = state.selectedProject()
    val selected = state.selectedMaster()
    if (project == null) {
        ConsolePanel(Modifier.fillMaxSize()) { EmptyState(tr("SIN PROYECTO", "NO PROJECT"), tr("Selecciona o crea un proyecto antes de armar la playlist.", "Select or create a project before building the setlist."), tr("IR A PROYECTOS", "GO TO PROJECTS")) { vm.setWorkspace(Workspace.PROJECTS) } }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Playlist", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("  ${project.playlist.size} cues", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            if (selected != null) {
                TextButton(onClick = { renameDialog = true }) { Text(tr("RENOMBRAR", "RENAME"), fontSize = 8.sp) }
                DawIconButton(
                    icon = DawIcon.DELETE,
                    label = tr("Quitar pista master", "Remove master track"),
                    danger = true,
                    onClick = { if (state.settings.confirmDestructiveActions) deleteDialog = true else vm.deleteSelectedMasterTrack() },
                )
                OutlinedButton(onClick = { vm.setWorkspace(Workspace.MASTER) }, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("MASTER", fontSize = 8.sp) }
                Spacer(Modifier.width(6.dp))
            }
            PrimarySmall(tr("PISTA", "TRACK")) { addDialog = true }
        }
        Surface(Modifier.fillMaxSize(), color = Panel, shape = RoundedCornerShape(10.dp)) {
            if (project.playlist.isEmpty()) {
                EmptyState(tr("Playlist vacía", "Empty setlist"), tr("Agrega la primera pista master del show.", "Add the first master track to the show."), tr("AGREGAR", "ADD"), onClick = { addDialog = true })
            } else LazyColumn(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(project.playlist, key = { _, item -> item.id }) { index, item ->
                    PlaylistRow(
                        index, item, project.defaultMetronome, item.id == state.selectedMasterTrackId,
                        select = { vm.selectMasterTrack(item.id) }, go = { vm.playMasterTrack(item.id) },
                        open = { vm.selectMasterTrack(item.id); vm.setWorkspace(Workspace.TRACK) },
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
    Surface(onClick = select, color = if (active) Color(0xFF1B2730) else Color(0xFF121920), shape = RoundedCornerShape(7.dp)) {
        BoxWithConstraints {
            val compact = maxWidth < 600.dp
            Row(Modifier.fillMaxWidth().height(if (compact) 68.dp else 58.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(44.dp).fillMaxHeight().background(if (active) Blue else Color(0xFF202A33)), contentAlignment = Alignment.Center) {
                Text((index + 1).toString().padStart(2, '0'), color = if (active) Color.White else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                Text("${item.tracks.size} STEMS   ${timeText(item.durationSeconds())}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
            }
            ConsoleReadout("TEMPO", "${metro.bpm.roundToInt()}  ${metro.numerator}/${metro.denominator}", if (metro.enabled) Amber else TextMuted)
            if (!compact) {
                ConsoleReadout("MASTER", "${formatDb(item.gainDb).replace(" dB", "")}  ${panLabel(item.pan)}", Mint)
                TinyIconButton(R.drawable.ic_ui_arrow_up, tr("Mover arriba", "Move up"), moveUp)
                TinyIconButton(R.drawable.ic_ui_arrow_down, tr("Mover abajo", "Move down"), moveDown)
                DawIconButton(DawIcon.TIMELINE, tr("Abrir pista", "Open track"), onClick = open)
                Spacer(Modifier.width(6.dp))
            }
            Button(onClick = go, modifier = Modifier.size(width = 62.dp, height = 42.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) Mint else Amber, contentColor = Bg), shape = RoundedCornerShape(8.dp)) { Text("GO", fontWeight = FontWeight.Black, fontSize = 11.sp) }
            Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun ConsoleReadout(label: String, value: String, color: Color) {
    Column(Modifier.width(92.dp)) {
        Text(label, color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TrackScreen(state: MainUiState, vm: MainViewModel, addAudio: () -> Unit) {
    var showStemOptions by remember { mutableStateOf(false) }
    var showEmptyStemDialog by remember { mutableStateOf(false) }
    val master = state.selectedMaster()
    if (master == null) {
        ConsolePanel(Modifier.fillMaxSize()) { EmptyState("SIN PISTA MASTER", "Selecciona una pista de la playlist antes de editar stems.", "ABRIR PLAYLIST") { vm.setWorkspace(Workspace.PLAYLIST) } }
        return
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(color = Panel, shape = RoundedCornerShape(8.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(master.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                DawIconButton(
                    icon = DawIcon.TIMELINE,
                    label = tr("Timeline", "Timeline"),
                    selected = state.trackWorkspace == TrackWorkspace.TIMELINE,
                ) { vm.setTrackWorkspace(TrackWorkspace.TIMELINE) }
                Spacer(Modifier.width(4.dp))
                DawIconButton(
                    icon = DawIcon.MIXER,
                    label = tr("Consola de mezcla", "Mix console"),
                    selected = state.trackWorkspace == TrackWorkspace.MIXER,
                ) { vm.setTrackWorkspace(TrackWorkspace.MIXER) }
            }
        }
        when (state.trackWorkspace) {
            TrackWorkspace.TIMELINE -> TimelineEditor(state, vm) { showStemOptions = true }
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
private fun TimelineEditor(state: MainUiState, vm: MainViewModel, addStem: () -> Unit) {
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
    var zoomIndex by remember { mutableIntStateOf(1) }
    val dpPerSecond = zoomLevels[zoomIndex]
    val density = LocalDensity.current
    val pxPerSecond = with(density) { dpPerSecond.dp.toPx() }
    val labelWidth = 188.dp
    val maxSeconds = maxOf(10.0, state.tracks.maxOf { it.startOffsetFrames.toDouble() / TIMELINE_SAMPLE_RATE + it.durationSeconds } + 1.0)
    var panSeconds by remember { mutableDoubleStateOf(0.0) }
    val grid = timelineGrid(dpPerSecond)
    val selected = state.tracks.firstOrNull { it.id == state.selectedTimelineTrackId }
    val canSplit = selected?.let { track ->
        val end = track.startOffsetFrames + (track.durationSeconds * TIMELINE_SAMPLE_RATE).roundToLong()
        state.timelineCursorFrames > track.startOffsetFrames && state.timelineCursorFrames < end
    } == true
    var localCursorFrames by remember { mutableLongStateOf(state.timelineCursorFrames) }
    LaunchedEffect(state.timelineCursorFrames) { localCursorFrames = state.timelineCursorFrames }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val editorWidth = maxWidth
        val viewportWidthPx = with(density) { (maxWidth - labelWidth).coerceAtLeast(120.dp).toPx() }
        val visibleSeconds = (viewportWidthPx / pxPerSecond).toDouble()
        val maxPanSeconds = (maxSeconds - visibleSeconds).coerceAtLeast(0.0)
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
            val candidates = buildList {
                add(0L)
                add(localCursorFrames)
                state.tracks.filterNot { it.id == trackId }.forEach { other ->
                    add(other.startOffsetFrames)
                    add(other.startOffsetFrames + (other.durationSeconds * TIMELINE_SAMPLE_RATE).roundToLong())
                }
            }
            val nearest = candidates.minByOrNull { abs(it - proposed) } ?: proposed
            return if (abs(nearest - proposed) <= tolerance) nearest else proposed
        }

        Surface(Modifier.fillMaxSize(), color = Panel, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).background(Raised).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DawIconButton(DawIcon.ADD, tr("Agregar stem", "Add stem"), selected = true, onClick = addStem)
                    if (editorWidth >= 720.dp) {
                        Spacer(Modifier.width(12.dp))
                        Text(selected?.name ?: tr("Selecciona un clip", "Select a clip"), Modifier.weight(1f),
                            color = if (selected == null) TextMuted else TextMain, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else Spacer(Modifier.weight(1f))
                    DawIconButton(DawIcon.UNDO, tr("Deshacer", "Undo"), enabled = state.canUndoTimeline, onClick = vm::undoTimelineEdit)
                    DawIconButton(DawIcon.REDO, tr("Rehacer", "Redo"), enabled = state.canRedoTimeline, onClick = vm::redoTimelineEdit)
                    DawIconButton(DawIcon.SPLIT, tr("Dividir stem en el cursor", "Split stem at playhead"), enabled = canSplit, onClick = vm::splitSelectedTrackAtCursor)
                    DawIconButton(
                        DawIcon.DELETE,
                        tr("Quitar stem seleccionado", "Remove selected stem"),
                        enabled = selected != null,
                        danger = true,
                        onClick = { selected?.let { vm.removeTrack(it.id) } },
                    )
                    Spacer(Modifier.width(4.dp))
                    DawIconButton(DawIcon.ZOOM_OUT, tr("Alejar timeline", "Zoom timeline out"), enabled = zoomIndex > 0) {
                        zoomIndex = (zoomIndex - 1).coerceAtLeast(0)
                    }
                    if (editorWidth >= 600.dp) {
                        Text(grid.scaleLabel, Modifier.width(58.dp), color = TextMuted, fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                    DawIconButton(DawIcon.ZOOM_IN, tr("Acercar timeline", "Zoom timeline in"), enabled = zoomIndex < zoomLevels.lastIndex) {
                        zoomIndex = (zoomIndex + 1).coerceAtMost(zoomLevels.lastIndex)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.height(52.dp).fillMaxWidth()) {
                            Box(Modifier.width(labelWidth).fillMaxHeight().background(Color(0xFF111315)).padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart) {
                                Column {
                                    Text(tr("CURSOR", "PLAYHEAD"), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text(timelineTimecode(localCursorFrames), color = Amber, fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TimelineRuler(
                                panSeconds, visibleSeconds, pxPerSecond, grid,
                                Modifier.weight(1f).fillMaxHeight().scrollable(horizontalPan, Orientation.Horizontal),
                            ) { seconds ->
                                val frame = (seconds * TIMELINE_SAMPLE_RATE).roundToLong().coerceAtLeast(0)
                                localCursorFrames = frame
                                vm.setTimelineCursor(frame)
                            }
                        }
                        LazyColumn(Modifier.weight(1f)) {
                            itemsIndexed(state.tracks, key = { _, it -> it.id }) { index, track ->
                                Row(Modifier.fillMaxWidth().height(68.dp)) {
                                    TimelineLaneHeader(track, index, track.id == state.selectedTimelineTrackId, labelWidth) {
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
                                        horizontalPan = horizontalPan,
                                        snapOffset = { snapOffset(track.id, it) },
                                        select = { vm.selectTimelineTrack(track.id) },
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
                        if (cursorX in labelWidthPx..size.width) {
                            drawLine(Amber, androidx.compose.ui.geometry.Offset(cursorX, 0f),
                                androidx.compose.ui.geometry.Offset(cursorX, size.height), 2.dp.toPx())
                        }
                    }
                    if (cursorX in labelWidthPx..with(density) { editorWidth.toPx() }) {
                        Box(
                            Modifier.offset { IntOffset((cursorX - with(density) { 20.dp.toPx() }).roundToInt(), 0) }
                                .size(width = 40.dp, height = 52.dp)
                                .pointerInput(pxPerSecond, maxSeconds) {
                                    detectDragGestures { change, amount ->
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
                drawLine(if (major) Silver else Border, androidx.compose.ui.geometry.Offset(x, if (major) 18.dp.toPx() else 27.dp.toPx()),
                    androidx.compose.ui.geometry.Offset(x, size.height), if (major) 1.5.dp.toPx() else 1.dp.toPx())
            }
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

private fun rulerLabel(seconds: Double, majorSeconds: Double): String = when {
    majorSeconds >= 1.0 -> "${seconds.roundToInt()}s"
    majorSeconds >= .5 -> "%.1fs".format(seconds)
    else -> "%.2fs".format(seconds)
}

@Composable
private fun TimelineLaneHeader(track: MixerTrackUi, index: Int, selected: Boolean, width: androidx.compose.ui.unit.Dp, select: () -> Unit) {
    Surface(onClick = select, modifier = Modifier.width(width).fillMaxHeight(),
        color = if (selected) Raised else if (index % 2 == 0) Panel else Color(0xFF191B1D)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(44.dp).background(Color(track.colorArgb), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(track.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${(index + 1).toString().padStart(2, '0')}  ·  IN ${timelineTimecode(track.startOffsetFrames)}",
                    color = if (selected) Color(track.colorArgb) else TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
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
    horizontalPan: androidx.compose.foundation.gestures.ScrollableState,
    snapOffset: (Long) -> Long,
    select: () -> Unit,
    commitOffset: (Long) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    var localStartFrames by remember(track.id, track.startOffsetFrames) { mutableLongStateOf(track.startOffsetFrames) }
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
                drawLine(if (tick % majorEvery == 0L) Border.copy(alpha = .7f) else Border.copy(alpha = .25f),
                    androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
            }
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
                    .pointerInput(track.id, pxPerSecond) {
                        detectDragGestures(
                            onDragStart = { select() },
                            onDragEnd = { commitOffset(localStartFrames) },
                            onDragCancel = { localStartFrames = track.startOffsetFrames },
                            onDrag = { change, amount ->
                                change.consume()
                                val raw = (localStartFrames.toDouble() + amount.x.toDouble() / pxPerSecond * TIMELINE_SAMPLE_RATE)
                                    .roundToLong().coerceAtLeast(0)
                                localStartFrames = snapOffset(raw)
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
                    Text(tr("Analizando onda…", "Analyzing waveform…"), Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
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
    Surface(Modifier.fillMaxSize(), color = Bg, shape = RoundedCornerShape(2.dp)) {
        LazyRow(Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            itemsIndexed(tracks, key = { _, it -> it.id }) { index, track ->
                ChannelStrip(index, track, { vm.setTrackGain(index, it) }, { vm.setTrackPan(index, it) }, { vm.toggleMute(index) }, { vm.toggleSolo(index) })
            }
        }
    }
}

@Composable
private fun ChannelStrip(index: Int, track: MixerTrackUi, gain: (Float) -> Unit, pan: (Float) -> Unit, mute: () -> Unit, solo: () -> Unit) {
    val stripColor = channelColor(index)
    Surface(
        Modifier.width(208.dp).fillMaxHeight(),
        color = Panel,
        shape = RoundedCornerShape(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(stripColor))
            Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(track.name, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text((index + 1).toString().padStart(2, '0'), color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
            }
            HorizontalDivider(color = Border)
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PeakMeter(track.peak, MeterGreen, Modifier.width(10.dp).fillMaxHeight())
                Spacer(Modifier.width(8.dp))
                VerticalFader(track.gainDb, gain, Silver, Modifier.weight(1f).fillMaxHeight())
            }
            Text(formatDb(track.gainDb), color = TextMain, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
                RotaryKnob(track.pan, -1f..1f, pan, Mint, Modifier.size(52.dp))
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
}

private fun channelColor(index: Int): Color = listOf(
    Color(0xFF728F86),
    Color(0xFF6E8098),
    Color(0xFF83758F),
    Color(0xFF9A825E),
)[index % 4]

@Composable
private fun VerticalFader(value: Float, change: (Float) -> Unit, color: Color, modifier: Modifier = Modifier) {
    val currentValue by rememberUpdatedState(value)
    Canvas(
        modifier.pointerInput(Unit) {
            var workingValue = currentValue
            detectDragGestures(
                onDragStart = { workingValue = currentValue },
                onDrag = { event, amount ->
                    event.consume()
                    val delta = if (size.height > 0) -amount.y / size.height * 66f else 0f
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
private fun MasterScreen(state: MainUiState, vm: MainViewModel) {
    var section by remember { mutableStateOf(MasterSection.PROJECT) }
    val project = state.selectedProject(); val master = state.selectedMaster()
    if (project == null) {
        ConsolePanel(Modifier.fillMaxSize()) { EmptyState("SIN PROYECTO", "El master pertenece a un proyecto.", "IR A PROYECTOS") { vm.setWorkspace(Workspace.PROJECTS) } }; return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(38.dp).background(Panel, RoundedCornerShape(8.dp)).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            MasterSection.entries.forEach { item ->
                Surface(onClick = { section = item }, color = if (section == item) Mint.copy(alpha = .14f) else Color.Transparent, shape = RoundedCornerShape(7.dp)) {
                    Row(Modifier.height(30.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        VectorIcon(item.iconRes, null, if (section == item) Mint else TextMuted, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(item.label, color = if (section == item) TextMain else TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                MasterSection.PROJECT -> ProjectMasterPanel(project, vm)
                MasterSection.TRACK -> MasterTrackPanel(master, vm)
                MasterSection.METRONOME -> MetronomePanel(project, master, vm)
                MasterSection.ROUTING -> RoutingPanel(state, vm)
            }
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
private fun ProjectMasterPanel(project: Project, vm: MainViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProjectOutputModule(project, vm, Modifier.fillMaxWidth().height(260.dp))
                DefaultMetronomeModule(project, vm, Modifier.fillMaxWidth().height(430.dp), compact = true)
            }
        } else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProjectOutputModule(project, vm, Modifier.width(330.dp).fillMaxHeight())
            DefaultMetronomeModule(project, vm, Modifier.weight(1f).fillMaxHeight(), compact = false)
        }
    }
}

@Composable
private fun ProjectOutputModule(project: Project, vm: MainViewModel, modifier: Modifier) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text("PROJECT OUTPUT", color = Mint, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                KnobControl("VOLUME", project.masterGainDb, -60f..6f, vm::setProjectGain, Mint, formatDb(project.masterGainDb), 92.dp)
                KnobControl("PAN", project.masterPan, -1f..1f, vm::setProjectPan, Amber, panLabel(project.masterPan), 76.dp)
            }
            Spacer(Modifier.weight(1f))
            Text("Afecta toda la playlist", color = TextMuted, fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DefaultMetronomeModule(project: Project, vm: MainViewModel, modifier: Modifier, compact: Boolean) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(8.dp)) {
        if (compact) Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text("METRONOME DEFAULT · NO GLOBAL", color = Amber, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text("${project.defaultMetronome.bpm.roundToInt()} BPM  ·  ${project.defaultMetronome.numerator}/${project.defaultMetronome.denominator}", color = Amber, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            MetronomeControls(project.defaultMetronome, true) { transform -> vm.updateDefaultMetronome(transform) }
        } else Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.width(180.dp)) {
                Text("METRONOME DEFAULT", color = Amber, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text("Plantilla", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("No reproduce globalmente. Cada pista hereda estos valores.", color = TextMuted, fontSize = 8.sp)
                Spacer(Modifier.weight(1f))
                Text("${project.defaultMetronome.bpm.roundToInt()} BPM  ·  ${project.defaultMetronome.numerator}/${project.defaultMetronome.denominator}", color = Amber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { MetronomeControls(project.defaultMetronome, true) { transform -> vm.updateDefaultMetronome(transform) } }
        }
    }
}

@Composable
private fun MasterTrackPanel(master: MasterTrack?, vm: MainViewModel) {
    if (master == null) { EmptyState("SIN PISTA SELECCIONADA", "Selecciona una pista de la playlist.", "ABRIR PLAYLIST") { vm.setWorkspace(Workspace.PLAYLIST) }; return }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        val output: @Composable (Modifier) -> Unit = { modifier ->
            Surface(modifier, color = Panel, shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("MASTER TRACK · ${master.name}", color = Blue, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        KnobControl("VOLUME", master.gainDb, -60f..6f, vm::setMasterGain, Mint, formatDb(master.gainDb), 92.dp)
                        KnobControl("PAN", master.pan, -1f..1f, vm::setMasterPan, Amber, panLabel(master.pan), 76.dp)
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        val summary: @Composable (Modifier) -> Unit = { modifier ->
            Surface(modifier, color = Panel, shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("SIGNAL SUMMARY", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    SummaryLine("Stems", master.tracks.size.toString()); SummaryLine("Duracion", timeText(master.durationSeconds()))
                    SummaryLine("Ultima entrada", timeText(master.tracks.maxOfOrNull { it.startOffsetFrames.toDouble() / TIMELINE_SAMPLE_RATE } ?: 0.0))
                    Spacer(Modifier.weight(1f))
                    DawIconButton(
                        DawIcon.MIXER,
                        tr("Abrir consola de mezcla", "Open mix console"),
                        selected = true,
                        modifier = Modifier.align(Alignment.End),
                    ) { vm.setWorkspace(Workspace.TRACK); vm.setTrackWorkspace(TrackWorkspace.MIXER) }
                }
            }
        }
        if (compact) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            output(Modifier.fillMaxWidth().height(260.dp)); summary(Modifier.fillMaxWidth().height(220.dp))
        } else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            output(Modifier.width(380.dp).fillMaxHeight()); summary(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun MetronomePanel(project: Project, master: MasterTrack?, vm: MainViewModel) {
    if (master == null) { EmptyState("SIN PISTA SELECCIONADA", "El metronomo siempre pertenece a una pista master.", "ABRIR PLAYLIST") { vm.setWorkspace(Workspace.PLAYLIST) }; return }
    val inherited = master.metronomeOverride == null
    val value = master.metronome(project.defaultMetronome)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        val inheritance: @Composable (Modifier) -> Unit = { modifier ->
            Surface(modifier, color = if (inherited) Blue.copy(alpha = .10f) else Panel, shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("${master.name} · CLICK", color = Amber, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(if (inherited) "INHERITED" else "CUSTOM", fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (inherited) "Usa la plantilla del proyecto" else "Ajuste exclusivo de esta pista", color = TextMuted, fontSize = 8.sp)
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("USE DEFAULT", Modifier.weight(1f), color = TextMuted, fontSize = 8.sp)
                        Switch(checked = inherited, onCheckedChange = vm::setMasterUsesDefault)
                    }
                }
            }
        }
        val controls: @Composable (Modifier) -> Unit = { modifier ->
            Surface(modifier, color = Panel, shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp).verticalScroll(rememberScrollState())) { MetronomeControls(value, !inherited) { transform -> vm.updateMasterMetronome(transform) } }
            }
        }
        if (compact) Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            inheritance(Modifier.fillMaxWidth().height(170.dp)); controls(Modifier.fillMaxWidth().height(430.dp))
        } else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            inheritance(Modifier.width(260.dp).fillMaxHeight()); controls(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun MetronomeControls(value: MetronomeSettings, enabled: Boolean, update: ((MetronomeSettings) -> MetronomeSettings) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("CLICK ACTIVO", fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Salida monitor protegida", color = TextMuted, fontSize = 9.sp) }
        Switch(value.enabled, { update { old -> old.copy(enabled = it) } }, enabled = enabled)
    }
    Spacer(Modifier.height(8.dp)); LabelValue("TEMPO", "${value.bpm.roundToInt()} BPM", Mint)
    Slider(value.bpm.toFloat(), { bpm -> update { it.copy(bpm = bpm.toDouble()) } }, valueRange = 40f..240f, enabled = enabled)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberStepper("PULSOS", value.numerator, enabled, Modifier.weight(1f)) { n -> update { it.copy(numerator = n) } }
        NumberStepper("FIGURA", value.denominator, enabled, Modifier.weight(1f), denominator = true) { d -> update { it.copy(denominator = d) } }
    }
    Spacer(Modifier.height(6.dp)); LabelValue("NIVEL CLICK", formatDb(value.gainDb), Amber)
    Slider(value.gainDb, { gain -> update { it.copy(gainDb = gain) } }, valueRange = -60f..0f, enabled = enabled)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("AUDICION EN MAIN", fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Apagado por seguridad", color = Red, fontSize = 9.sp) }
        Switch(value.mainEnabled, { main -> update { it.copy(mainEnabled = main) } }, enabled = enabled)
    }
}

@Composable
private fun RoutingPanel(state: MainUiState, vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RouteCard("SINGLE MIX", "MAIN L/R", "Mezcla estereo principal", !state.stereoSplit, { vm.setStereoSplit(false) }, Modifier.weight(1f))
            RouteCard("STEREO SPLIT", "L MAIN · R MON", "Click aislado en monitor", state.stereoSplit, { vm.setStereoSplit(true) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Surface(Modifier.fillMaxWidth().height(68.dp), color = Panel, shape = RoundedCornerShape(8.dp)) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Eyebrow("HARDWARE ACTUAL"); Text(state.devices.joinToString { it.name }.ifBlank { "Salida Android" }, fontWeight = FontWeight.Bold); Text("${state.diagnostics.actualSampleRate.takeIf { it > 0 } ?: 0} Hz · ${state.diagnostics.actualChannels} canales · XRuns ${state.diagnostics.xRuns}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp) }
                Button(onClick = vm::panic, colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White), shape = RoundedCornerShape(8.dp)) { Text("MUTE ALL", fontWeight = FontWeight.Black) }
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
                Column(
                    Modifier.fillMaxHeight().then(if (wide) Modifier.widthIn(max = 820.dp).align(Alignment.TopCenter) else Modifier.fillMaxWidth())
                        .verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 24.dp else 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (section) {
                        SettingsSection.GENERAL -> GeneralSettings(state, vm)
                        SettingsSection.STEMS -> StemSettings(state, vm)
                        SettingsSection.ABOUT -> AboutSettings()
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
        tr("Confirmar acciones destructivas", "Confirm destructive actions"),
        tr("Solicita confirmación antes de quitar proyectos o pistas.", "Asks before removing projects or playlist tracks."),
        state.settings.confirmDestructiveActions,
        vm::setConfirmDestructiveActions,
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
@Suppress("DEPRECATION")
private fun AboutSettings() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val installedVersion = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(84.dp), color = Raised, shape = RoundedCornerShape(2.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Image(painterResource(R.drawable.ic_brand_mark), contentDescription = "LiveTracks", modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text("LiveTracks", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("v$installedVersion", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text(tr("Consola multipista para vivo", "Multitrack live console"), color = Mint, fontSize = 11.sp)
        }
    }
    Button(
        onClick = { uriHandler.openUri("https://github.com/thomrnowtea/livetracks/releases/latest") },
        modifier = Modifier.fillMaxWidth().height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Bg),
        shape = RoundedCornerShape(2.dp),
    ) { Text(tr("VER ÚLTIMA RELEASE", "OPEN LATEST RELEASE"), fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); VectorIcon(R.drawable.ic_ui_external_link, null, Bg, Modifier.size(19.dp)) }
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
private fun CompactTransport(state: MainUiState, playPause: () -> Unit, stop: () -> Unit, seek: (Float) -> Unit, panic: () -> Unit) {
    val master = state.selectedMaster()
    val rate = state.diagnostics.actualSampleRate.takeIf { it > 0 } ?: 48_000
    val engineDuration = state.diagnostics.durationFrames.toDouble() / rate
    val total = maxOf(engineDuration, master?.durationSeconds() ?: 0.0)
    val position = if (state.diagnostics.durationFrames > 0) state.diagnostics.renderedFrames.toDouble() / rate else 0.0
    val fraction = if (total > 0) (position / total).toFloat().coerceIn(0f, 1f) else 0f
    Surface(color = Color(0xFF0C1116)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(54.dp)) {
            val compact = maxWidth < 600.dp
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = playPause, enabled = !state.openingOutput, modifier = Modifier.size(width = 62.dp, height = 44.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (state.diagnostics.toneEnabled) Amber else Mint, contentColor = Bg), shape = RoundedCornerShape(9.dp)) {
                VectorIcon(if (state.diagnostics.toneEnabled) R.drawable.ic_ui_pause else R.drawable.ic_ui_play, tr("Reproducir o pausar", "Play or pause"), Bg, Modifier.size(25.dp))
            }
            Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = stop, modifier = Modifier.size(44.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(9.dp)) { VectorIcon(R.drawable.ic_ui_stop, tr("Detener", "Stop"), TextMain, Modifier.size(21.dp)) }
            Spacer(Modifier.width(10.dp))
            if (!compact) Text(master?.name ?: "Sin pista", Modifier.width(120.dp), fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Slider(fraction, seek, enabled = state.diagnostics.durationFrames > 0, modifier = Modifier.weight(1f).height(30.dp))
            Text("${timeText(position)}/${timeText(total)}", Modifier.width(if (compact) 72.dp else 92.dp), color = Mint, fontFamily = FontFamily.Monospace, fontSize = if (compact) 8.sp else 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (!compact) Text("XR ${state.diagnostics.xRuns}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            Spacer(Modifier.width(if (compact) 2.dp else 8.dp))
            TextButton(onClick = panic, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = if (compact) 6.dp else 10.dp), colors = ButtonDefaults.textButtonColors(contentColor = Red)) { Text(if (compact) "!" else "PANIC", fontWeight = FontWeight.Black, fontSize = 8.sp) }
            }
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
@Composable private fun VectorIcon(iconRes: Int, description: String?, tint: Color, modifier: Modifier = Modifier) = Image(painterResource(iconRes), contentDescription = description, colorFilter = ColorFilter.tint(tint), modifier = modifier)
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
    TIMELINE(R.drawable.ic_ui_timeline),
    MIXER(R.drawable.ic_ui_mixer),
    SPLIT(R.drawable.ic_ui_split),
    DELETE(R.drawable.ic_ui_delete),
    UNDO(R.drawable.ic_ui_undo),
    REDO(R.drawable.ic_ui_redo),
    ZOOM_IN(R.drawable.ic_ui_zoom_in),
    ZOOM_OUT(R.drawable.ic_ui_zoom_out),
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
        modifier = modifier.size(48.dp).semantics {
            contentDescription = label
            role = Role.Button
        },
        color = when {
            selected -> Blue
            danger && enabled -> Red.copy(alpha = .08f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, if (selected) Blue else Border.copy(alpha = if (enabled) .9f else .35f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VectorIcon(icon.iconRes, null, ink, Modifier.size(26.dp))
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
        Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).then(if (active) Modifier.background(Mint, CircleShape) else Modifier.border(1.dp, TextMuted, CircleShape))); Spacer(Modifier.width(7.dp)); Text(if (active) "ACTIVO" else "DISPONIBLE", color = if (active) Mint else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp); Text(route, color = Amber, fontFamily = FontFamily.Monospace, fontSize = 11.sp); Text(detail, color = TextMuted, fontSize = 9.sp) }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, dismiss: () -> Unit, confirm: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, singleLine = true, label = { Text(tr("Nombre", "Name")) }) }, confirmButton = { TextButton(onClick = { confirm(value) }, enabled = value.isNotBlank()) { Text(tr("GUARDAR", "SAVE")) } }, dismissButton = { TextButton(onClick = dismiss) { Text(tr("CANCELAR", "CANCEL")) } })
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = createEmpty, modifier = Modifier.height(44.dp)) {
                    Text(tr("STEM VACÍO", "EMPTY STEM"), fontWeight = FontWeight.Bold)
                }
                Button(onClick = importAudio, modifier = Modifier.height(44.dp)) {
                    Text(tr("IMPORTAR AUDIO", "IMPORT AUDIO"), fontWeight = FontWeight.Bold)
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
                    singleLine = true,
                    label = { Text(tr("Nombre", "Name")) },
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
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
