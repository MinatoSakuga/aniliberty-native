@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package top.aniliberty.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import top.aniliberty.app.R
import kotlin.math.pow

data class Accent(val key: String, val title: String, val color: Color)
val accents = listOf(
    Accent("red", "Красный AniLiberty", Color(0xFFFE3635)),
    Accent("coral", "Персиковый", Color(0xFFFF825B)),
    Accent("sage", "Шалфейный", Color(0xFFB7D9A8)),
    Accent("lavender", "Лавандовый", Color(0xFFC3B0EE)),
    Accent("sky", "Небесный", Color(0xFF5BB8F5)),
    Accent("mint", "Мятный", Color(0xFF5FD4A8)),
    Accent("amber", "Янтарный", Color(0xFFF2B544)),
    Accent("rose", "Розовый", Color(0xFFF27BA0)),
    Accent("violet", "Фиолетовый", Color(0xFF9D8CFF)),
    Accent("lime", "Лаймовый", Color(0xFFC6F432)),
    Accent("turquoise", "Бирюзовый", Color(0xFF00C2B8)),
    Accent("indigo", "Индиго", Color(0xFF5C6BC0)),
    Accent("fuchsia", "Фуксия", Color(0xFFE040FB)),
    Accent("copper", "Медный", Color(0xFFC87941))
)

data class AppPalette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val textSecondary: Color,
    val textDim: Color,
    val outline: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val glassScrim: Color,
    val navTint: Color,
    val divider: Color
)
val LocalAppPalette = staticCompositionLocalOf {
    AppPalette(true, Color(0xFF101113), Color(0xFF191A1E), Color(0xFF24252B), Color(0xFFF6F4F1), Color(0xFFB4B4BE), Color(0xFF9A9BA6), Color(0xFF4A4B55), Color.White.copy(alpha = .05f), Color.White.copy(alpha = .14f), Color(0xFF14151A).copy(alpha = .68f), Color(0xFF17191F).copy(alpha = .46f), Color.White.copy(alpha = .06f))
}
val Bg: Color @Composable get() = LocalAppPalette.current.bg
val SurfaceDark: Color @Composable get() = LocalAppPalette.current.surface
val SurfaceHigh: Color @Composable get() = LocalAppPalette.current.surfaceHigh
val TextPrimary: Color @Composable get() = LocalAppPalette.current.text
val TextSecondary: Color @Composable get() = LocalAppPalette.current.textSecondary
val TextDim: Color @Composable get() = LocalAppPalette.current.textDim
val GlassFill: Color @Composable get() = LocalAppPalette.current.glassFill
val GlassBorder: Color @Composable get() = LocalAppPalette.current.glassBorder
val GlassScrim: Color @Composable get() = LocalAppPalette.current.glassScrim
val DividerColor: Color @Composable get() = LocalAppPalette.current.divider
val IsDarkTheme: Boolean @Composable get() = LocalAppPalette.current.dark
const val OnImage = 0xFFFFFFFF
val OnImageColor = Color(OnImage)
val OnImageDim = Color(0xFFD2D3D8)

private fun srgbLuminance(c: Color): Double {
    fun lin(v: Float): Double { val x = v.toDouble(); return if (x <= .04045) x / 12.92 else ((x + .055) / 1.055).pow(2.4) }
    return .2126 * lin(c.red) + .7152 * lin(c.green) + .0722 * lin(c.blue)
}
fun contrastRatio(a: Color, b: Color): Double {
    val la = srgbLuminance(a); val lb = srgbLuminance(b)
    return (maxOf(la, lb) + .05) / (minOf(la, lb) + .05)
}
private fun readableAccent(base: Color, bg: Color, dark: Boolean): Color {
    var c = base
    var step = 0
    while (contrastRatio(c, bg) < 4.5 && step < 24) {
        c = if (dark) c.copy(red = (c.red + (1f - c.red) * .08f), green = (c.green + (1f - c.green) * .08f), blue = (c.blue + (1f - c.blue) * .08f))
        else c.copy(red = c.red * .92f, green = c.green * .92f, blue = c.blue * .92f)
        step++
    }
    return c
}
private fun onColor(c: Color): Color = if (contrastRatio(Color.White, c) >= contrastRatio(Color(0xFF161313), c)) Color.White else Color(0xFF161313)

private fun manrope(weight: FontWeight, wght: Int) = Font(
    R.font.manrope, weight,
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
        androidx.compose.ui.text.font.FontVariation.Setting("wght", wght.toFloat())
    )
)
private val manrope = FontFamily(
    manrope(FontWeight.Light, 300), manrope(FontWeight.Normal, 400), manrope(FontWeight.Medium, 500),
    manrope(FontWeight.SemiBold, 600), manrope(FontWeight.Bold, 700), manrope(FontWeight.ExtraBold, 800)
)
private val typo = Typography(
    headlineLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 35.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
    labelSmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Medium, fontSize = 9.sp, lineHeight = 13.sp)
)

@Composable fun AniTheme(accent: String, themeMode: String = "dark", content: @Composable () -> Unit) {
    val dark = themeMode != "light"
    val palette = if (dark) LocalAppPalette.current else AppPalette(
        dark = false,
        bg = Color(0xFFF6F7FA), surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFECEEF4),
        text = Color(0xFF17181C), textSecondary = Color(0xFF494C56), textDim = Color(0xFF666A75),
        outline = Color(0xFFC7CAD4),
        glassFill = Color.White.copy(alpha = .55f), glassBorder = Color(0xFF101113).copy(alpha = .10f),
        glassScrim = Color(0xFFFFFFFF).copy(alpha = .78f), navTint = Color(0xFFF1F3F8).copy(alpha = .72f),
        divider = Color(0xFF101113).copy(alpha = .08f)
    )
    val base = accents.firstOrNull { it.key == accent }?.color ?: accents.first().color
    val primary = readableAccent(base, palette.bg, dark)
    val onPrimary = onColor(primary)
    val scheme = if (dark) darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primary.copy(alpha = .18f), onPrimaryContainer = primary,
        secondary = primary, onSecondary = onPrimary,
        background = palette.bg, onBackground = palette.text,
        surface = palette.surface, onSurface = palette.text,
        surfaceVariant = palette.surfaceHigh, onSurfaceVariant = palette.textSecondary,
        surfaceTint = primary, outline = palette.outline, outlineVariant = palette.divider,
        error = Color(0xFFFFB4A2), onError = Color(0xFF3A0A05),
        inverseSurface = Color(0xFFE8E6E3), inverseOnSurface = Color(0xFF1A1B1E)
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primary.copy(alpha = .12f).compositeOver(palette.surface), onPrimaryContainer = primary,
        secondary = primary, onSecondary = onPrimary,
        background = palette.bg, onBackground = palette.text,
        surface = palette.surface, onSurface = palette.text,
        surfaceVariant = palette.surfaceHigh, onSurfaceVariant = palette.textSecondary,
        surfaceTint = primary, outline = palette.outline, outlineVariant = palette.divider,
        error = Color(0xFFB3261E), onError = Color.White,
        inverseSurface = Color(0xFF2A2B30), inverseOnSurface = Color(0xFFF2F2F5)
    )
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = typo) {
            CompositionLocalProvider(LocalContentColor provides palette.text, content = content)
        }
    }
}
