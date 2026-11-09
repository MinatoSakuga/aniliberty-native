package top.aniliberty.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.Screen
import top.aniliberty.app.data.*

@Composable fun HomeScreen(vm: MainViewModel) {
    val home by vm.repo.home.collectAsStateWithLifecycle(); val releases by vm.repo.releases.collectAsStateWithLifecycle(); val details by vm.repo.details.collectAsStateWithLifecycle(); val library by vm.repo.library.collectAsStateWithLifecycle(); val settings by vm.repo.settings.collectAsStateWithLifecycle()
    val history = library.history.values.filter { Rules.visible(releases[it.releaseId] ?: return@filter false, settings.hideAdult) }.sortedByDescending { it.updatedAt }.take(6)
    LaunchedEffect(Unit) { vm.repo.refreshHome() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
        item {
                    val pager = rememberPagerState(pageCount = { home.hero.size.coerceAtLeast(1) })
            Column { HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(372.dp).padding(top = 6.dp), pageSpacing = 12.dp) { page ->
                val release = releases[home.hero.getOrElse(page) { home.hero.firstOrNull() ?: 10085 }]
                if (release != null) HeroCard(release, library.collections["favorite"]?.contains(release.id) == true, { vm.openEpisodes(release.id) }, { vm.repo.toggleFavorite(release.id) })
            }; Row(Modifier.fillMaxWidth().padding(top = 11.dp), horizontalArrangement = Arrangement.Center) { repeat(home.hero.size) { i -> Box(Modifier.padding(horizontal = 3.dp).size(if (pager.currentPage == i) 19.dp else 5.dp, 5.dp).clip(CircleShape).background(if (pager.currentPage == i) MaterialTheme.colorScheme.primary else SurfaceHigh)) } } }
        }
        if (history.isNotEmpty()) { item { SectionHeading("Продолжить просмотр", "История") { vm.navigate(Screen.HISTORY) }; LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) { items(history, key = { it.key }) { entry -> releases[entry.releaseId]?.let { ContinueCard(entry, it) { vm.openPlayer(entry.releaseId, entry.ordinal) } } } } } }
        item { SectionHeading("Свежие серии", "Все новинки") { vm.navigate(Screen.CATALOG) } }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(home.latest.filter { releases[it]?.let { r -> Rules.visible(r, settings.hideAdult) } == true }, key = { it }) { id -> releases[id]?.let { ReleaseCard(it, true, Rules.releasedEpisodes(details[id]).size, library.collections["favorite"]?.contains(id) == true, { vm.openDetail(id) }, { vm.openEpisodes(id) }, { vm.repo.toggleFavorite(id) }, Modifier.width(162.dp), alignHomeActions = true) } } } }
        item { SectionHeading("Выбор сообщества", "Ещё") { vm.repo.loadCatalog(true, CatalogFilter(sort = "RATING_DESC")); vm.navigate(Screen.CATALOG) }; Text("Истории, которые хочется советовать друзьям.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 13.dp)) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(home.popular.filter { releases[it]?.let { r -> Rules.visible(r, settings.hideAdult) } == true }, key = { it }) { id -> releases[id]?.let { ReleaseCard(it, true, Rules.releasedEpisodes(details[id]).size, library.collections["favorite"]?.contains(id) == true, { vm.openDetail(id) }, { vm.openEpisodes(id) }, { vm.repo.toggleFavorite(id) }, Modifier.width(162.dp), alignHomeActions = true) } } } }
        item { AppFooter() }
    }
}
@Composable private fun HeroCard(release: Release, favorite: Boolean, watch: () -> Unit, save: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(25.dp)).background(SurfaceDark)) {
        Box(Modifier.fillMaxWidth().height(196.dp)) {
            val hero = if (release.id == 10085) bundledRes(context, "hero_frieren") else bundledRes(context, "e${release.id}_1")
            Artwork(if (release.id == 10085) null else imageUrl(release.poster), hero, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .32f), Color.Transparent, Color(0x660B1012)))))
            Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) { Surface(shape = RoundedCornerShape(7.dp), color = Color.Black.copy(alpha = .45f), contentColor = Color.White) { Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(5.dp)); Text("ВЫБОР РЕДАКЦИИ", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White) } } }
        }
        Column(Modifier.padding(horizontal = 21.dp)) {
            Text(release.name.english ?: "AniLiberty", color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
            Text(release.name.main, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Text("${release.year ?: ""} · ${release.genres.firstOrNull()?.name ?: "Аниме"}${release.episodesTotal?.let { " · $it серий" } ?: ""}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) { Button(onClick = watch, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)) { Icon(Icons.Rounded.PlayArrow, null, Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text("Смотреть") }; Spacer(Modifier.width(10.dp)); OutlinedIconButton(onClick = save, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(46.dp), border = BorderStroke(1.dp, if (favorite) MaterialTheme.colorScheme.primary else TextSecondary)) { Icon(if (favorite) Icons.Rounded.Check else Icons.Rounded.Add, "Избранное", tint = if (favorite) MaterialTheme.colorScheme.primary else TextSecondary) } }
        }
    }
}
