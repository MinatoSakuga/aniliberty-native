package top.aniliberty.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.Screen
import top.aniliberty.app.data.*

/**
 * «Избранное» из нижней панели: первая вкладка всегда «Избранное»,
 * дальше — коллекции. Порядок коллекций меняется долгим тапом и перетаскиванием.
 */
@Composable
fun LibraryScreen(vm: MainViewModel) {
    val library by vm.repo.library.collectAsStateWithLifecycle()
    val settings by vm.repo.settings.collectAsStateWithLifecycle()
    val sync by vm.repo.sync.collectAsStateWithLifecycle()
    val releases by vm.repo.releases.collectAsStateWithLifecycle()
    val details by vm.repo.details.collectAsStateWithLifecycle()
    val selected by vm.libraryTab.collectAsStateWithLifecycle()
    val favoriteSelected = selected == "favorite"
    val rawIds = library.collections[selected].orEmpty()
    val orderedIds = if (favoriteSelected) Rules.displayFavorites(rawIds, settings.favoritesNewestFirst) else rawIds
    val ids = orderedIds.filter { releases[it]?.let { r -> Rules.visible(r, settings.hideAdult) } == true }
    val activeKind = CollectionKind.entries.firstOrNull { it.key == selected && it != CollectionKind.FAVORITE }
    val favoriteCount = library.collections["favorite"]?.size ?: 0

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageTitle("Избранное", "Закладки и коллекции")

        LibraryTabs(
            order = settings.tabOrder,
            collections = library.collections,
            selected = selected,
            favoriteCount = favoriteCount,
            favoritesNewestFirst = settings.favoritesNewestFirst,
            onSelect = vm::selectLibraryTab,
            onOrder = { order -> vm.repo.store.updateSettings { it.copy(tabOrder = order) } }
        )

        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (favoriteSelected) "Избранное" else activeKind?.title ?: "Коллекции",
                color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
            )
            if (favoriteSelected) TextButton(onClick = vm::toggleFavoriteDirection, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(if (settings.favoritesNewestFirst) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                Text(if (settings.favoritesNewestFirst) " Сначала новые" else " Сначала старые", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
            }
            ViewToggle(settings.libraryGrid) { grid -> vm.repo.store.updateSettings { it.copy(libraryGrid = grid) } }
        }
        Spacer(Modifier.height(10.dp))

        if (ids.isEmpty()) Box(Modifier.weight(1f)) {
            EmptyState(
                if (favoriteSelected) Icons.Rounded.Bookmarks else Icons.Rounded.VideoLibrary,
                if (sync.running) "Загружаем…" else if (favoriteSelected) "В избранном пока пусто" else "В этой коллекции пока пусто",
                "Добавляйте истории из каталога — они появятся здесь.",
                "Открыть каталог"
            ) { vm.navigate(Screen.CATALOG) }
        } else LazyVerticalGrid(
            columns = GridCells.Fixed(if (settings.libraryGrid) 2 else 1),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(if (settings.libraryGrid) 12.dp else 3.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // For favorites we intentionally key by position, not release id: reversing the order
            // must swap the content under the current viewport instead of scrolling to the moved key.
            items(ids, key = if (favoriteSelected) null else { id -> id }) { id ->
                releases[id]?.let {
                    ReleaseCard(it, settings.libraryGrid, Rules.releasedEpisodes(details[id]).size, library.collections["favorite"]?.contains(id) == true, { vm.openDetail(id) }, { vm.openEpisodes(id) }, { vm.repo.toggleFavorite(id) })
                }
            }
        }
    }
}

@Composable
private fun LibraryTabs(
    order: List<String>,
    collections: Map<String, List<Int>>,
    selected: String,
    favoriteCount: Int,
    favoritesNewestFirst: Boolean,
    onSelect: (String) -> Unit,
    onOrder: (List<String>) -> Unit
) {
    val keys = Rules.collectionTabOrder(order)
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val currentOrder by rememberUpdatedState(keys)
    val haptic = LocalHapticFeedback.current
    val favoriteSelected = selected == "favorite"

    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState(), enabled = dragging == null), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        // «Избранное» закреплено первым и не участвует в перетаскивании.
        Surface(
            onClick = { if (dragging == null) onSelect("favorite") },
            shape = RoundedCornerShape(12.dp),
            color = if (favoriteSelected) MaterialTheme.colorScheme.primary.copy(alpha = .20f) else GlassFill,
            contentColor = if (favoriteSelected) MaterialTheme.colorScheme.primary else TextPrimary,
            border = BorderStroke(1.dp, if (favoriteSelected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else GlassBorder),
            modifier = Modifier.height(40.dp).semantics {
                contentDescription = if (favoriteSelected) "Избранное, $favoriteCount, нажмите ещё раз чтобы поменять порядок" else "Избранное, $favoriteCount"
            }
        ) {
            Row(Modifier.padding(horizontal = 12.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bookmark, null, Modifier.size(14.dp), tint = if (favoriteSelected) MaterialTheme.colorScheme.primary else TextSecondary)
                Text(" Избранное", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (favoriteSelected) MaterialTheme.colorScheme.primary else TextPrimary, maxLines = 1)
                Text(" $favoriteCount", fontSize = 9.sp, color = TextSecondary)
                if (favoriteSelected) Icon(if (favoritesNewestFirst) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null, Modifier.size(12.dp).padding(start = 2.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        keys.forEach { key ->
            val kind = CollectionKind.entries.first { it.key == key }
            val isSelected = selected == key
            val scale by animateFloatAsState(if (dragging == key) 1.09f else 1f, label = "tab-scale")
            Surface(
                onClick = { if (dragging == null) onSelect(key) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else GlassFill,
                contentColor = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary,
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .42f) else GlassBorder),
                modifier = Modifier.width(112.dp).height(40.dp).zIndex(if (dragging == key) 2f else 0f).graphicsLayer { scaleX = scale; scaleY = scale }.pointerInput(key) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragging = key; dragX = 0f; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDragEnd = { dragging = null; dragX = 0f },
                        onDragCancel = { dragging = null; dragX = 0f },
                        onDrag = { change, amount ->
                            change.consume(); dragX += amount.x
                            if (kotlin.math.abs(dragX) > 85f) {
                                val list = currentOrder.toMutableList(); val from = list.indexOf(key); val to = (from + if (dragX > 0) 1 else -1).coerceIn(0, list.lastIndex)
                                if (to != from) { list.removeAt(from); list.add(to, key); onOrder(list); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                                dragX = 0f
                            }
                        }
                    )
                }
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(kind.title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(" ${collections[key]?.size ?: 0}", fontSize = 9.sp, color = TextSecondary)
                }
            }
        }
    }
}
