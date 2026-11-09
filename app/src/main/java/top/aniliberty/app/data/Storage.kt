package top.aniliberty.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable data class Settings(
    val accent: String = "red",
    val themeMode: String = "dark",
    val catalogGrid: Boolean = true,
    val libraryGrid: Boolean = true,
    val scheduleGrid: Boolean = true,
    val hideAdult: Boolean = false,
    val skipOpening: Boolean = false,
    val reduceMotion: Boolean = false,
    val notifications: Boolean = false,
    val notifyAll: Boolean = false,
    val favoritesNewestFirst: Boolean = true,
    val tabOrder: List<String> = listOf("watching", "planned", "watched", "postponed", "abandoned"),
    val installedAt: Long = 0L,
    val statTitles: Int = 0,
    val statOpened: Int = 0,
    val statMinutes: Long = 0L
)

@Serializable data class LocalProfile(
    val avatarPath: String? = null,
    val coverPath: String? = null,
    val name: String? = null,
    val about: String = "",
    val frameId: Int = 0,
    val coverStyle: Int = 0
)

@Serializable data class EpisodeUpdate(
    val releaseId: Int,
    val episodeId: String,
    val ordinal: Double,
    val at: Long = System.currentTimeMillis()
)

@Serializable data class NotificationDiagnostics(
    val lastCheckAt: Long = 0,
    val lastSuccessAt: Long = 0,
    val lastError: String = "",
    val lastNotifiedAt: Long = 0,
    val seenEpisodes: Int = 0
)

class LocalStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("aniliberty_settings", Context.MODE_PRIVATE)
    private val json = Network.json
    val settings = MutableStateFlow(runCatching { json.decodeFromString<Settings>(prefs.getString("settings", "{}") ?: "{}") }.getOrDefault(Settings()))
    val localProfile = MutableStateFlow(runCatching { json.decodeFromString<LocalProfile>(prefs.getString("local_profile", "{}") ?: "{}") }.getOrDefault(LocalProfile()))
    val episodeUpdates = MutableStateFlow(readEpisodeUpdates())
    val notificationDiagnostics = MutableStateFlow(readDiagnostics())
    init {
        if (settings.value.installedAt <= 0L) updateSettings { it.copy(installedAt = System.currentTimeMillis()) }
    }
    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(settings.value)
        settings.value = next
        prefs.edit().putString("settings", json.encodeToString(Settings.serializer(), next)).apply()
    }
    fun updateLocalProfile(transform: (LocalProfile) -> LocalProfile) {
        val next = transform(localProfile.value)
        localProfile.value = next
        prefs.edit().putString("local_profile", json.encodeToString(LocalProfile.serializer(), next)).apply()
    }
    fun readLibrary(account: Boolean): LibraryState = runCatching {
        json.decodeFromString<LibraryState>(file(if (account) "account_library.json" else "guest_library.json").readText())
    }.getOrDefault(LibraryState())
    fun saveLibrary(account: Boolean, value: LibraryState) = write(if (account) "account_library.json" else "guest_library.json", json.encodeToString(LibraryState.serializer(), value))
    fun clearAccountLibrary() { file("account_library.json").delete() }
    fun readDetailCache(): Map<String, CachedDetail> = runCatching { json.decodeFromString<Map<String, CachedDetail>>(file("episode_cache.json").readText()) }.getOrDefault(emptyMap())
    fun saveDetailCache(value: Map<String, CachedDetail>) = write("episode_cache.json", json.encodeToString(kotlinx.serialization.serializer<Map<String, CachedDetail>>(), value))
    fun readReleaseCache(): Map<String, Release> = runCatching { json.decodeFromString<Map<String, Release>>(file("release_cache.json").readText()) }.getOrDefault(emptyMap())
    fun saveReleaseCache(value: Map<String, Release>) = write("release_cache.json", json.encodeToString(kotlinx.serialization.serializer<Map<String, Release>>(), value))
    fun episodeBaseline(): Map<String, String> = runCatching { json.decodeFromString<Map<String, String>>(prefs.getString("episode_baseline", "{}") ?: "{}") }.getOrDefault(emptyMap())
    fun saveEpisodeBaseline(value: Map<String, String>) { prefs.edit().putString("episode_baseline", json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), value)).apply() }
    fun notificationTargets(): Set<Int> = prefs.getStringSet("notification_targets", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    fun saveNotificationTargets(ids: Set<Int>) { prefs.edit().putStringSet("notification_targets", ids.map(Int::toString).toSet()).apply() }
    fun bundled(): Snapshot = json.decodeFromString<Snapshot>(context.assets.open("snapshot.json").bufferedReader().use { it.readText() })

    private fun readEpisodeUpdates(): Map<Int, EpisodeUpdate> = runCatching {
        json.decodeFromString<Map<String, EpisodeUpdate>>(prefs.getString("episode_updates", "{}") ?: "{}").mapKeys { it.key.toInt() }
    }.getOrDefault(emptyMap())
    fun saveEpisodeUpdates(value: Map<Int, EpisodeUpdate>) {
        episodeUpdates.value = value
        prefs.edit().putString("episode_updates", json.encodeToString(kotlinx.serialization.serializer<Map<String, EpisodeUpdate>>(), value.mapKeys { it.key.toString() })).apply()
    }
    fun clearEpisodeUpdate(releaseId: Int) { if (episodeUpdates.value.containsKey(releaseId)) saveEpisodeUpdates(episodeUpdates.value - releaseId) }
    private fun readDiagnostics(): NotificationDiagnostics = runCatching {
        json.decodeFromString<NotificationDiagnostics>(prefs.getString("notification_diagnostics", "{}") ?: "{}")
    }.getOrDefault(NotificationDiagnostics())
    fun saveDiagnostics(value: NotificationDiagnostics) {
        notificationDiagnostics.value = value
        prefs.edit().putString("notification_diagnostics", json.encodeToString(NotificationDiagnostics.serializer(), value)).apply()
    }
    private fun file(name: String) = File(context.filesDir, name)
    private fun write(name: String, value: String) {
        val target = file(name); val temp = File(context.filesDir, "$name.tmp")
        temp.writeText(value); if (!temp.renameTo(target)) { target.writeText(value); temp.delete() }
    }
}

/** Only the session token is persisted, encrypted with a non-exportable Android Keystore key. */
class SecureSession(context: Context) {
    private val file = File(context.filesDir, "session.bin")
    private val alias = "aniliberty_session_aes_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    fun read(): String? = runCatching {
        if (!file.exists()) return null
        val raw = file.readBytes(); val iv = raw.copyOfRange(0, 12); val encrypted = raw.copyOfRange(12, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrNull()
    fun write(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key())
        val temp = File(file.parentFile, "session.tmp"); temp.writeBytes(cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8)))
        if (!temp.renameTo(file)) { file.writeBytes(temp.readBytes()); temp.delete() }
    }
    fun clear() { file.delete() }
}
