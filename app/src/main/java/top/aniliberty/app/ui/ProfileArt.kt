package top.aniliberty.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun XpRing(progress: Float, modifier: Modifier = Modifier, stroke: Dp = 4.dp) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val sw = stroke.toPx()
        val r = size.minDimension / 2f - sw / 2
        drawCircle(Color.White.copy(alpha = .13f), r, style = Stroke(sw, cap = StrokeCap.Round))
        val p = progress.coerceIn(0f, 1f)
        if (p > 0.01f) drawArc(
            brush = Brush.linearGradient(listOf(Color(0xFFFF9D6C), accent)),
            startAngle = -90f, sweepAngle = 360f * p, useCenter = false,
            topLeft = Offset(center.x - r, center.y - r), size = Size(2 * r, 2 * r),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
    }
}

/** Five built-in covers (шапки профиля) drawn with thin flat shapes. */
@Composable
fun CoverArt(style: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val grad = when (style) {
            1 -> listOf(Color(0xFF0B1030), Color(0xFF2A2F7E), Color(0xFF5C6BC0))
            2 -> listOf(Color(0xFF4A1230), Color(0xFFF27BA0), Color(0xFFFFD2E2))
            3 -> listOf(Color(0xFF120B2E), Color(0xFF241B52), Color(0xFF5C6BC0))
            4 -> listOf(Color(0xFF22825F), Color(0xFF5FD4A8), Color(0xFFC3B0EE))
            else -> listOf(Color(0xFF3E0E1B), Color(0xFFFE3635), Color(0xFF8E2340))
        }
        drawRect(Brush.linearGradient(grad))
        when (style) {
            0 -> { // Красный рассвет
                drawCircle(Color.White.copy(alpha = .5f), h * .21f, Offset(w * .78f, h * .26f))
                val back = Path().apply {
                    moveTo(0f, h * .8f); lineTo(w * .22f, h * .44f); lineTo(w * .4f, h * .7f)
                    lineTo(w * .62f, h * .38f); lineTo(w * .85f, h * .72f); lineTo(w, h * .5f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(back, Color(0x801E060E))
                val front = Path().apply {
                    moveTo(0f, h * .93f); lineTo(w * .2f, h * .68f); lineTo(w * .45f, h * .9f)
                    lineTo(w * .7f, h * .64f); lineTo(w, h * .88f); lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(front, Color(0x8C14040A))
                val sp = Offset(w * .18f, h * .22f); val l = h * .06f
                drawLine(Color.White.copy(alpha = .8f), Offset(sp.x - l, sp.y), Offset(sp.x + l, sp.y), 1.dp.toPx())
                drawLine(Color.White.copy(alpha = .8f), Offset(sp.x, sp.y - l), Offset(sp.x, sp.y + l), 1.dp.toPx())
            }
            1 -> { // Кибер-ночь
                val towers = listOf(.22f to .38f, .18f to .24f, .3f to .5f, .18f to .16f, .28f to .36f, .16f to .28f, .32f to .46f, .22f to .22f, .2f to .42f)
                var x = w * .03f
                towers.forEach { (tw, th) ->
                    val width = w * tw * .45f
                    val top = h * (1f - th)
                    drawRect(Color(0x8C5BB8F5), topLeft = Offset(x, top), size = Size(width, h - top))
                    x += width + w * .012f
                }
                drawLine(Color.White.copy(alpha = .5f), Offset(0f, h * .62f), Offset(w, h * .62f), 1.4.dp.toPx())
                drawLine(Color.White.copy(alpha = .5f), Offset(0f, h * .76f), Offset(w, h * .76f), 1.4.dp.toPx())
                listOf(.19f, .41f, .83f).forEach { fx ->
                    drawRect(Color(0xFFE040FB), topLeft = Offset(w * fx, h * .3f), size = Size(3.dp.toPx(), h * .1f))
                }
            }
            2 -> { // Сакура
                val petals = listOf(Triple(.19f, .23f, 30f), Triple(.38f, .44f, -20f), Triple(.63f, .2f, 40f), Triple(.82f, .49f, -35f), Triple(.5f, .76f, 15f), Triple(.94f, .23f, -25f))
                petals.forEach { (px, py, angle) ->
                    rotate(angle) {
                        drawOval(
                            Color.White.copy(alpha = .55f),
                            topLeft = Offset(w * px - w * .022f, h * py - h * .045f),
                            size = Size(w * .044f, h * .09f)
                        )
                    }
                }
                val hill = Path().apply {
                    moveTo(0f, h * .9f)
                    cubicTo(w * .2f, h * .76f, w * .35f, h * .9f, w * .53f, h * .79f)
                    cubicTo(w * .72f, h * .7f, w * .88f, h * .84f, w, h * .76f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(hill, Color(0x734A1230))
            }
            3 -> { // Глубокий космос
                listOf(Triple(.11f, .18f, 1.5f), Triple(.26f, .4f, 1f), Triple(.4f, .14f, 1.8f), Triple(.55f, .33f, 1f), Triple(.69f, .17f, 1.5f), Triple(.84f, .43f, 1f), Triple(.94f, .15f, 1.8f), Triple(.18f, .66f, 1f), Triple(.78f, .72f, 1.3f)).forEach { (sx, sy, sr) ->
                    drawCircle(Color.White.copy(alpha = .75f), sr.dp.toPx(), Offset(w * sx, h * sy))
                }
                drawCircle(Color(0x599D8CFF), h * .26f, Offset(w * .74f, h * .46f))
                drawCircle(Color(0x80D9CCFF), h * .23f, Offset(w * .71f, h * .42f))
                val s1 = Offset(w * .34f, h * .23f); val l = h * .05f
                drawLine(Color.White, Offset(s1.x - l, s1.y), Offset(s1.x + l, s1.y), 1.dp.toPx())
                drawLine(Color.White, Offset(s1.x, s1.y - l), Offset(s1.x, s1.y + l), 1.dp.toPx())
                val s2 = Offset(w * .9f, h * .74f); val l2 = h * .04f
                drawLine(Color.White.copy(alpha = .8f), Offset(s2.x - l2, s2.y), Offset(s2.x + l2, s2.y), 1.dp.toPx())
                drawLine(Color.White.copy(alpha = .8f), Offset(s2.x, s2.y - l2), Offset(s2.x, s2.y + l2), 1.dp.toPx())
            }
            4 -> { // Мятный изгиб
                val wave1 = Path().apply {
                    moveTo(-w * .06f, h * .72f)
                    cubicTo(w * .19f, h * .39f, w * .38f, h * .83f, w * .63f, h * .5f)
                    cubicTo(w * .81f, h * .26f, w, h * .59f, w * 1.06f, h * .39f)
                }
                drawPath(wave1, Color.White.copy(alpha = .35f), style = Stroke(h * .075f))
                val wave2 = Path().apply {
                    moveTo(-w * .06f, h * .9f)
                    cubicTo(w * .22f, h * .59f, w * .44f, h * .95f, w * .66f, h * .7f)
                    cubicTo(w * .85f, h * .48f, w * 1.03f, h * .76f, w * 1.06f, h * .62f)
                }
                drawPath(wave2, Color.White.copy(alpha = .22f), style = Stroke(h * .055f))
            }
        }
    }
}

/** XP ring -> avatar, with level chip and optional pencil badge. */
@Composable
fun AvatarStack(
    avatarUrl: String?,
    xpProgress: Float,
    levelLabel: String?,
    size: Dp,
    pen: Boolean = false,
    onPen: (() -> Unit)? = null
) {
    Box(Modifier.size(size)) {
        XpRing(xpProgress, Modifier.fillMaxSize().padding(size * .055f), stroke = size * .032f)
        Box(
            Modifier.fillMaxSize().padding(size * .115f).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border((size * .018f), Bg, CircleShape)
        ) {
            if (avatarUrl != null) Artwork(avatarUrl, 0, Modifier.fillMaxSize())
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, Modifier.size(size * .38f), tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (levelLabel != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).offset(y = size * .05f).clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = size * .075f, vertical = size * .042f)
            ) {
                Text(levelLabel, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * .082f).sp, maxLines = 1)
            }
        }
        if (pen) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = size * .02f, y = size * .02f).size(size * .25f)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { onPen?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Edit, "Аватар", Modifier.size(size * .12f), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** Cover thumbnail for the editor picker. */
@Composable
fun CoverSwatch(style: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(width = 76.dp, height = 50.dp).clip(shape).clickable(onClick = onClick)
            .then(if (selected) Modifier.border(2.dp, Color.White, shape) else Modifier)
    ) {
        CoverArt(style, Modifier.fillMaxSize())
    }
}
