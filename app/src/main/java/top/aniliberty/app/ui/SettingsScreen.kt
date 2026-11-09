package top.aniliberty.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.repo.settings.collectAsStateWithLifecycle()
    val auth by vm.repo.auth.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var about by remember { mutableStateOf(false) }
    var clear by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.notificationsEnabled(granted)
        if (!granted) vm.repo.message("Разрешите уведомления в настройках Android")
    }
    Column(Modifier.fillMaxSize()) {
        NativeBack("Настройки", vm::back)
        LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 120.dp)) {
            item {
                SettingsLabel("АККАУНТ")
                Surface(onClick = { if (auth.profile == null) vm.openLogin() else vm.navigate(top.aniliberty.app.Screen.PROFILE) }, color = Color.Transparent, contentColor = TextPrimary) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, null, Modifier.size(20.dp), tint = TextSecondary)
                        Text(auth.profile?.displayName ?: "Войти в AniLiberty", modifier = Modifier.weight(1f).padding(start = 12.dp), fontSize = 13.sp)
                        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = TextDim)
                    }
                }
            }
            item {
                SettingsLabel("ТЕМА")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("dark" to "Тёмная", "light" to "Светлая").forEach { (key, title) ->
                        FilterChip(selected = settings.themeMode == key, onClick = { vm.repo.store.updateSettings { it.copy(themeMode = key) } }, label = { Text(title) }, modifier = Modifier.weight(1f))
                    }
                }
                SettingsLabel("АКЦЕНТ")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalArrangement = Arrangement.spacedBy(13.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                    accents.forEach { accent ->
                        Surface(onClick = { vm.repo.store.updateSettings { it.copy(accent = accent.key) } }, shape = CircleShape, color = accent.color, border = BorderStroke(if (settings.accent == accent.key) 3.dp else 0.dp, TextPrimary), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { if (settings.accent == accent.key) Icon(Icons.Rounded.Check, accent.title, Modifier.size(19.dp), tint = if (contrastRatio(Color.White, accent.color) >= 3.0) Color.White else Color(0xFF171719)) }
                        }
                    }
                }
            }
            item {
                SettingsLabel("ВНЕШНИЙ ВИД")
                SettingsRow("Каталог плитками", "Единый вид для всего каталога", settings.catalogGrid) { value -> vm.repo.store.updateSettings { it.copy(catalogGrid = value) } }
                SettingsRow("Коллекции плитками", "Единый вид для всех вкладок избранного", settings.libraryGrid) { value -> vm.repo.store.updateSettings { it.copy(libraryGrid = value) } }
                SettingsRow("Меньше движения", "Отключить анимации интерфейса", settings.reduceMotion) { value -> vm.repo.store.updateSettings { it.copy(reduceMotion = value) } }
            }
            item {
                SettingsLabel("ПЛЕЕР И КОНТЕНТ")
                SettingsRow("Автопропуск опенинга", "По таймкодам серии из AniLiberty", settings.skipOpening) { value -> vm.repo.store.updateSettings { it.copy(skipOpening = value) } }
                SettingsRow("Скрывать 18+", "По умолчанию выключено. Поиск всегда показывает все совпадения.", settings.hideAdult) { value -> vm.repo.store.updateSettings { it.copy(hideAdult = value) }; vm.repo.loadCatalog() }
            }
            item {
                SettingsLabel("УВЕДОМЛЕНИЯ")
                SettingsRow("Новые серии", "Фоновая проверка примерно раз в 15 минут и мгновенная проверка при включении", settings.notifications) { enabled ->
                    if (!enabled) vm.notificationsEnabled(false)
                    else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.notificationsEnabled(true)
                }
                if (settings.notifications) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !settings.notifyAll, onClick = { vm.repo.store.updateSettings { it.copy(notifyAll = false) } }, label = { Text("Мои коллекции") })
                        FilterChip(selected = settings.notifyAll, onClick = { vm.repo.store.updateSettings { it.copy(notifyAll = true) } }, label = { Text("Все новинки") })
                    }
                }
            }
            item {
                SettingsLabel("ДАННЫЕ")
                if (auth.profile == null) SettingsAction("Очистить коллекции и историю", Icons.Rounded.DeleteOutline) { clear = true }
                else Text("Коллекции и история хранятся в аккаунте AniLiberty. Синхронизация выполняется автоматически.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                SettingsAction("О приложении", Icons.Rounded.Info) { about = true }
                AppFooter()
            }
        }
    }
    if (about) AlertDialog(onDismissRequest = { about = false }, containerColor = SurfaceDark, titleContentColor = TextPrimary, textContentColor = TextSecondary, title = { Text("AniLiberty", color = TextPrimary) }, text = { Text("Нативное приложение на Kotlin и Jetpack Compose. Видео — Media3 ExoPlayer. Каталог, аккаунт и серии — AniLiberty API V1.\n\nВход выполняется напрямую на aniliberty.top. Пароль не сохраняется; токен защищён Android Keystore. Проект не связан с командой AniLiberty.") }, confirmButton = { TextButton(onClick = { about = false }) { Text("Понятно") } })
    if (clear) AlertDialog(onDismissRequest = { clear = false }, containerColor = SurfaceDark, titleContentColor = TextPrimary, textContentColor = TextSecondary, title = { Text("Начать с чистого листа?", color = TextPrimary) }, text = { Text("Гостевые коллекции и история будут удалены с этого устройства. Настройки останутся.") }, confirmButton = { TextButton(onClick = { vm.repo.clearGuestData(false); clear = false }) { Text("Очистить") } }, dismissButton = { TextButton(onClick = { clear = false }) { Text("Отмена") } })
}
@Composable private fun SettingsLabel(text: String) { Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 7.dp)) }
@Composable private fun SettingsRow(title: String, subtitle: String, checked: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f).padding(end = 15.dp)) { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) }; Switch(checked, change) } }
@Composable private fun SettingsAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) { Surface(onClick = action, color = Color.Transparent, contentColor = TextPrimary) { Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(19.dp), tint = TextSecondary); Text(title, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp)) } } }
