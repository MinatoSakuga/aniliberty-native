package top.aniliberty.app.ui

import android.net.Uri
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.Screen
import top.aniliberty.app.data.*
import java.util.Locale

private val posterTints = listOf(0xFFFE3635, 0xFFF2B544, 0xFF5BB8F5, 0xFF9D8CFF, 0xFFB8BDD0, 0xFFE0A93B, 0xFFC9CCD6, 0xFF9D8CFF, 0xFFF27BA0, 0xFF5FD4A8)

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val library by vm.repo.library.collectAsStateWithLifecycle()
    val auth by vm.repo.auth.collectAsStateWithLifecycle()
    val local by vm.repo.store.localProfile.collectAsStateWithLifecycle()
    val releases by vm.repo.releases.collectAsStateWithLifecycle()
    val settings by vm.repo.store.settings.collectAsStateWithLifecycle()
    val profile = auth.profile
    val score = Rules.profileScore(library)
    val level = Rules.profileLevel(score)
    val progress = Rules.levelProgress(score)
    val toNext = ProfileRules.scoreToNext(score)
    val stats = remember(library, local) {
        ProfileRules.stats(library, local.avatarPath != null || local.coverPath != null, settings.installedAt)
    }
    // Плитки не обнуляются после сброса/очистки: копим максимумы и добавляем коллекции.
    val titles = maxOf(stats.uniqueTitles, library.collections.values.flatten().toSet().size, settings.statTitles)
    val opened = maxOf(stats.opened, settings.statOpened)
    val minutes = maxOf((stats.hours * 60).toLong(), settings.statMinutes)
    LaunchedEffect(stats) {
        if (stats.uniqueTitles > settings.statTitles || stats.opened > settings.statOpened || (stats.hours * 60).toLong() > settings.statMinutes) {
            vm.repo.store.updateSettings {
                it.copy(
                    statTitles = maxOf(it.statTitles, stats.uniqueTitles),
                    statOpened = maxOf(it.statOpened, stats.opened),
                    statMinutes = maxOf(it.statMinutes, (stats.hours * 60).toLong())
                )
            }
        }
    }
    val displayName = local.name?.takeIf { it.isNotBlank() } ?: profile?.displayName ?: "Гостевой профиль"
    val history = library.history.values.sortedByDescending { it.updatedAt }
    var confirmLogout by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        // ---- cover + top actions ----
        item {
            Box(Modifier.fillMaxWidth().height(178.dp)) {
                if (local.coverPath != null) Artwork(Uri.fromFile(File(local.coverPath!!)).toString(), 0, Modifier.fillMaxSize())
                else CoverArt(local.coverStyle.coerceIn(0, 4), Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Bg))))
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TopIcon(Icons.Rounded.Edit, "Редактировать профиль") { editProfile = true }
                    Spacer(Modifier.width(8.dp))
                    TopIcon(Icons.Rounded.Settings, "Настройки") { vm.navigate(Screen.SETTINGS) }
                    if (profile != null) {
                        Spacer(Modifier.width(8.dp))
                        TopIcon(Icons.Rounded.ExitToApp, "Выйти") { confirmLogout = true }
                    }
                }
            }
        }
        // ---- identity ----
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-42).dp), verticalAlignment = Alignment.Bottom) {
                AvatarStack(
                    avatarUrl = local.avatarPath?.let { Uri.fromFile(File(it)).toString() } ?: imageUrl(profile?.avatar),
                    xpProgress = progress,
                    levelLabel = "$level ур.",
                    size = 132.dp
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).padding(bottom = 10.dp)) {
                    Text(displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        profile?.let { "@${it.login}" } ?: "Данные хранятся на устройстве",
                        color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                    if (local.about.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = GlassScrim, contentColor = TextSecondary, border = BorderStroke(1.dp, GlassBorder)) {
                            Text(local.about, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
        if (profile == null) {
            item {
                Button(onClick = vm::openLogin, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-26).dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Войти в AniLiberty")
                }
            }
        }
        // ---- level & score ----
        item {
            GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-18).dp), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Star, null, Modifier.size(20.dp)) }
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Уровень", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(ProfileRules.levelTitle(level), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$score", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("очков активности", color = TextDim, fontSize = 9.sp, letterSpacing = .5.sp)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(6.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary, trackColor = SurfaceHigh
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("До ${level + 1} уровня", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text("ещё $toNext очков", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // ---- stats ----
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(Modifier.weight(1f), "$titles", "Тайтлов", Icons.Rounded.VideoLibrary)
                MiniStat(Modifier.weight(1f), "$opened", "Серий", Icons.Rounded.PlayCircle)
                MiniStat(Modifier.weight(1f), "${(minutes / 60).toInt()}", "Часов", Icons.Rounded.Schedule)
                MiniStat(Modifier.weight(1f), "${stats.favorites}", "Избранное", Icons.Rounded.Bookmark)
            }
        }
        // ---- collections ----
        item {
            Text("Мои коллекции", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(CollectionKind.entries.filter { it != CollectionKind.FAVORITE }) { kind ->
                    val ids = library.collections[kind.key].orEmpty().take(3)
                    CollectionCard(kind, library.collections[kind.key]?.size ?: 0, ids, releases) {
                        vm.navigate(Screen.LIBRARY); vm.selectLibraryTab(kind.key)
                    }
                }
            }
        }
        // ---- history ----
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("История", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.navigate(Screen.HISTORY) }) { Text("Вся", fontSize = 11.sp) }
            }
            if (history.isEmpty()) {
                Text("Начните смотреть — прогресс и статистика появятся здесь.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
        }
        items(history.take(6), key = { it.key }) { entry ->
            val release = releases[entry.releaseId]
            Surface(onClick = { vm.openPlayer(entry.releaseId, entry.ordinal) }, color = Color.Transparent, contentColor = TextPrimary, modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    EpisodeArt(entry.releaseId, Episode(ordinal = entry.ordinal), Modifier.size(84.dp, 56.dp).clip(RoundedCornerShape(8.dp)))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(release?.name?.main ?: "Релиз #${entry.releaseId}", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Серия ${entry.ordinal.toInt()} · ${formatTime(entry.time)} / ${formatTime(entry.duration)}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                        LinearProgressIndicator(progress = { entry.progress }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(3.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = SurfaceHigh)
                    }
                    Icon(if (Rules.watched(entry)) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                }
            }
        }
    }
    if (editProfile) ProfileEditSheet(vm) { editProfile = false }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        containerColor = SurfaceDark, titleContentColor = TextPrimary, textContentColor = TextSecondary,
        title = { Text("Выйти из аккаунта?") },
        text = { Text("Данные останутся в AniLiberty. Вернётся гостевой профиль.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; vm.repo.logout() }) { Text("Выйти") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Отмена") } }
    )
}

