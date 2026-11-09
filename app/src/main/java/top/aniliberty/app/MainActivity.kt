package top.aniliberty.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.MutableStateFlow
import top.aniliberty.app.player.PlayerScreen
import top.aniliberty.app.ui.*
import java.io.File

class MainActivity : ComponentActivity() {
    private val incoming = MutableStateFlow<Int?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge(); incoming.value = intent?.getIntExtra("release_id", -1)?.takeIf { it > 0 }
        setContent {
            val vm: MainViewModel = viewModel(); val settings by vm.repo.settings.collectAsStateWithLifecycle(); val player by vm.player.collectAsStateWithLifecycle()
            DisposableEffect(player != null) {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                if (player != null) { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; controller.hide(WindowInsetsCompat.Type.systemBars()); controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
                else { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; controller.show(WindowInsetsCompat.Type.systemBars()) }
                onDispose { requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; controller.show(WindowInsetsCompat.Type.systemBars()) }
            }
            AniTheme(settings.accent, settings.themeMode) { AppRoot(vm, incoming) }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); incoming.value = intent.getIntExtra("release_id", -1).takeIf { it > 0 } }
}

@Composable
private fun AppRoot(vm: MainViewModel, incoming: MutableStateFlow<Int?>) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val detail by vm.detailId.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val loading by vm.playerLoading.collectAsStateWithLifecycle()
    val login by vm.loginVisible.collectAsStateWithLifecycle()
    val episodes by vm.episodeSheet.collectAsStateWithLifecycle()
    val collection by vm.collectionMenu.collectAsStateWithLifecycle()
    val filters by vm.filterVisible.collectAsStateWithLifecycle()
    val auth by vm.repo.auth.collectAsStateWithLifecycle()
    val localProfile by vm.repo.store.localProfile.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val hazeState = remember { HazeState() }
    val showBottomBar = detail == null && player == null

    LaunchedEffect(Unit) { vm.repo.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { incoming.collect { id -> if (id != null) { vm.openReleaseExtra(id); incoming.value = null } } }
    BackHandler(enabled = player != null || vm.canBack) { if (player != null) vm.closePlayer() else vm.back() }

    Box(Modifier.fillMaxSize().background(Bg)) {
        // The scrollable page is the blur source and deliberately continues underneath the floating bar.
        Box(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .haze(state = hazeState, style = HazeDefaults.style(backgroundColor = Bg, tint = Color.Transparent, blurRadius = 26.dp, noiseFactor = .05f))
        ) {
            if (detail != null) DetailScreen(vm, detail!!) else when (screen) {
                Screen.HOME -> Column(Modifier.fillMaxSize()) { AppHeader(auth.profile, localProfile.avatarPath, { vm.navigate(Screen.SEARCH) }, { vm.navigate(Screen.PROFILE) }); HomeScreen(vm) }
                Screen.CATALOG -> CatalogScreen(vm)
                Screen.LIBRARY -> LibraryScreen(vm)
                Screen.SCHEDULE -> ScheduleScreen(vm)
                Screen.PROFILE -> ProfileScreen(vm)
                Screen.SEARCH -> SearchScreen(vm)
                Screen.SETTINGS -> SettingsScreen(vm)
                Screen.HISTORY -> HistoryScreen(vm)
            }
        }

        if (showBottomBar) NativeBottomBar(screen, hazeState, Modifier.align(Alignment.BottomCenter)) { vm.navigate(it) }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (showBottomBar) 120.dp else 24.dp)) { data -> Snackbar(snackbarData = data, containerColor = SurfaceHigh, contentColor = TextPrimary, actionColor = MaterialTheme.colorScheme.primary) }

        if (login) LoginSheet(vm)
        episodes?.let { EpisodesSheet(vm, it) }
        collection?.let { CollectionSheet(vm, it) }
        if (filters) FilterSheet(vm)
        if (loading) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
            GlassPanel(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text("Открываем плеер…", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp)) }
            }
        }
        player?.let { PlayerScreen(vm, it) }
    }
}

@Composable
private fun AppHeader(account: top.aniliberty.app.data.Profile?, localAvatarPath: String?, search: () -> Unit, profile: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Brand()
        Text("AniLiberty", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(start = 9.dp))
        IconButton(search) { Icon(Icons.Rounded.Search, "Поиск", Modifier.size(22.dp)) }
        Surface(onClick = profile, shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .16f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f)), modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) {
                val avatar = localAvatarPath?.let { Uri.fromFile(File(it)).toString() } ?: top.aniliberty.app.data.imageUrl(account?.avatar)
                if (avatar != null) Artwork(avatar, 0, Modifier.fillMaxSize())
                else Icon(Icons.Rounded.Person, "Профиль", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NativeBottomBar(screen: Screen, hazeState: HazeState, modifier: Modifier = Modifier, navigate: (Screen) -> Unit) {
    val items = listOf(
        Triple(Screen.HOME, "Главная", Icons.Rounded.Home),
        Triple(Screen.CATALOG, "Каталог", Icons.Rounded.VideoLibrary),
        Triple(Screen.LIBRARY, "Избранное", Icons.Rounded.Bookmark),
        Triple(Screen.SCHEDULE, "Расписание", Icons.Rounded.CalendarMonth),
        Triple(Screen.PROFILE, "Профиль", Icons.Rounded.Person)
    )
    // Панель на всю ширину экрана, плоская и прозрачная: размытие сохранено, светящейся кромки-выпуклости нет.
    Column(
        modifier.fillMaxWidth()
            .hazeChild(state = hazeState, shape = RectangleShape, style = HazeStyle(tint = LocalAppPalette.current.navTint, blurRadius = 30.dp, noiseFactor = .06f))
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { (target, label, icon) ->
                val selected = screen == target || (target == Screen.PROFILE && screen in listOf(Screen.SETTINGS, Screen.HISTORY))
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { navigate(target) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.width(46.dp).height(28.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .22f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, label, Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.primary else TextSecondary) }
                    Text(label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        // Место под системную навигацию — фон панели продолжается до самого низа экрана.
        Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
