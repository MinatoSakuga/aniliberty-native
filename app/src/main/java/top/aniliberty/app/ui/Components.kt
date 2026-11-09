package top.aniliberty.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import top.aniliberty.app.R
import top.aniliberty.app.data.*

fun bundledRes(context: android.content.Context, key: String): Int = context.resources.getIdentifier(key.lowercase().replace('-', '_'), "raw", context.packageName)

@Composable
fun Artwork(url: String?, fallback: Int, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop) {
    val safeFallback = if (fallback != 0) fallback else R.drawable.ic_brand
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url ?: safeFallback).error(safeFallback).fallback(safeFallback).crossfade(true).build(),
        contentDescription = null, modifier = modifier, contentScale = scale
    )
}

@Composable
fun PosterArt(release: Release, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Artwork(imageUrl(release.poster), bundledRes(context, "p${release.id}"), modifier)
}

@Composable
fun EpisodeArt(releaseId: Int, episode: Episode?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val key = "e${releaseId}_${episode?.number ?: "1"}"
    val fallback = bundledRes(context, key).takeIf { it != 0 } ?: bundledRes(context, "p$releaseId")
    Artwork(imageUrl(episode?.preview), fallback, modifier)
}

@Composable
fun Brand(modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_brand), null, modifier.size(29.dp), tint = MaterialTheme.colorScheme.primary)
}

@Composable
fun PageTitle(title: String, subtitle: String? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(top = 5.dp))
        }
        action?.invoke()
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 23.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null) TextButton(onClick = { onClick?.invoke() }, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(action, fontSize = 11.sp)
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(15.dp))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, button: String? = null, action: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(23.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(68.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
        }
        Spacer(Modifier.height(17.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 9.dp))
        if (button != null) Button(onClick = { action?.invoke() }, modifier = Modifier.padding(top = 18.dp)) { Text(button) }
    }
}

@Composable
fun SmallBadge(text: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Surface(modifier, shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = .55f)) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
    }
}