@Composable
private fun TopIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = .38f), contentColor = Color.White, border = BorderStroke(1.dp, Color.White.copy(alpha = .16f))) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(17.dp)) }
    }
}

@Composable
private fun MiniStat(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    GlassPanel(modifier.height(76.dp), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxSize().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.padding(top = 5.dp))
            Text(label, color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp, maxLines = 1)
        }
    }
}


@Composable
private fun CollectionCard(kind: CollectionKind, count: Int, sample: List<Int>, releases: Map<Int, Release>, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = GlassScrim, contentColor = TextPrimary, border = BorderStroke(1.dp, GlassBorder)) {
        Column(Modifier.width(160.dp).padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                sample.reversed().forEachIndexed { index, id ->
                    val order = sample.size - 1 - index
                    val art = releases[id]?.let { imageUrl(it.poster) }
                    Box(
                        Modifier.padding(start = (order * 30).dp).size(width = 44.dp, height = 62.dp)
                            .offset(y = (order * 1.5).dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(posterTints[(id % posterTints.size).coerceIn(0, posterTints.size - 1)]))
                    ) {
                        if (art != null) Artwork(art, 0, Modifier.fillMaxSize(), ContentScale.Crop)
                    }
                }
            }
            Text(kind.title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 9.dp))
            Text("$count тайтлов", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    GlassPanel(modifier.height(86.dp), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 11.dp)) {
                Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(label, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val library by vm.repo.library.collectAsStateWithLifecycle()
    val releases by vm.repo.releases.collectAsStateWithLifecycle()
    val auth by vm.repo.auth.collectAsStateWithLifecycle()
    val entries = library.history.values.sortedByDescending { it.updatedAt }
    Column(Modifier.fillMaxSize()) {
        NativeBack("История просмотров", vm::back) { if (auth.profile == null && entries.isNotEmpty()) IconButton(onClick = vm::clearHistory) { Icon(Icons.Rounded.DeleteOutline, "Очистить историю", tint = TextSecondary) } }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp)) {
            items(entries, key = { it.key }) { entry ->
                val release = releases[entry.releaseId]
                Surface(onClick = { vm.openPlayer(entry.releaseId, entry.ordinal) }, color = Color.Transparent, contentColor = TextPrimary) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        EpisodeArt(entry.releaseId, Episode(ordinal = entry.ordinal), Modifier.size(88.dp, 58.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(release?.name?.main ?: "Релиз #${entry.releaseId}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Серия ${entry.ordinal.toInt()} · ${formatTime(entry.time)} / ${formatTime(entry.duration)}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                        Icon(if (Rules.watched(entry)) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (entries.isEmpty()) item { EmptyState(Icons.Rounded.History, "История пока пуста", "Начните смотреть — прогресс сохранится здесь.") }
        }
    }
}
private fun MainViewModel.clearHistory() { repo.clearGuestData(true) }
