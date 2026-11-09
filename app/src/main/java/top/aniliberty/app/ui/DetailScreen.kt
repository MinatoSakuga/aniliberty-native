package top.aniliberty.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun DetailScreen(vm: MainViewModel, id: Int) {
    val releases by vm.repo.releases.collectAsStateWithLifecycle()
    val details by vm.repo.details.collectAsStateWithLifecycle()
    val library by vm.repo.library.collectAsStateWithLifecycle()
    val updates by vm.repo.store.episodeUpdates.collectAsStateWithLifecycle()
    val release = releases[id]
    val episodes = Rules.releasedEpisodes(details[id])
    val favorite = library.collections["favorite"]?.contains(id) == true
    if (release == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val kind = CollectionKind.entries.firstOrNull { it != CollectionKind.FAVORITE && library.collections[it.key]?.contains(id) == true }
    val voice = Rules.voiceActors(details[id] ?: release)
    val reminded = id in library.reminders
    val parts = Franchise.parts(releases.values.asSequence().plus(details.values).distinctBy { it.id }.toList(), release)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(id) { listState.scrollToItem(0) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        // ---- постер без названия, текст — под ним ----
        item {
            Box(Modifier.fillMaxWidth().height(252.dp)) {
                PosterArt(release, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent, Bg))))
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = vm::closeDetail, modifier = Modifier.background(Color.Black.copy(alpha = .35f), RoundedCornerShape(12.dp))) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Color.White) }
                    IconButton(onClick = { vm.repo.toggleFavorite(id) }, modifier = Modifier.background(Color.Black.copy(alpha = .35f), RoundedCornerShape(12.dp))) { Icon(if (favorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Избранное", tint = if (favorite) MaterialTheme.colorScheme.primary else Color.White) }
                }
            }
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text(release.name.english ?: "", color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(release.name.main, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 6.dp))
                Text("${release.year ?: "—"} · ${release.type?.value ?: release.type?.description ?: "TV"} · ${release.ageRating.label}${release.episodesTotal?.let { " · $it серий" } ?: ""}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.openEpisodes(id) }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp)); Text("Смотреть", fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 5.dp)) }
                    OutlinedButton(onClick = { vm.openCollection(id) }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(if (kind != null) Icons.Rounded.Check else Icons.Rounded.Add, null, Modifier.size(16.dp)); Text(kind?.title ?: "В коллекцию", fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 5.dp)) }
                    NotificationBell(reminded, updates.containsKey(id), { vm.repo.toggleReminder(id) }, Modifier.size(42.dp))
                }
                if (updates.containsKey(id)) Text("Вышла новая серия ${updates[id]?.ordinal?.toInt() ?: ""}. Уже можно смотреть.", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                OutlinedButton(onClick = { vm.openEpisodes(id) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(18.dp)); Text("  Все серии · ${episodes.size}", fontSize = 12.sp) }
            }
        }
        item {
            val description = release.description?.takeIf { it.isNotBlank() } ?: "Описание появится после загрузки данных."
            var expanded by remember(id) { mutableStateOf(false) }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 14.dp)) {
                Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 20.sp, maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis)
                if (description.length > 220) TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) { Text(if (expanded) "Свернуть" else "Читать полностью", fontSize = 11.sp) }
            }
        }
        if (voice.isNotEmpty()) item {
            Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 6.dp)) {
                Text("Озвучка", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    voice.forEach { name -> SuggestionChip(onClick = {}, label = { Text(name, fontSize = 11.sp) }) }
                }
            }
        }
        if (release.genres.isNotEmpty()) item {
            Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 2.dp)) {
                Text("Жанры", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    release.genres.forEach { genre -> SuggestionChip(onClick = { vm.repo.loadCatalog(true, CatalogFilter(genres = setOf(genre.id))); vm.closeDetail(); vm.navigate(top.aniliberty.app.Screen.CATALOG) }, label = { Text(genre.name, fontSize = 11.sp) }) }
                }
            }
        }
        // ---- части франшизы: сворачивающийся список, по умолчанию свёрнут ----
        if (parts.size > 1) item {
            var open by remember(id) { mutableStateOf(false) }
            Column(Modifier.padding(horizontal = 22.dp).padding(top = 4.dp)) {
                Surface(onClick = { open = !open }, shape = RoundedCornerShape(10.dp), color = SurfaceHigh, contentColor = TextPrimary, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Icon(Icons.Rounded.Layers, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("  Части · ${parts.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(if (open) "Свернуть" else "Показать", color = TextSecondary, fontSize = 10.sp)
                        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, Modifier.size(17.dp).padding(start = 4.dp), tint = TextSecondary)
                    }
                }
                if (open) parts.forEach { part ->
                    val current = part.id == id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { if (!current) vm.openDetail(part.id) }.padding(vertical = 6.dp)
                    ) {
                        PosterArt(part, Modifier.size(34.dp, 48.dp).clip(RoundedCornerShape(6.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(part.name.main, fontSize = 11.5.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (current) MaterialTheme.colorScheme.primary else TextPrimary)
                            Text(listOfNotNull(part.year?.toString(), if (current) "вы открыли" else null).joinToString(" · "), color = TextSecondary, fontSize = 9.5.sp)
                        }
                        if (current) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        else Icon(Icons.Rounded.ChevronRight, null, Modifier.size(15.dp), tint = TextDim)
                    }
                }
            }
        }
        item { Box(Modifier.padding(horizontal = 22.dp)) { SectionHeading("Серии", "Все ${episodes.size}") { vm.openEpisodes(id) } } }
        if (episodes.isEmpty()) item { Text("Список серий загружается…", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) }
        items(episodes.take(6), key = { it.id.ifBlank { it.ordinal.toString() } }) { episode ->
            Box(Modifier.padding(horizontal = 14.dp)) {
                EpisodeRow(id, episode, vm.repo.isWatched(id, episode.ordinal), vm.repo.entry(id, episode.ordinal), seen = vm.repo.isSeen(id, episode.ordinal)) { vm.openPlayer(id, episode.ordinal) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                InfoLine("Статус", if (release.ongoing) "Онгоинг" else "Завершён")
                InfoLine("Вышло серий", episodes.size.toString())
                InfoLine("Возрастной рейтинг", release.ageRating.label)
                release.shikimori?.rating?.let { InfoLine("Рейтинг Shikimori", "%.2f".format(it)) }
            }
        }
    }
}
@Composable private fun InfoLine(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) { Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f)); Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) } }