@Composable
fun ViewToggle(grid: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, border = BorderStroke(1.dp, GlassBorder)) {
        Row(Modifier.padding(3.dp)) {
            listOf(true to Icons.Rounded.GridView, false to Icons.Rounded.ViewAgenda).forEach { (value, icon) ->
                IconButton(onClick = { onChange(value) }, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(if (grid == value) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)) {
                    Icon(icon, if (value) "Плитки" else "Список", Modifier.size(17.dp), tint = if (grid == value) MaterialTheme.colorScheme.primary else TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ReleaseCard(release: Release, grid: Boolean, episodes: Int, favorite: Boolean, onOpen: () -> Unit, onEpisodes: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier, alignHomeActions: Boolean = false) {
    if (grid) {
        Column(modifier.clickable(onClick = onOpen)) {
            CardPoster(release, episodes, true)
            CardInfo(release, episodes, favorite, onEpisodes, onFavorite, false, Modifier.fillMaxWidth().padding(top = 9.dp), alignHomeActions)
        }
    } else {
        Row(modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
            CardPoster(release, episodes, false)
            CardInfo(release, episodes, favorite, onEpisodes, onFavorite, true, Modifier.weight(1f).padding(start = 13.dp))
        }
    }
}

@Composable
private fun CardPoster(release: Release, episodes: Int, grid: Boolean) {
    Box(Modifier.then(if (grid) Modifier.fillMaxWidth().aspectRatio(.72f) else Modifier.size(104.dp, 146.dp)).clip(RoundedCornerShape(if (grid) 14.dp else 12.dp))) {
        PosterArt(release, Modifier.fillMaxSize())
    }
}

@Composable
private fun CardInfo(release: Release, episodes: Int, favorite: Boolean, onEpisodes: () -> Unit, onFavorite: () -> Unit, list: Boolean, modifier: Modifier, alignHomeActions: Boolean = false) {
    Column(modifier) {
        Text(release.name.main, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = if (alignHomeActions) Modifier.height(40.dp) else Modifier)
        Text(buildString {
            append(release.year ?: "—")
            append(" · ")
            append(release.type?.value ?: release.type?.description ?: "TV")
            append(" · ")
            append(release.ageRating.label)
            release.shikimori?.rating?.let { append(" · ★ ${"%.1f".format(it)}") }
        }, color = TextSecondary, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (alignHomeActions) 3.dp else 4.dp))
        if (list && release.genres.isNotEmpty()) Text(release.genres.take(2).joinToString(" · ") { it.name }, color = TextDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
            TextButton(onClick = onEpisodes, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp), modifier = Modifier.height(30.dp)) {
                Icon(Icons.Rounded.VideoLibrary, null, Modifier.size(13.dp)); Spacer(Modifier.width(5.dp)); Text(if (episodes > 0) "Все $episodes" else "Серии", fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFavorite, modifier = Modifier.size(30.dp)) {
                Icon(if (favorite) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Избранное", Modifier.size(17.dp), tint = if (favorite) MaterialTheme.colorScheme.primary else TextSecondary)
            }
        }
    }
}

@Composable
fun ContinueCard(entry: WatchEntry, release: Release, onClick: () -> Unit) {
    Surface(Modifier.width(275.dp).clickable(onClick = onClick), shape = RoundedCornerShape(17.dp), color = SurfaceDark) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(91.dp, 67.dp).clip(RoundedCornerShape(11.dp))) {
                EpisodeArt(entry.releaseId, Episode(ordinal = entry.ordinal), Modifier.fillMaxSize())
                Icon(Icons.Rounded.PlayCircle, null, Modifier.align(Alignment.Center).size(28.dp), Color.White)
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(release.name.main, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Серия ${if (entry.ordinal % 1 == 0.0) entry.ordinal.toInt() else entry.ordinal} · ${formatTime(entry.time)}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
                LinearProgressIndicator(progress = { entry.progress }, modifier = Modifier.fillMaxWidth().padding(top = 11.dp).height(3.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = SurfaceHigh)
            }
        }
    }
}

@Composable
fun EpisodeRow(releaseId: Int, episode: Episode, watched: Boolean, progress: WatchEntry?, seen: Boolean = watched, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(13.dp), color = if (progress != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else Color.Transparent, contentColor = TextPrimary, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(70.dp, 47.dp).clip(RoundedCornerShape(9.dp))) {
                EpisodeArt(releaseId, episode, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)), contentAlignment = Alignment.Center) { Text(episode.number, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White) }
                if (seen) Icon(Icons.Rounded.CheckCircle, null, Modifier.align(Alignment.BottomEnd).padding(3.dp).size(15.dp), MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(episode.name?.takeIf { it.isNotBlank() } ?: "Серия ${episode.number}", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(formatTime(episode.duration ?: 0.0) + if (watched) " · просмотрено" else if (seen || progress != null) " · начато${progress?.let { " · ${formatTime(it.time)}" } ?: ""}" else "", color = if (watched) MaterialTheme.colorScheme.primary else TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }
            Icon(if (seen) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow, null, Modifier.size(21.dp), tint = if (seen) MaterialTheme.colorScheme.primary else TextSecondary)
        }
    }
}

@Composable
fun NativeBack(title: String, onBack: () -> Unit, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun AppFooter() {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Brand(Modifier.size(21.dp))
        Text("AniLiberty", color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun NotificationBell(reminded: Boolean, hasUpdate: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        IconButton(onClick = onClick) {
            Icon(if (reminded) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone, if (reminded) "Уведомления включены" else "Включить уведомления", tint = if (reminded) MaterialTheme.colorScheme.primary else TextSecondary)
        }
        if (hasUpdate) Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(15.dp)
        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, "Вышла новая серия", Modifier.size(10.dp)) } }
    }
}
