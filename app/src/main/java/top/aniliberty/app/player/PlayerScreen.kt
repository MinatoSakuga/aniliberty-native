package top.aniliberty.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.data.*

@androidx.annotation.OptIn(UnstableApi::class)
@Composable fun PlayerScreen(vm: MainViewModel, request: PlayerRequest) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by vm.repo.settings.collectAsStateWithLifecycle()
    val release = vm.repo.release(request.releaseId)
    val episodes = vm.repo.episodes(request.releaseId)
    val index = episodes.indexOfFirst { it.ordinal == request.episode.ordinal }
    var quality by remember(request.episode.id) { mutableIntStateOf(if (request.episode.hls720 != null) 720 else if (request.episode.hls1080 != null) 1080 else 480) }
    var speed by remember(request.episode.id) { mutableFloatStateOf(1f) }
    var retry by remember(request.episode.id) { mutableIntStateOf(0) }
    val player = remember(request.episode.id) {
        ExoPlayer.Builder(context).setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_NETWORK).build().apply {
            setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            playWhenReady = true
        }
    }
    var playing by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(request.resumeMs) }
    var duration by remember { mutableLongStateOf(((request.episode.duration ?: 0.0) * 1000).toLong()) }
    var error by remember { mutableStateOf<String?>(null) }
    var controls by remember { mutableStateOf(true) }
    var seeking by remember { mutableStateOf(false) }
    var qualityMenu by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    var seekAccum by remember { mutableLongStateOf(0L) }
    var lastSeekAt by remember { mutableLongStateOf(0L) }
    var opened by remember(request.episode.id) { mutableStateOf(false) }

    fun save(watched: Boolean = false) {
        if (player.currentPosition > 1000 || watched) vm.repo.saveProgress(request.releaseId, request.episode, player.currentPosition, player.duration.takeIf { it > 0 } ?: duration, watched)
    }
    fun seekBy(deltaMs: Long) {
        val now = System.currentTimeMillis()
        seekAccum = if (now - lastSeekAt < 900) seekAccum + deltaMs else deltaMs
        lastSeekAt = now
        val target = Rules.seekTarget(player.currentPosition, player.duration.takeIf { it > 0 } ?: duration, deltaMs)
        player.seekTo(target)
        position = target
        seekFeedback = (if (deltaMs < 0) -1 else 1) to seekAccum
        controls = true
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { playing = value }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0)
                    if (!opened) { opened = true; vm.repo.markOpened(request.releaseId, request.episode, player.currentPosition, player.duration) }
                }
                if (state == Player.STATE_ENDED) save(true)
            }
            override fun onPlayerError(e: PlaybackException) { error = "Не удалось загрузить видео. Проверьте интернет или выберите другое качество."; buffering = false }
        }
        player.addListener(listener)
        onDispose { save(); player.removeListener(listener); player.release(); view.keepScreenOn = false }
    }
    LaunchedEffect(request.episode.id, quality, retry) {
        error = null; buffering = true
        val url = request.episode.url(quality)
        if (url == null) { error = "Это качество недоступно"; buffering = false; return@LaunchedEffect }
        val resume = if (player.currentPosition > 1000) player.currentPosition else request.resumeMs
        val source = HlsMediaSource.Factory(DefaultHttpDataSource.Factory().setUserAgent("AniLibertyNative/1.8.5 (Android)").setConnectTimeoutMs(20_000).setReadTimeoutMs(30_000).setAllowCrossProtocolRedirects(true)).createMediaSource(MediaItem.fromUri(url))
        player.setMediaSource(source); player.prepare(); if (resume > 0) player.seekTo(resume); player.playWhenReady = true; player.setPlaybackSpeed(speed)
    }
    LaunchedEffect(player) { while (true) { delay(250); if (!seeking) position = player.currentPosition.coerceAtLeast(0); if (player.duration > 0) duration = player.duration; if (Rules.shouldSkipOpening(position, request.episode.opening, settings.skipOpening)) { player.seekTo(((request.episode.opening?.stop ?: 0.0) * 1000).toLong()); vm.repo.message("Опенинг пропущен") } } }
    LaunchedEffect(player) { while (true) { delay(10_000); if (player.isPlaying) save() } }
    LaunchedEffect(playing, buffering) { view.keepScreenOn = playing || buffering }
    LaunchedEffect(controls, playing) { if (controls && playing) { delay(3800); controls = false } }
    LaunchedEffect(seekFeedback) { if (seekFeedback != null) { delay(800); seekFeedback = null; seekAccum = 0L } }
    val close = { save(); vm.closePlayer() }

    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
        detectTapGestures(
            onTap = { controls = !controls },
            onDoubleTap = { offset -> seekBy(if (offset.x < size.width / 2f) -10_000L else 10_000L) }
        )
    }) {
        AndroidView(factory = { PlayerView(context).apply { useController = false; setShutterBackgroundColor(android.graphics.Color.BLACK); resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT; this.player = player } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
        if (buffering && error == null) CircularProgressIndicator(Modifier.align(Alignment.Center).size(42.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        if (error != null) Column(Modifier.align(Alignment.Center).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.WifiOff, null, Modifier.size(38.dp), tint = Color.White); Text(error!!, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)); Button(onClick = { retry++ }, modifier = Modifier.padding(top = 16.dp)) { Text("Повторить") } }

        seekFeedback?.let { (direction, amount) ->
            Surface(Modifier.align(if (direction < 0) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 28.dp), shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = .55f), contentColor = Color.White) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (direction < 0) Icons.Rounded.FastRewind else Icons.Rounded.FastForward, null, Modifier.size(28.dp))
                    Text("${if (amount > 0) "+" else ""}${amount / 1000} сек", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        val opening = request.episode.opening
        val openingStart = opening?.start ?: 0.0
        val openingStop = opening?.stop
        if (openingStop != null && openingStop > openingStart && position / 1000.0 >= openingStart && position / 1000.0 < openingStop && error == null) {
            Surface(onClick = { player.seekTo((openingStop * 1000).toLong()) }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 76.dp), shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = .6f), contentColor = Color.White) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FastForward, null, Modifier.size(16.dp))
                    Text("Пропустить опенинг", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }

        AnimatedVisibility(visible = controls || !playing, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().align(Alignment.TopStart).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .8f), Color.Transparent))).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = close) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Закрыть плеер", tint = Color.White) }
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(release?.name?.main ?: "AniLiberty", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Серия ${request.episode.number}${request.episode.name?.let { " · $it" } ?: ""}", color = Color.White.copy(alpha = .7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box {
                            TextButton(onClick = { speedMenu = true }) { Text("${speed}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            DropdownMenu(speedMenu, { speedMenu = false }) { listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s -> DropdownMenuItem(text = { Text("${s}x${if (s == speed) " ✓" else ""}") }, onClick = { speedMenu = false; speed = s; player.setPlaybackSpeed(s) }) } }
                        }
                        Box {
                            IconButton(onClick = { qualityMenu = true }) { Row(verticalAlignment = Alignment.CenterVertically) { Text("${quality}p", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold); Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(16.dp), tint = Color.White) } }
                            DropdownMenu(qualityMenu, { qualityMenu = false }) { listOf(1080, 720, 480).forEach { q -> val actual = request.episode.url(q) != null; DropdownMenuItem(enabled = actual, text = { Text("${q}p${if (q == quality) " ✓" else ""}") }, onClick = { qualityMenu = false; save(); quality = q }) } }
                        }
                    }
                }
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    IconButton(onClick = { seekBy(-10_000L) }, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.Replay10, "Назад 10 секунд", Modifier.size(28.dp), tint = Color.White) }
                    IconButton(onClick = { save(); vm.stepEpisode(-1) }, enabled = index > 0, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.SkipPrevious, "Предыдущая серия", Modifier.size(30.dp), tint = if (index > 0) Color.White else Color.White.copy(alpha = .25f)) }
                    Surface(onClick = { if (player.isPlaying) player.pause() else player.play() }, shape = CircleShape, color = Color.Black.copy(alpha = .45f), modifier = Modifier.size(66.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(38.dp), tint = Color.White) } }
                    IconButton(onClick = { save(); vm.stepEpisode(1) }, enabled = index < episodes.lastIndex, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.SkipNext, "Следующая серия", Modifier.size(30.dp), tint = if (index < episodes.lastIndex) Color.White else Color.White.copy(alpha = .25f)) }
                    IconButton(onClick = { seekBy(10_000L) }, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.Forward10, "Вперёд 10 секунд", Modifier.size(28.dp), tint = Color.White) }
                }
                Column(Modifier.fillMaxWidth().align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .85f)))).padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(position / 1000.0), color = Color.White.copy(alpha = .8f), fontSize = 10.sp, modifier = Modifier.width(48.dp))
                        PlayerSeekBar(positionMs = position, durationMs = duration, accent = MaterialTheme.colorScheme.primary, onSeek = { seeking = true; position = it }, onSeekStart = { seeking = true }, onSeekEnd = { player.seekTo(position); seeking = false; save() }, modifier = Modifier.weight(1f))
                        Text(formatTime(duration / 1000.0), color = Color.White.copy(alpha = .8f), fontSize = 10.sp, modifier = Modifier.width(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSeekBar(positionMs: Long, durationMs: Long, accent: Color, onSeek: (Long) -> Unit, onSeekStart: () -> Unit, onSeekEnd: () -> Unit, modifier: Modifier = Modifier) {
    val total = durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Canvas(modifier.height(24.dp)
        .pointerInput(total) {
            detectTapGestures { offset ->
                onSeekStart()
                onSeek((offset.x / size.width.coerceAtLeast(1) * total).toLong().coerceIn(0L, total))
                onSeekEnd()
            }
        }
        .pointerInput(total) {
            detectHorizontalDragGestures(onDragStart = { onSeekStart() }, onDragEnd = { onSeekEnd() }, onDragCancel = { onSeekEnd() }) { change, _ ->
                onSeek((change.position.x / size.width.coerceAtLeast(1) * total).toLong().coerceIn(0L, total))
            }
        }
    ) {
        val y = size.height / 2f
        val x = fraction * size.width
        val track = 3.dp.toPx()
        val thumb = 6.dp.toPx()
        drawLine(Color.White.copy(alpha = .25f), Offset(0f, y), Offset(size.width, y), strokeWidth = track, cap = StrokeCap.Round)
        if (x > 0f) drawLine(accent, Offset(0f, y), Offset(x, y), strokeWidth = track, cap = StrokeCap.Round)
        drawCircle(accent, radius = thumb, center = Offset(x, y))
        drawCircle(Color.White, radius = thumb * .42f, center = Offset(x, y))
    }
}

