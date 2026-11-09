package top.aniliberty.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.data.*

@Composable fun SearchScreen(vm: MainViewModel) {
    val state by vm.repo.search.collectAsStateWithLifecycle(); val releases by vm.repo.releases.collectAsStateWithLifecycle(); val details by vm.repo.details.collectAsStateWithLifecycle(); val library by vm.repo.library.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        PageTitle("Поиск", "Та самая история — в паре слов от тебя")
        OutlinedTextField(state.query, vm.repo::search, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Найти своё аниме…") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, trailingIcon = { IconButton(onClick = vm.repo::clearSearch) { Icon(Icons.Rounded.Close, "Очистить поиск") } }, singleLine = true, shape = RoundedCornerShape(15.dp))
        if (state.query.isBlank()) LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            if (library.searches.isNotEmpty()) { item { SectionHeading("Недавно искали") }; items(library.searches) { query -> AssistChip(onClick = { vm.repo.search(query) }, label = { Text(query) }, leadingIcon = { Icon(Icons.Rounded.History, null, Modifier.size(15.dp)) }, modifier = Modifier.padding(bottom = 6.dp)) } }
            item { SectionHeading("Попробуй найти"); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Фрирен", "Киберпанк", "Хоримия").forEach { query -> SuggestionChip(onClick = { vm.repo.search(query) }, label = { Text(query, fontSize = 11.sp) }) } } }
            item { SectionHeading("Сейчас выбирают") }; items(vm.repo.home.value.popular.take(6)) { id -> releases[id]?.let { SearchRow(it, Rules.releasedEpisodes(details[id]).size, { vm.repo.rememberSearch(); vm.openDetail(id) }, { vm.openEpisodes(id) }) } }
        } else LazyColumn(Modifier.padding(top = 14.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
            item { Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (state.remote) "Найдено: ${state.ids.size}" else "В сохранённом каталоге: ${state.ids.size}", color = TextSecondary, fontSize = 11.sp); if (state.loading) { Spacer(Modifier.width(10.dp)); CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 1.5.dp) } } }
            if (state.ids.any { releases[it]?.ageRating?.adult == true }) item { Surface(color = SurfaceDark, shape = RoundedCornerShape(11.dp), modifier = Modifier.padding(bottom = 10.dp)) { Text("По запросу показаны все совпадения, включая 18+.", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(11.dp)) } }
            items(state.ids, key = { it }) { id -> releases[id]?.let { SearchRow(it, Rules.releasedEpisodes(details[id]).size, { vm.repo.rememberSearch(); vm.openDetail(id) }, { vm.openEpisodes(id) }) } }
            if (!state.loading && state.ids.isEmpty()) item { EmptyState(Icons.Rounded.SearchOff, "Пока не нашли", "Попробуйте другое название или проверьте написание.") }
        }
    }
}
@Composable private fun SearchRow(release: Release, count: Int, open: () -> Unit, episodes: () -> Unit) { Surface(onClick = open, color = androidx.compose.ui.graphics.Color.Transparent, contentColor = TextPrimary, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { PosterArt(release, Modifier.size(78.dp, 109.dp).clip(RoundedCornerShape(10.dp))); Column(Modifier.weight(1f).padding(start = 13.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(release.name.main, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f, fill = false), maxLines = 2); if (release.ageRating.adult) SmallBadge("18+", Modifier.padding(start = 7.dp), MaterialTheme.colorScheme.primary) }; Text("${release.year ?: "—"} · ${release.type?.description ?: "TV"}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)); TextButton(onClick = episodes, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(29.dp)) { Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(13.dp)); Text(if (count > 0) "  Все $count серии" else "  Серии", fontSize = 10.sp) } }; Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = TextDim) } } }
