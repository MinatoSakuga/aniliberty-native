package top.aniliberty.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.aniliberty.app.data.*
import top.aniliberty.app.notifications.EpisodeNotifications
import java.io.File

enum class Screen { HOME, CATALOG, LIBRARY, SCHEDULE, PROFILE, SEARCH, SETTINGS, HISTORY }
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = (application as AniLibertyApplication).container.repository
    val repo get() = repository
    private val _screen = MutableStateFlow(Screen.HOME); val screen = _screen.asStateFlow()
    private val _detailId = MutableStateFlow<Int?>(null); val detailId = _detailId.asStateFlow()
    private val _episodeSheet = MutableStateFlow<Int?>(null); val episodeSheet = _episodeSheet.asStateFlow()
    private val _collectionMenu = MutableStateFlow<Int?>(null); val collectionMenu = _collectionMenu.asStateFlow()
    private val _loginVisible = MutableStateFlow(false); val loginVisible = _loginVisible.asStateFlow()
    private val _filterVisible = MutableStateFlow(false); val filterVisible = _filterVisible.asStateFlow()
    private val _player = MutableStateFlow<PlayerRequest?>(null); val player = _player.asStateFlow()
    private val _playerLoading = MutableStateFlow(false); val playerLoading = _playerLoading.asStateFlow()
    private val _libraryTab = MutableStateFlow("favorite"); val libraryTab = _libraryTab.asStateFlow()
    private val _scheduleDay = MutableStateFlow(java.time.LocalDate.now().dayOfWeek.value - 1); val scheduleDay = _scheduleDay.asStateFlow()
    private var playerJob: Job? = null
    private var lastCollectionTab = "watching"
    val canBack get() = _detailId.value != null || _screen.value !in listOf(Screen.HOME, Screen.CATALOG, Screen.LIBRARY, Screen.SCHEDULE, Screen.PROFILE)
    init { repository.start() }
    fun navigate(screen: Screen) {
        _detailId.value = null
        if (screen == Screen.LIBRARY) {
            // Bottom-bar «Избранное»: a second tap while already on the favorites tab flips the order.
            if (_screen.value == Screen.LIBRARY && _libraryTab.value == "favorite") toggleFavoriteDirection()
            else _libraryTab.value = "favorite"
        }
        _screen.value = screen
        if (screen == Screen.CATALOG && repo.catalog.value.ids.isEmpty()) repo.loadCatalog()
    }
    fun openDetail(id: Int) { _episodeSheet.value = null; _detailId.value = id; viewModelScope.launch { runCatching { repo.ensureDetail(id) }.onFailure { repo.message("Показаны сохранённые серии. Проверьте интернет для обновления.") } } }
    fun closeDetail() { _detailId.value = null }
    fun back() { when { _detailId.value != null -> _detailId.value = null; _screen.value == Screen.SETTINGS || _screen.value == Screen.HISTORY -> _screen.value = Screen.PROFILE; _screen.value == Screen.SEARCH -> _screen.value = Screen.HOME; else -> _screen.value = Screen.HOME } }
    fun openEpisodes(id: Int) { _episodeSheet.value = id; repo.clearEpisodeUpdate(id); viewModelScope.launch { runCatching { repo.ensureDetail(id) } } }
    fun dismissEpisodes() { _episodeSheet.value = null }
    fun openCollection(id: Int) { _collectionMenu.value = id }
    fun dismissCollection() { _collectionMenu.value = null }
    fun chooseCollection(id: Int, kind: CollectionKind?) { repo.setCollection(id, kind); _collectionMenu.value = null }
    fun showFilters() { _filterVisible.value = true }
    fun dismissFilters() { _filterVisible.value = false }
    fun applyFilter(filter: CatalogFilter) { _filterVisible.value = false; repo.loadCatalog(true, filter) }
    fun selectLibraryTab(key: String) {
        if (key == "favorite" && _libraryTab.value == "favorite") { toggleFavoriteDirection(); return }
        _libraryTab.value = key
        if (key != "favorite") lastCollectionTab = key
    }
    fun toggleFavoriteDirection() {
        val next = !repo.settings.value.favoritesNewestFirst
        repo.store.updateSettings { it.copy(favoritesNewestFirst = next) }
        repo.message(if (next) "Избранное: сначала последние добавленные" else "Избранное: сначала первые добавленные")
    }
    fun toggleScheduleView() {
        repo.store.updateSettings { it.copy(scheduleGrid = !it.scheduleGrid) }
    }
    fun saveProfileImage(uri: Uri, cover: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val app = getApplication<Application>()
                val file = File(app.filesDir, if (cover) "profile_cover.local" else "profile_avatar.local")
                app.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: error("stream")
                repo.store.updateLocalProfile { if (cover) it.copy(coverPath = file.absolutePath) else it.copy(avatarPath = file.absolutePath) }
                repo.message(if (cover) "Обложка сохранена на этом устройстве" else "Аватар сохранён на этом устройстве")
            }.onFailure { repo.message("Не удалось загрузить изображение") }
        }
    }
    fun resetProfileImages() {
        val app = getApplication<Application>()
        File(app.filesDir, "profile_cover.local").delete(); File(app.filesDir, "profile_avatar.local").delete()
        repo.store.updateLocalProfile { LocalProfile() }
        repo.message("Локальные изображения профиля сброшены")
    }
    fun selectDay(day: Int) { _scheduleDay.value = day }
    fun openLogin() { _loginVisible.value = true }
    fun dismissLogin() { repo.cancelAuth(); _loginVisible.value = false }
    fun onLoggedIn() { _loginVisible.value = false; navigate(Screen.PROFILE) }
    fun login(login: String, password: String) = viewModelScope.launch { runCatching { repo.login(login, password) }.onSuccess { onLoggedIn() } }
    fun openPlayer(id: Int, ordinal: Double) {
        playerJob?.cancel(); _playerLoading.value = true
        playerJob = viewModelScope.launch {
            try { _player.value = repo.playable(id, ordinal); _episodeSheet.value = null }
            catch (e: Exception) { repo.message(repo.errorMessage(e)) }
            finally { _playerLoading.value = false }
        }
    }
    fun continueRelease(id: Int) {
        repo.clearEpisodeUpdate(id)
        val history = repo.library.value.history.values.filter { it.releaseId == id }.maxByOrNull { it.updatedAt }
        val episodes = repo.episodes(id)
        val ordinal = history?.ordinal ?: episodes.firstOrNull()?.ordinal
        if (ordinal != null) openPlayer(id, ordinal)
        else viewModelScope.launch { runCatching { repo.ensureDetail(id, true) }.onSuccess { detail -> Rules.releasedEpisodes(detail).firstOrNull()?.let { openPlayer(id, it.ordinal) } }.onFailure { repo.message("Не удалось загрузить серии") } }
    }
    fun closePlayer() { playerJob?.cancel(); _player.value = null; viewModelScope.launch { repo.flushProgress() } }
    fun stepEpisode(direction: Int) {
        val current = _player.value ?: return; val episodes = repo.episodes(current.releaseId); val index = episodes.indexOfFirst { it.ordinal == current.episode.ordinal }; val next = episodes.getOrNull(index + direction) ?: return
        openPlayer(current.releaseId, next.ordinal)
    }
    fun notificationsEnabled(enabled: Boolean) {
        repo.store.updateSettings { it.copy(notifications = enabled) }
        if (enabled) {
            EpisodeNotifications.schedule(getApplication())
            EpisodeNotifications.checkNow(getApplication())
        } else EpisodeNotifications.cancel(getApplication())
    }
    fun checkEpisodesNow() { EpisodeNotifications.checkNow(getApplication()); repo.message("Проверяем новые серии…") }
    fun testNotification() {
        val ok = EpisodeNotifications.test(getApplication())
        repo.message(if (ok) "Тестовое уведомление отправлено" else "Уведомления запрещены. Откройте настройки Android.")
        if (!ok) EpisodeNotifications.openSystemSettings(getApplication())
    }
    fun openReleaseExtra(id: Int?) { if (id != null && id > 0) openDetail(id) }
    override fun onCleared() { viewModelScope.launch { repo.flushProgress() }; super.onCleared() }
}
