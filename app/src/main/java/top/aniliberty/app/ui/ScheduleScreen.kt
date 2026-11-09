package top.aniliberty.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun ScheduleScreen(vm: MainViewModel) {
    val home by vm.repo.home.collectAsStateWithLifecycle()
    val selected by vm.scheduleDay.collectAsStateWithLifecycle()
    val library by vm.repo.library.collectAsStateWithLifecycle()
    val settings by vm.repo.settings.collectAsStateWithLifecycle()
    val details by vm.repo.details.collectAsStateWithLifecycle()
    val releases by vm.repo.releases.collectAsStateWithLifecycle()
    val updates by vm.repo.store.episodeUpdates.collectAsStateWithLifecycle()
    val monday = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
    val names = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val items = home.schedule.filter { it.release.publishDay?.value == selected + 1 && Rules.visible(releases[it.release.id] ?: it.release, settings.hideAdult) }
    val grid = settings.scheduleGrid
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.background(Bg).padding(horizontal = 16.dp)) {
            PageTitle("Расписание", "Новые серии. Знакомые вселенные.", action = {
                ViewToggle(grid) { vm.toggleScheduleView() }
            })
            Row(Modifier.fillMaxWidth().padding(bottom = 15.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                names.forEachIndexed { index, name ->
                    val date = monday.plusDays(index.toLong())
                    Surface(onClick = { vm.selectDay(index) }, shape = RoundedCornerShape(14.dp), color = if (selected == index) MaterialTheme.colorScheme.primary else SurfaceDark, border = if (date == LocalDate.now() && selected != index) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .5f)) else null, modifier = Modifier.weight(1f).height(66.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(name, fontSize = 9.sp, color = if (selected == index) MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f) else TextSecondary)
                            Text(date.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            HorizontalDivider(color = DividerColor)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(if (grid) 12.dp else 2.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(monday.plusDays(selected.toLong()).format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${items.size} релизов", color = TextSecondary, fontSize = 10.sp)
                }
            }
            items(items.distinctBy { it.release.id }, key = { it.release.id }, span = { GridItemSpan(if (grid) 1 else 3) }) { entry ->
                val release = releases[entry.release.id] ?: entry.release
                val reminded = release.id in library.reminders
                val update = updates[release.id]
                if (grid) ScheduleTile(release, entry, reminded, update != null, details[release.id], vm)
                else ScheduleRow(release, entry, reminded, update != null, details[release.id], vm)
            }
            if (items.isEmpty()) item(span = { GridItemSpan(3) }) { EmptyState(Icons.Rounded.Weekend, "День для других историй", "На этот день в расписании нет релизов.") }
            item(span = { GridItemSpan(3) }) { Text("API указывает день выхода, а не точное время. Даты могут меняться.", color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

/** Плитка: компактный постер (3 в ряд), под ним — название и выход серии. */
@Composable
private fun ScheduleTile(release: Release, entry: ScheduleItem, reminded: Boolean, hasUpdate: Boolean, detail: Release?, vm: MainViewModel) {
    Column(Modifier.fillMaxWidth().clickable { vm.openDetail(release.id) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(.74f).clip(RoundedCornerShape(11.dp))) {
            PosterArt(release, Modifier.fillMaxSize())
        }
        Text(release.name.main, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp, modifier = Modifier.padding(top = 5.dp).height(26.dp))
        Text(buildString { append(release.year ?: "—"); append(" · "); append(release.type?.value ?: "TV"); append(" · "); append(release.ageRating.label); release.shikimori?.rating?.let { append(" · ★ "); append("%.1f".format(it)) } }, color = TextSecondary, fontSize = 8.5.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        Text(
            if (hasUpdate) "Вышла новая серия" else if (entry.complete) "Сезон целиком" else "Далее: ${entry.next ?: "—"}",
            color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                if (Rules.releasedEpisodes(detail).isNotEmpty()) "Все ${Rules.releasedEpisodes(detail).size}" else "Серии",
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary,
                modifier = Modifier.clickable { vm.openEpisodes(release.id) }.padding(end = 6.dp)
            )
            Spacer(Modifier.weight(1f))
            NotificationBell(reminded, hasUpdate, { vm.repo.toggleReminder(release.id) }, Modifier.size(26.dp))
        }
    }
}

/** Список: постеры как в «Избранном» (104×146). */
@Composable
private fun ScheduleRow(release: Release, entry: ScheduleItem, reminded: Boolean, hasUpdate: Boolean, detail: Release?, vm: MainViewModel) {
    Row(Modifier.fillMaxWidth().clickable { vm.openDetail(release.id) }.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(104.dp, 146.dp).clip(RoundedCornerShape(12.dp))) {
            PosterArt(release, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f).padding(start = 13.dp)) {
            Text(release.name.main, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp, modifier = Modifier.height(33.dp))
            Text("${release.year ?: "—"} · ${release.type?.value ?: "TV"} · ${release.ageRating.label}", color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            if (release.genres.isNotEmpty()) Text(release.genres.take(2).joinToString(" · ") { it.name }, color = TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            Text(
                if (hasUpdate) "Вышла новая серия" else if (entry.complete) "Сезон вышел целиком" else "Следующая серия ${entry.next ?: "—"}",
                color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                modifier = Modifier.padding(top = 5.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                TextButton(onClick = { vm.openEpisodes(release.id) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp), modifier = Modifier.height(30.dp)) {
                    Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(13.dp)); Spacer(Modifier.width(5.dp)); Text(if (Rules.releasedEpisodes(detail).isNotEmpty()) "Все ${Rules.releasedEpisodes(detail).size} серии" else "Серии", fontSize = 10.sp)
                }
                Spacer(Modifier.weight(1f))
                NotificationBell(reminded, hasUpdate, { vm.repo.toggleReminder(release.id) })
            }
        }
    }
}
