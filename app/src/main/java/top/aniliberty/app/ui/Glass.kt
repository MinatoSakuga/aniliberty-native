package top.aniliberty.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Translucent native panel with a guaranteed scrim underneath the highlight. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalAppPalette.current
    val rim = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else palette.glassBorder
    val scrim = if (palette.dark) Color(0xFF14151A).copy(alpha = if (selected) .78f else .68f)
    else Color.White.copy(alpha = if (selected) .88f else .78f)
    val highlight = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (palette.dark) (if (selected) .12f else .08f) else (if (selected) .65f else .5f)),
            Color.White.copy(alpha = if (palette.dark) (if (selected) .045f else .02f) else .18f)
        )
    )
    if (onClick != null) {
        Surface(
            onClick = onClick, modifier = modifier, shape = shape,
            color = Color.Transparent, contentColor = TextPrimary,
            border = BorderStroke(1.dp, rim), shadowElevation = 0.dp
        ) {
            Box(Modifier.fillMaxWidth().background(scrim).background(highlight), contentAlignment = Alignment.Center, content = content)
        }
    } else {
        Surface(
            modifier = modifier, shape = shape,
            color = Color.Transparent, contentColor = TextPrimary,
            border = BorderStroke(1.dp, rim), shadowElevation = 0.dp
        ) {
            Box(Modifier.fillMaxWidth().background(scrim).background(highlight), contentAlignment = Alignment.Center, content = content)
        }
    }
}
