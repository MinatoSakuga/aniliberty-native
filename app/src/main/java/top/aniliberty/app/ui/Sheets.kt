package top.aniliberty.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EpisodesSheet(vm: MainViewModel, id: Int) {
    val details by vm.repo.details.collectAsStateWithLifecycle(); val library by vm.repo.library.collectAsStateWithLifecycle(); val release = vm.repo.release(id); val episodes = Rules.releasedEpisodes(details[id])
    var query by remember(id) { mutableStateOf("") }; var descending by remember(id) { mutableStateOf(false) }
    val filtered = episodes.filter { query.isBlank() || it.number.contains(query) || it.name?.contains(query, true) == true }.let { if (descending) it.reversed() else it }
    ModalBottomSheet(onDismissRequest = vm::dismissEpisodes, containerColor = SurfaceDark, contentColor = TextPrimary, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).padding(horizontal = 18.dp)) {
            Text(release?.name?.main ?: "Серии", style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.padding(horizontal = 4.dp)); Text("Вышло ${episodes.size} · просмотрено ${episodes.count { Rules.watched(library.history["$id:${it.ordinal}"]) }}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(query, { query = it }, placeholder = { Text("Номер или название серии", fontSize = 12.sp) }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium); IconButton(onClick = { descending = !descending }) { Icon(Icons.Rounded.SwapVert, "Порядок серий") } }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 10.dp)) { items(filtered, key = { it.id.ifBlank { it.ordinal.toString() } }) { episode -> EpisodeRow(id, episode, Rules.watched(library.history["$id:${episode.ordinal}"]), library.history["$id:${episode.ordinal}"], seen = Rules.seen(library.history["$id:${episode.ordinal}"])) { vm.openPlayer(id, episode.ordinal) } } }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CollectionSheet(vm: MainViewModel, id: Int) {
    val library by vm.repo.library.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = vm::dismissCollection, containerColor = SurfaceDark, contentColor = TextPrimary) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp)) { Text("Добавить в коллекцию", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 14.dp)); CollectionKind.entries.forEach { kind -> val selected = library.collections[kind.key]?.contains(id) == true; Surface(onClick = { vm.chooseCollection(id, kind) }, color = Color.Transparent, contentColor = TextPrimary, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(when (kind) { CollectionKind.WATCHING -> Icons.Rounded.PlayCircle; CollectionKind.PLANNED -> Icons.Rounded.Schedule; CollectionKind.WATCHED -> Icons.Rounded.CheckCircle; CollectionKind.FAVORITE -> Icons.Rounded.Bookmark; CollectionKind.POSTPONED -> Icons.Rounded.PauseCircle; CollectionKind.ABANDONED -> Icons.Rounded.Cancel }, null, Modifier.size(21.dp), tint = if (selected) MaterialTheme.colorScheme.primary else TextSecondary); Text(kind.title, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 13.dp)); if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary) } } }; TextButton(onClick = { vm.chooseCollection(id, null) }, modifier = Modifier.align(Alignment.End)) { Text("Убрать из коллекции", fontSize = 12.sp) } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LoginSheet(vm: MainViewModel) {
    val auth by vm.repo.auth.collectAsStateWithLifecycle(); val context = LocalContext.current; val clipboard = LocalClipboardManager.current
    var mode by remember { mutableStateOf("password") }; var login by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    LaunchedEffect(auth.profile) { if (auth.profile != null) vm.onLoggedIn() }
    ModalBottomSheet(onDismissRequest = vm::dismissLogin, containerColor = SurfaceDark, contentColor = TextPrimary, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 34.dp)) {
            Text("С возвращением", style = MaterialTheme.typography.headlineMedium); Text("Твои коллекции и прогресс — всегда с тобой.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("password" to "Пароль", "otp" to "По коду", "vk" to "VK").forEach { (key, title) -> FilterChip(selected = mode == key, onClick = { mode = key; vm.repo.cancelAuth() }, label = { Text(title, fontSize = 12.sp) }) } }
            if (auth.error.isNotBlank()) Surface(color = MaterialTheme.colorScheme.error.copy(alpha = .12f), shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text(auth.error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(12.dp)) }
            when (mode) {
                "password" -> Column(Modifier.padding(top = 16.dp)) { OutlinedTextField(login, { login = it }, label = { Text("Логин или email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !auth.busy); OutlinedTextField(password, { password = it }, label = { Text("Пароль") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth().padding(top = 11.dp), enabled = !auth.busy); Button(onClick = { vm.login(login, password) }, enabled = !auth.busy && login.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 17.dp).height(48.dp), shape = RoundedCornerShape(13.dp)) { if (auth.busy) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text("Войти") }; Text("Пароль отправляется только на aniliberty.top и не сохраняется в приложении.", color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp)) }
                "otp" -> Column(Modifier.padding(top = 16.dp)) {
                    if (auth.otp == null) { Text("Получите одноразовый код и подтвердите вход на официальном сайте AniLiberty в браузере.", color = TextSecondary, fontSize = 12.sp); Button(onClick = vm.repo::startOtp, enabled = !auth.busy, modifier = Modifier.fillMaxWidth().padding(top = 15.dp)) { Text(if (auth.busy) "Получаем код…" else "Получить код") } }
                    else { val otp = auth.otp!!; Surface(color = SurfaceHigh, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Ваш код", color = TextSecondary, fontSize = 11.sp); Text(otp.code, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp, modifier = Modifier.padding(top = 8.dp)); Text("Ожидает подтверждения", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp)) } }; Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedButton(onClick = { clipboard.setText(AnnotatedString(otp.code)); vm.repo.message("Код скопирован") }, modifier = Modifier.weight(1f)) { Text("Копировать") }; Button(onClick = { if (!openExternal(context, OTP_DEVICE_URL)) vm.repo.message("Не удалось открыть браузер") }, modifier = Modifier.weight(1.3f)) { Text("Подтвердить") } }; Text("После подтверждения вернитесь в приложение — вход выполнится автоматически.", color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp)) }
                }
                "vk" -> Column(Modifier.padding(top = 16.dp)) {
                    Text("AniLiberty использует официальный вход VK. Если страница VK ID не открывается или не подтверждает вход, попробуйте пароль или вход по коду.", color = TextSecondary, fontSize = 12.sp)
                    Button(
                        onClick = { vm.repo.startVk { url -> if (!openExternal(context, url)) vm.repo.message("Не удалось открыть браузер") } },
                        enabled = !auth.busy && !auth.socialPending,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text(if (auth.socialPending) "Ожидаем подтверждение…" else "Продолжить через VK") }
                    if (auth.socialPending) {
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 13.dp), color = MaterialTheme.colorScheme.primary, trackColor = SurfaceHigh)
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            OutlinedButton(onClick = { auth.socialUrl?.let { if (!openExternal(context, it)) vm.repo.message("Не удалось открыть браузер") } }, enabled = auth.socialUrl != null, modifier = Modifier.weight(1f)) { Text("Открыть ещё раз", fontSize = 12.sp) }
                            TextButton(onClick = vm.repo::cancelAuth, modifier = Modifier.weight(1f)) { Text("Отменить", fontSize = 12.sp) }
                        }
                        Text("После подтверждения вернитесь в приложение. Если VK не отвечает, это ограничение текущего OAuth AniLiberty.", color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}
