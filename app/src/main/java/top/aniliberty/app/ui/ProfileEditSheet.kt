package top.aniliberty.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.aniliberty.app.MainViewModel
import top.aniliberty.app.data.Rules
import top.aniliberty.app.data.imageUrl
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val local by vm.repo.store.localProfile.collectAsStateWithLifecycle()
    val library by vm.repo.library.collectAsStateWithLifecycle()
    val auth by vm.repo.auth.collectAsStateWithLifecycle()
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> uri?.let { vm.saveProfileImage(it, cover = false) } }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> uri?.let { vm.saveProfileImage(it, cover = true) } }
    val score = Rules.profileScore(library)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceDark, contentColor = TextPrimary, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 34.dp)) {
            Text("Редактирование профиля", style = MaterialTheme.typography.titleLarge)

            // ---- cover preview: action buttons at the TOP ----
            Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(132.dp).clip(RoundedCornerShape(10.dp))) {
                if (local.coverPath != null) Artwork(Uri.fromFile(File(local.coverPath!!)).toString(), 0, Modifier.fillMaxSize())
                else CoverArt(local.coverStyle.coerceIn(0, 4), Modifier.fillMaxSize())
                Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CoverButton(Icons.Rounded.Image, "Обложка") { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    CoverButton(Icons.Rounded.Restore, "Сбросить") { vm.resetProfileImages() }
                }
            }

            // ---- avatar with pencil badge ----
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarStack(
                    avatarUrl = local.avatarPath?.let { Uri.fromFile(File(it)).toString() } ?: imageUrl(auth.profile?.avatar),
                    xpProgress = Rules.levelProgress(score),
                    levelLabel = null,
                    size = 96.dp,
                    pen = true,
                    onPen = { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Аватар и рамка", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Нажмите карандаш, чтобы выбрать аватар. Всё хранится только на этом устройстве.", color = TextSecondary, fontSize = 10.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // ---- name & about ----
            OutlinedTextField(
                value = local.name.orEmpty(),
                onValueChange = { vm.repo.store.updateLocalProfile { p -> p.copy(name = it.take(40).ifBlank { null }) } },
                label = { Text("Имя") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            OutlinedTextField(
                value = local.about,
                onValueChange = { vm.repo.store.updateLocalProfile { p -> p.copy(about = it.take(120)) } },
                label = { Text("О себе") },
                minLines = 2,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )

            // ---- cover picker ----
            Text("Шапка профиля · 5 вариантов", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(5) { style ->
                    CoverSwatch(style, selected = local.coverPath == null && local.coverStyle == style) {
                        vm.repo.store.updateLocalProfile { it.copy(coverStyle = style, coverPath = null) }
                    }
                }
                item {
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        Modifier.size(width = 76.dp, height = 50.dp).clip(shape).background(SurfaceHigh)
                            .clickable { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Image, "Галерея", Modifier.size(19.dp), tint = TextSecondary) }
                }
            }

            // ---- privacy ----
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF5FD4A8).copy(alpha = .08f), contentColor = TextSecondary, border = BorderStroke(1.dp, Color(0xFF5FD4A8).copy(alpha = .2f)), modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                Row(Modifier.padding(13.dp)) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = Color(0xFF5FD4A8))
                    Column(Modifier.padding(start = 10.dp)) {
                        Text("Приватно и локально", color = Color(0xFFBFF4DF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Свои изображения хранятся в закрытом каталоге приложения: не отправляются в сеть и удаляются вместе с приложением.", fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Rounded.Check, null, Modifier.size(17.dp))
                Text("  Сохранить профиль", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = { vm.resetProfileImages() }, modifier = Modifier.fillMaxWidth().padding(top = 9.dp), shape = RoundedCornerShape(10.dp)) {
                Text("Сбросить изображения", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CoverButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = .42f), contentColor = Color.White, border = BorderStroke(1.dp, Color.White.copy(alpha = .16f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(13.dp))
            Text("  $label", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
