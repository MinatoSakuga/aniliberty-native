package top.aniliberty.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.data.*

@Composable fun CatalogScreen(vm: MainViewModel) {
    val state by vm.repo.catalog.collectAsStateWithLifecycle(); val releases by vm.repo.releases.collectAsStateWithLifecycle(); val details by vm.repo.details.collectAsStateWithLifecycle(); val library by vm.repo.library.collectAsStateWithLifecycle(); val settings by vm.repo.settings.collectAsStateWithLifecycle(); val home by vm.repo.home.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, state.ids.size) { snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last -> if (last >= state.ids.size - 6 && !state.loading && state.page < state.pages) vm.repo.loadCatalog(false) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageTitle("Каталог", "Истории на любой настрой") { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { vm.navigate(top.aniliberty.app.Screen.SEARCH) }) { Icon(Icons.Rounded.Search, "Поиск", Modifier.size(22.dp)) }; ViewToggle(settings.catalogGrid) { value -> vm.repo.store.updateSettings { it.copy(catalogGrid = value) } }; Spacer(Modifier.width(2.dp)); IconButton(vm::showFilters) { Icon(Icons.Rounded.Tune, "Фильтры") } } }
        LazyRow(Modifier.padding(top = 13.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { item { FilterChip(selected = state.filter.genres.isEmpty(), onClick = { vm.repo.loadCatalog(true, state.filter.copy(genres = emptySet())) }, label = { Text("Все") }) }; items(home.genres.take(12)) { genre -> FilterChip(selected = genre.id in state.filter.genres, onClick = { vm.repo.loadCatalog(true, state.filter.copy(genres = if (genre.id in state.filter.genres) state.filter.genres - genre.id else state.filter.genres + genre.id)) }, label = { Text(genre.name, fontSize = 11.sp) }) } }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (state.offline) "${state.total} в сохранённом каталоге" else "${state.total} историй", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f)); var sortMenu by remember { mutableStateOf(false) }; TextButton(onClick = { sortMenu = true }, contentPadding = PaddingValues(4.dp)) { Text(when (state.filter.sort) { "YEAR_DESC" -> "Сначала новые"; "RATING_DESC" -> "Популярные"; else -> "Обновления" }, fontSize = 10.sp); Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(16.dp)) }; DropdownMenu(sortMenu, { sortMenu = false }) { listOf("FRESH_AT_DESC" to "Обновления", "RATING_DESC" to "Популярные", "YEAR_DESC" to "Сначала новые").forEach { (key, title) -> DropdownMenuItem(text = { Text(title) }, onClick = { sortMenu = false; vm.repo.loadCatalog(true, state.filter.copy(sort = key)) }) } } }
        if (state.loading && state.ids.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } else LazyVerticalGrid(columns = GridCells.Fixed(if (settings.catalogGrid) 2 else 1), state = gridState, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(if (settings.catalogGrid) 12.dp else 3.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
            items(state.ids, key = { it }) { id -> releases[id]?.let { ReleaseCard(it, settings.catalogGrid, Rules.releasedEpisodes(details[id]).size, library.collections["favorite"]?.contains(id) == true, { vm.openDetail(id) }, { vm.openEpisodes(id) }, { vm.repo.toggleFavorite(id) }) } }
            if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(25.dp), strokeWidth = 2.dp) } }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun FilterSheet(vm: MainViewModel) {
    val state by vm.repo.catalog.collectAsStateWithLifecycle(); val genres by vm.repo.home.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(state.filter) }
    ModalBottomSheet(onDismissRequest = vm::dismissFilters, containerColor = SurfaceDark, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) {
            Text("Твой идеальный вечер", style = MaterialTheme.typography.titleLarge); Text("Настроим каталог под тебя", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp, bottom = 18.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { genres.genres.forEach { genre -> FilterChip(selected = genre.id in filter.genres, onClick = { filter = filter.copy(genres = if (genre.id in filter.genres) filter.genres - genre.id else filter.genres + genre.id) }, label = { Text(genre.name) }) } }
            Row(Modifier.padding(top = 19.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(filter.from, { filter = filter.copy(from = it.filter(Char::isDigit).take(4)) }, label = { Text("Год от") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(filter.to, { filter = filter.copy(to = it.filter(Char::isDigit).take(4)) }, label = { Text("Год до") }, modifier = Modifier.weight(1f), singleLine = true) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text("Только онгоинги", modifier = Modifier.weight(1f)); Switch(filter.ongoing, { filter = filter.copy(ongoing = it) }) }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { filter = CatalogFilter() }, modifier = Modifier.weight(1f)) { Text("Сбросить") }; Button(onClick = { vm.applyFilter(filter) }, modifier = Modifier.weight(1.4f)) { Text("Показать") } }
        }
    }
}
