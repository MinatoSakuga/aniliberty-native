package top.aniliberty.app.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonArray
import retrofit2.HttpException
import top.aniliberty.app.notifications.EpisodeNotifications
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
data class CatalogFilter(val genres: Set<Int> = emptySet(), val type: String = "", val from: String = "", val to: String = "", val ongoing: Boolean = false, val sort: String = "FRESH_AT_DESC") {
    fun query(page: Int, hideAdult: Boolean): Map<String, String> = buildMap {
        put("page", page.toString()); put("limit", "24"); put("f[sorting]", sort)
        if (genres.isNotEmpty()) put("f[genres]", genres.joinToString(","))
        if (type.isNotBlank()) put("f[types]", type)
        if (from.isNotBlank()) put("f[years][from_year]", from)
        if (to.isNotBlank()) put("f[years][to_year]", to)
        if (ongoing) put("f[publish_statuses]", "IS_ONGOING")
        if (hideAdult) put("f[age_ratings]", "R0_PLUS,R6_PLUS,R12_PLUS,R16_PLUS")
    }
}
data class HomeState(val latest: List<Int>, val popular: List<Int>, val hero: List<Int>, val schedule: List<ScheduleItem>, val genres: List<Genre>, val loading: Boolean = true, val offline: Boolean = false)
data class CatalogState(val ids: List<Int> = emptyList(), val total: Int = 0, val page: Int = 1, val pages: Int = 1, val loading: Boolean = false, val offline: Boolean = false, val filter: CatalogFilter = CatalogFilter())
data class SearchState(val query: String = "", val ids: List<Int> = emptyList(), val loading: Boolean = false, val remote: Boolean = false)
data class AuthState(val profile: Profile? = null, val busy: Boolean = false, val error: String = "", val otp: OtpUi? = null, val socialPending: Boolean = false, val socialUrl: String? = null)
data class OtpUi(val code: String, val deviceId: String, val expiresAt: Long)
data class SyncState(val running: Boolean = false, val failed: Set<String> = emptySet(), val lastSuccess: Long = 0) {
    val label: String get() = when { running -> "Обновляем…"; failed.size == 3 -> "Нет связи. Повторим автоматически."; failed.isNotEmpty() -> "Догружаем часть данных…"; lastSuccess > 0 -> "Обновлено автоматически"; else -> "Автосинхронизация включена" }
}
data class PlayerRequest(val releaseId: Int, val episode: Episode, val resumeMs: Long = 0)

class Repository(val context: Context, val api: AniLibertyApi, val store: LocalStore) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = SecureSession(context)
    private val snapshot = store.bundled()
    private val libraryMutex = Mutex()
    private val cacheMutex = Mutex()
    private val detailCache = store.readDetailCache().toMutableMap()
    private val detailJobs = ConcurrentHashMap<Int, Deferred<Release>>()
    private val prefetchGate = kotlinx.coroutines.sync.Semaphore(3)
    private var cacheSaveJob: Job? = null
    private var epoch = 0
    private var started = false
    private var catalogVersion = 0
    private var searchJob: Job? = null
    private var syncJob: Job? = null
    private var retryJob: Job? = null
    private var progressJob: Job? = null
    private var otpJob: Job? = null
    private var socialJob: Job? = null
    private val progressQueue = ConcurrentHashMap<String, TimecodeRequest>()
    @Volatile private var token: String? = null
    private val _library = MutableStateFlow(store.readLibrary(false))
    val library = _library.asStateFlow()
    private val _releases = MutableStateFlow<Map<Int, Release>>(snapshot.items.mapKeys { it.key.toInt() }.toMutableMap().also { map -> store.readReleaseCache().forEach { (k, v) -> runCatching { map[k.toInt()] = v } } })
    val releases = _releases.asStateFlow()
    private val _details = MutableStateFlow<Map<Int, Release>>(snapshot.details.mapKeys { it.key.toInt() }.toMutableMap().also { map -> detailCache.forEach { (k, v) -> runCatching { map[k.toInt()] = v.release } } })
    val details = _details.asStateFlow()
    private val _home = MutableStateFlow(HomeState(snapshot.latest.map(String::toInt), snapshot.popular.map(String::toInt), snapshot.hero.map(String::toInt), snapshot.schedule.mapNotNull { s -> snapshot.items[s.releaseId]?.let { ScheduleItem(it, s.complete, s.next) } }, snapshot.genres))
    val home = _home.asStateFlow()
    private val _catalog = MutableStateFlow(CatalogState()); val catalog = _catalog.asStateFlow()
    private val _search = MutableStateFlow(SearchState()); val search = _search.asStateFlow()
    private val _auth = MutableStateFlow(AuthState()); val auth = _auth.asStateFlow()
    private val _sync = MutableStateFlow(SyncState()); val sync = _sync.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8); val messages = _messages.asSharedFlow()
    val settings = store.settings
    val loggedIn get() = token != null && _auth.value.profile != null
    val authorization get() = token?.let { "Bearer $it" }
    fun release(id: Int): Release? = _releases.value[id]
    fun episodes(id: Int): List<Episode> = Rules.releasedEpisodes(_details.value[id])
    fun entry(id: Int, ordinal: Double) = _library.value.history["$id:$ordinal"]
    fun isWatched(id: Int, ordinal: Double) = Rules.watched(entry(id, ordinal))
    fun isSeen(id: Int, ordinal: Double) = Rules.seen(entry(id, ordinal))
    fun message(text: String) { _messages.tryEmit(text) }

    fun start() {
        if (started) return; started = true
        refreshHome(); restoreSession()
        if (settings.value.notifications) { EpisodeNotifications.schedule(context); EpisodeNotifications.checkNow(context) }
        scope.launch { checkEpisodeUpdates(notify = false) }
        scope.launch { while (isActive) { delay(90_000); if (loggedIn) syncAccount() } }
    }
    fun refreshHome() = scope.launch {
        _home.update { it.copy(loading = true) }
        val latest = async { runCatching { api.latest() }.getOrNull() }
        val schedule = async { runCatching { api.schedule().data }.getOrNull() }
        val genres = async { runCatching { api.genres() }.getOrNull() }
        val recommended = async { runCatching { api.recommended() }.getOrNull() }
        val fresh = latest.await(); val week = schedule.await(); val genreList = genres.await(); val heroList = recommended.await()
        if (fresh != null) mergeReleases(fresh)
        if (week != null) mergeReleases(week.map { it.release })
        if (heroList != null) mergeReleases(heroList)
        _home.update { it.copy(latest = fresh?.map { r -> r.id } ?: it.latest, hero = heroList?.take(8)?.map { r -> r.id }?.ifEmpty { it.hero } ?: it.hero, schedule = week ?: it.schedule, genres = genreList ?: it.genres, loading = false, offline = fresh == null && week == null) }
        prefetch(_home.value.latest.take(12)); prefetch(_home.value.hero)
    }
    fun mergeReleases(list: List<Release>) {
        if (list.isEmpty()) return
        _releases.update { old -> old.toMutableMap().apply { list.forEach { incoming ->
            val previous = this[incoming.id]
            this[incoming.id] = incoming.copy(genres = incoming.genres.ifEmpty { previous?.genres.orEmpty() }, poster = incoming.poster ?: previous?.poster, description = incoming.description ?: previous?.description, latestEpisode = incoming.latestEpisode ?: previous?.latestEpisode, members = incoming.members.ifEmpty { previous?.members.orEmpty() }, episodes = null)
        } } }
        scheduleCacheSave()
    }
    private fun mergeDetail(release: Release) {
        if (release.episodes == null) return
        _details.update { it + (release.id to release.copy(episodes = release.episodes.sortedBy(Episode::ordinal))) }
        mergeReleases(listOf(release))
    }
    private fun scheduleCacheSave() {
        cacheSaveJob?.cancel()
        cacheSaveJob = scope.launch { delay(700); cacheMutex.withLock {
            store.saveReleaseCache(_releases.value.mapKeys { it.key.toString() })
            store.saveDetailCache(detailCache.toMap())
        } }
    }
    suspend fun ensureDetail(id: Int, force: Boolean = false): Release {
        val cached = _details.value[id]
        val fetched = detailCache[id.toString()]
        val maxAge = if (release(id)?.ongoing == true) 15 * 60_000L else 6 * 3_600_000L
        if (!force && cached?.episodes != null && fetched != null && System.currentTimeMillis() - fetched.at < maxAge) return cached
        if (!force && cached?.episodes != null && fetched == null && release(id)?.ongoing != true) return cached
        detailJobs[id]?.let { return it.await() }
        val job = scope.async {
            val ref = release(id)?.alias ?: id.toString()
            val result = api.release(ref)
            if (result.episodes == null) throw IOException("API не вернул полный список серий")
            mergeDetail(result)
            cacheMutex.withLock { detailCache[id.toString()] = CachedDetail(_details.value.getValue(id), System.currentTimeMillis()) }
            scheduleCacheSave(); _details.value.getValue(id)
        }
        detailJobs[id] = job
        return try { job.await() } finally { detailJobs.remove(id) }
    }
    fun prefetch(ids: List<Int>) {
        ids.distinct().take(60).forEach { id ->
            if (detailJobs.containsKey(id)) return@forEach
            val fresh = detailCache[id.toString()]?.at ?: 0
            val bundled = snapshot.details.containsKey(id.toString())
            if (bundled && release(id)?.ongoing != true) return@forEach
            if (System.currentTimeMillis() - fresh < 15 * 60_000) return@forEach
            scope.launch { prefetchGate.withPermit { runCatching { ensureDetail(id) } } }
        }
    }
    fun localCatalog(filter: CatalogFilter): List<Release> = _releases.value.values.filter { r ->
        Rules.visible(r, settings.value.hideAdult) && (filter.genres.isEmpty() || r.genres.any { it.id in filter.genres }) && (filter.type.isBlank() || r.type?.value == filter.type) && (filter.from.toIntOrNull()?.let { (r.year ?: 0) >= it } != false) && (filter.to.toIntOrNull()?.let { (r.year ?: 0) <= it } != false) && (!filter.ongoing || r.ongoing)
    }.sortedWith(when (filter.sort) { "YEAR_DESC" -> compareByDescending { it.year }; "YEAR_ASC" -> compareBy { it.year }; "RATING_DESC" -> compareByDescending { it.favoritesCount ?: 0 }; else -> compareByDescending { it.freshAt } })
    fun loadCatalog(reset: Boolean = true, filter: CatalogFilter = _catalog.value.filter) {
        val version = if (reset) ++catalogVersion else catalogVersion
        val page = if (reset) 1 else _catalog.value.page + 1
        _catalog.update { it.copy(loading = true, filter = filter, ids = if (reset) emptyList() else it.ids) }
        scope.launch {
            try {
                val result = api.catalog(filter.query(page, settings.value.hideAdult))
                if (version != catalogVersion) return@launch
                mergeReleases(result.data)
                _catalog.update { it.copy(ids = (it.ids + result.data.map { r -> r.id }).distinct(), total = result.meta.pagination.total, page = page, pages = result.meta.pagination.totalPages, loading = false, offline = false) }
                prefetch(result.data.map { it.id })
            } catch (e: Exception) {
                if (version != catalogVersion) return@launch
                val local = localCatalog(filter).map { it.id }
                _catalog.update { it.copy(ids = local.take(page * 24), total = local.size, page = page, pages = (local.size + 23) / 24, loading = false, offline = true) }
            }
        }
    }
    fun search(query: String) {
        searchJob?.cancel()
        val local = if (query.isBlank()) emptyList() else _releases.value.values.filter { Rules.matches(it, query) }.sortedByDescending { it.favoritesCount ?: 0 }.map { it.id }
        _search.value = SearchState(query, local, query.trim().length >= 2, false)
        if (query.trim().length < 2) return
        searchJob = scope.launch {
            delay(350)
            try { val result = api.search(query.trim()); mergeReleases(result); _search.update { it.copy(ids = result.map { r -> r.id }, loading = false, remote = true) }; prefetch(result.map { it.id }) }
            catch (e: Exception) { _search.update { it.copy(loading = false, remote = true) } }
        }
    }
    fun rememberSearch() { val q = _search.value.query.trim(); if (q.length < 2) return; updateLibrary { it.copy(searches = (listOf(q) + it.searches.filterNot { s -> s == q }).take(6)) } }
    fun clearSearch() { searchJob?.cancel(); _search.value = SearchState() }
    private fun updateLibrary(transform: (LibraryState) -> LibraryState) {
        _library.update(transform); store.saveLibrary(loggedIn, _library.value); updateNotificationTargets()
    }
    private fun updateNotificationTargets() {
        val l = _library.value
        store.saveNotificationTargets(l.collections.values.flatten().toSet() + l.reminders)
    }
    fun toggleReminder(id: Int) { updateLibrary { it.copy(reminders = if (id in it.reminders) it.reminders - id else it.reminders + id) } }
    fun toggleFavorite(id: Int) {
        val before = _library.value; val current = before.collections["favorite"].orEmpty(); val add = id !in current
        updateLibrary { val next = if (add) listOf(id) + current.filterNot { it == id } else current - id; it.copy(collections = it.collections + ("favorite" to next), favoriteOrder = next) }
        if (!loggedIn) { message(if (add) "Добавлено в избранное" else "Убрано из избранного"); return }
        scope.launch {
            try { val auth = authorization ?: return@launch; val ids = if (add) api.addFavorite(auth, listOf(ReleaseId(id))) else api.removeFavorite(auth, listOf(ReleaseId(id))); libraryMutex.withLock { updateLibrary { val ordered = Rules.orderFavorites(it.favoriteOrder.ifEmpty { it.collections["favorite"].orEmpty() }, ids); it.copy(collections = it.collections + ("favorite" to ordered), favoriteOrder = ordered) } } }
            catch (e: Exception) { if (loggedIn) { _library.value = before; store.saveLibrary(true, before); message("Не удалось сохранить избранное. Повторим при синхронизации.") }; syncAccount(setOf("favorites")) }
        }
    }
    fun setCollection(id: Int, kind: CollectionKind?) {
        if (kind == CollectionKind.FAVORITE) { toggleFavorite(id); return }
        val before = _library.value
        updateLibrary { it.copy(collections = it.collections.mapValues { (key, ids) -> if (key == "favorite") ids else ids.filterNot { value -> value == id } }.let { map -> if (kind == null) map else map + (kind.key to listOf(id) + map[kind.key].orEmpty()) }) }
        if (!loggedIn) return
        scope.launch {
            try {
                val auth = authorization ?: return@launch
                val rows = if (kind == null) api.removeCollection(auth, listOf(ReleaseId(id))) else api.addCollection(auth, listOf(CollectionChange(id, kind.api)))
                applyCollections(rows)
            } catch (e: Exception) { if (loggedIn) { _library.value = before; store.saveLibrary(true, before) }; message("Изменение коллекции не сохранилось"); syncAccount(setOf("collections")) }
        }
    }
    private suspend fun applyCollections(rows: JsonArray) {
        val parsed = Rules.parseCollections(rows)
        libraryMutex.withLock { updateLibrary { it.copy(collections = it.collections + parsed) } }
    }
    fun saveProgress(releaseId: Int, episode: Episode, positionMs: Long, durationMs: Long, watched: Boolean = false) {
        if (positionMs < 1000 && !watched) return
        val duration = if (durationMs > 0) durationMs / 1000.0 else episode.duration ?: 0.0
        val isWatched = watched || (duration > 0 && positionMs / 1000.0 / duration >= .95)
        val value = WatchEntry(releaseId, episode.ordinal, episode.id, positionMs / 1000.0, duration, isWatched, opened = true)
        updateLibrary { it.copy(history = it.history + (value.key to value)) }
        if (loggedIn && episode.id.isNotBlank()) {
            progressQueue[episode.id] = TimecodeRequest((positionMs / 1000).coerceAtLeast(0), isWatched, episode.id)
            progressJob?.cancel(); progressJob = scope.launch { delay(8000); flushProgress() }
        }
    }
    fun markOpened(releaseId: Int, episode: Episode, positionMs: Long = 0, durationMs: Long = 0) {
        val previous = entry(releaseId, episode.ordinal)
        val duration = if (durationMs > 0) durationMs / 1000.0 else episode.duration ?: previous?.duration ?: 0.0
        val value = WatchEntry(releaseId, episode.ordinal, episode.id, (positionMs / 1000.0).coerceAtLeast(previous?.time ?: 0.0), duration, previous?.watched ?: false, opened = true)
        updateLibrary { it.copy(history = it.history + (value.key to value)) }
    }
    fun markWatched(releaseId: Int, episode: Episode) {
        val current = isWatched(releaseId, episode.ordinal); val duration = episode.duration ?: entry(releaseId, episode.ordinal)?.duration ?: 0.0
        val value = WatchEntry(releaseId, episode.ordinal, episode.id, if (current) 0.0 else duration, duration, !current, opened = !current)
        updateLibrary { it.copy(history = it.history + (value.key to value)) }
        if (loggedIn) { progressQueue[episode.id] = TimecodeRequest(value.time.toLong(), !current, episode.id); scope.launch { flushProgress() } }
        message(if (current) "Отметка просмотра снята" else "Серия отмечена просмотренной")
    }
    suspend fun flushProgress() {
        val auth = authorization ?: return
        if (progressQueue.isEmpty()) return
        val batch = progressQueue.values.take(1000)
        try { api.saveTimecodes(auth, batch); batch.forEach { progressQueue.remove(it.episodeId) } }
        catch (e: Exception) { retryJob?.cancel(); retryJob = scope.launch { delay(30_000); flushProgress() } }
    }
    fun clearGuestData(historyOnly: Boolean) {
        if (loggedIn) return
        updateLibrary { if (historyOnly) it.copy(history = emptyMap()) else LibraryState(searches = it.searches) }
        progressQueue.clear()
    }

    private suspend fun <T> retry(block: suspend () -> T): T {
        var last: Exception? = null
        repeat(3) { attempt -> try { return block() } catch (e: Exception) { last = e; if (e is HttpException && e.code() in listOf(401, 403)) throw e; if (attempt < 2) delay(700L * (attempt + 1)) } }
        throw last ?: IOException("Сеть недоступна")
    }
    fun syncAccount(sections: Set<String> = setOf("favorites", "collections", "history")) {
        if (!loggedIn || _sync.value.running) return
        retryJob?.cancel(); val currentEpoch = epoch
        syncJob = scope.launch {
            _sync.update { it.copy(running = true) }
            val failed = java.util.Collections.synchronizedSet(mutableSetOf<String>()); val ids = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
            val jobs = mutableListOf<Deferred<Unit>>()
            if ("favorites" in sections) jobs.add(async { try {
                val auth = authorization!!
                val first = retry { api.favoriteReleases(auth) }
                val ordered = first.data.map { it.id }.toMutableList()
                for (page in 2..first.meta.pagination.totalPages.coerceAtMost(50)) ordered += retry { api.favoriteReleases(auth, page) }.data.map { it.id }
                mergeReleases(first.data)
                libraryMutex.withLock { updateLibrary { val result = Rules.orderFavorites(it.favoriteOrder.ifEmpty { it.collections["favorite"].orEmpty() }, ordered); it.copy(collections = it.collections + ("favorite" to result), favoriteOrder = result) } }
                ids += ordered
            } catch (e: Exception) {
                try { val result = retry { api.favoriteIds(authorization!!) }; libraryMutex.withLock { updateLibrary { val ordered = Rules.orderFavorites(it.favoriteOrder.ifEmpty { it.collections["favorite"].orEmpty() }, result); it.copy(collections = it.collections + ("favorite" to ordered), favoriteOrder = ordered) } }; ids += result } catch (fallback: Exception) { failed += "favorites" }
            } })
            if ("collections" in sections) jobs.add(async { try { val result = retry { api.collectionIds(authorization!!) }; applyCollections(result); ids += Rules.parseCollections(result).values.flatten() } catch (e: Exception) { failed += "collections" } })
            if ("history" in sections) jobs.add(async { try {
                val auth = authorization!!
                val first = retry { api.history(auth) }
                val rows = first.data.toMutableList()
                for (page in 2..first.meta.pagination.totalPages.coerceAtMost(50)) rows += retry { api.history(auth, page) }.data
                val entries = rows.mapNotNull { row -> val ep = row.episode ?: return@mapNotNull null; val rid = ep.release?.id ?: ep.releaseId ?: return@mapNotNull null; ep.release?.let { mergeReleases(listOf(it)) }; val key = "$rid:${ep.ordinal}"; key to WatchEntry(rid, ep.ordinal, ep.id, row.time, ep.duration ?: 0.0, row.watched, opened = row.time > 0 || row.watched) }.toMap()
                libraryMutex.withLock { updateLibrary { it.copy(history = entries) } }; ids += entries.values.map { it.releaseId }
            } catch (e: Exception) { failed += "history" } })
            jobs.awaitAll()
            if (currentEpoch != epoch || !loggedIn) return@launch
            hydrateReleases(ids.toList())
            _sync.update { it.copy(running = false, failed = failed, lastSuccess = if (failed.isEmpty()) System.currentTimeMillis() else it.lastSuccess) }
            if (failed.isNotEmpty()) { retryJob = scope.launch { delay(15_000); syncAccount(failed.toSet()) } }
        }
    }
    private suspend fun hydrateReleases(ids: List<Int>) {
        val missing = ids.distinct().filter { !_releases.value.containsKey(it) }.take(500)
        missing.chunked(50).forEach { chunk -> runCatching { mergeReleases(api.releases(chunk.joinToString(",")).data) } }
        prefetch(ids)
    }
    private fun restoreSession() {
        val saved = session.read() ?: return
        scope.launch { try { val profile = api.profile("Bearer $saved"); token = saved; epoch++; _auth.value = AuthState(profile); _library.value = store.readLibrary(true); syncAccount() } catch (e: Exception) { if (e is HttpException && e.code() in listOf(401, 403)) { session.clear(); token = null }; _library.value = store.readLibrary(false) } }
    }
    suspend fun login(login: String, password: String) {
        cancelAuth(); _auth.value = AuthState(busy = true)
        try { val result = api.login(LoginRequest(login.trim(), password)); establish(result.token) }
        catch (e: Exception) { _auth.value = AuthState(error = errorMessage(e)); throw e }
    }
    private suspend fun establish(value: String) {
        if (value.isBlank()) throw IOException("API не вернул токен")
        val profile = api.profile("Bearer $value")
        token = value; session.write(value); epoch++; progressQueue.clear(); store.clearAccountLibrary(); _library.value = LibraryState(); _auth.value = AuthState(profile); syncAccount()
    }
    fun logout() = scope.launch {
        cancelAuth(); flushProgress(); val old = authorization
        if (old != null) runCatching { api.logout(old) }
        epoch++; token = null; session.clear(); progressQueue.clear(); store.clearAccountLibrary(); _auth.value = AuthState(); _sync.value = SyncState(); _library.value = store.readLibrary(false); updateNotificationTargets(); message("Вы вышли из аккаунта")
    }
    fun startOtp() {
        cancelAuth(); _auth.value = AuthState(busy = true)
        scope.launch {
            try {
                val id = UUID.randomUUID().toString(); val response = api.otpStart(OtpStartRequest(id)); val otp = OtpUi(response.otp.code, id, System.currentTimeMillis() + response.remaining * 1000L)
                _auth.value = AuthState(otp = otp)
                otpJob = scope.launch {
                    while (isActive && System.currentTimeMillis() < otp.expiresAt) {
                        delay(2800)
                        try { val result = api.otpLogin(OtpLoginRequest(otp.code.toInt(), id)); establish(result.token); return@launch }
                        catch (e: HttpException) { if (e.code() !in listOf(401, 404, 429)) _auth.update { it.copy(error = "Ожидаем подтверждение…") }; if (e.code() == 429) delay(7000) }
                        catch (e: Exception) { delay(4000) }
                    }
                    _auth.update { it.copy(otp = null, error = "Код истёк. Получите новый.") }
                }
            } catch (e: Exception) { _auth.value = AuthState(error = errorMessage(e)) }
        }
    }
    fun startVk(onUrl: (String) -> Unit) {
        cancelAuth(); _auth.value = AuthState(busy = true)
        scope.launch {
            try {
                val response = api.socialLogin(); _auth.value = AuthState(socialPending = true, socialUrl = response.url); onUrl(response.url)
                val deadline = System.currentTimeMillis() + 300_000
                socialJob = scope.launch {
                    while (isActive && System.currentTimeMillis() < deadline) {
                        delay(2600)
                        try { val result = api.socialAuthenticate(response.state); establish(result.token); return@launch }
                        catch (e: HttpException) { if (e.code() == 429) delay(8000) }
                        catch (e: Exception) { delay(3000) }
                    }
                    _auth.value = AuthState(error = "VK не подтвердил вход за 5 минут. Попробуйте снова или войдите по паролю/коду.")
                }
            } catch (e: Exception) { _auth.value = AuthState(error = if (e is HttpException && e.code() >= 500) "VK вход сейчас не отвечает на стороне AniLiberty. Попробуйте пароль или вход по коду." else errorMessage(e)) }
        }
    }
    fun cancelAuth() { otpJob?.cancel(); socialJob?.cancel(); if (!loggedIn) _auth.value = AuthState() }
    suspend fun playable(id: Int, ordinal: Double): PlayerRequest {
        val detail = ensureDetail(id); val episode = Rules.releasedEpisodes(detail).firstOrNull { it.ordinal == ordinal } ?: throw IOException("Серия не найдена среди вышедших")
        val auth = authorization
        val full = if (auth != null) runCatching { val authorized = api.episode(episode.id, auth); episode.copy(hls480 = authorized.hls480 ?: episode.hls480, hls720 = authorized.hls720 ?: episode.hls720, hls1080 = authorized.hls1080 ?: episode.hls1080) }.getOrDefault(episode) else episode
        if (full.url(720) == null) throw IOException("Видео этой серии пока недоступно")
        val progress = entry(id, ordinal)
        return PlayerRequest(id, full, if (progress != null && !Rules.watched(progress)) (progress.time * 1000).toLong() else 0)
    }
    suspend fun checkEpisodeUpdates(notify: Boolean): Int {
        val settings = store.settings.value
        store.saveDiagnostics(store.notificationDiagnostics.value.copy(lastCheckAt = System.currentTimeMillis()))
        return try {
            val latest = runCatching { api.latest(50) }.getOrNull().orEmpty()
            val schedule = runCatching { api.schedule().data.map { it.release.copy(latestEpisode = it.published ?: it.release.latestEpisode) } }.getOrNull().orEmpty()
            val targets = store.notificationTargets()
            val covered = (latest + schedule).map { it.id }.toSet()
            val extra = targets.filter { it !in covered }.take(20).mapNotNull { id -> runCatching { api.release(id.toString()) }.getOrNull() }
            val all = (latest + schedule + extra).distinctBy { it.id }
            if (all.isEmpty()) throw IOException("Нет данных о сериях")
            mergeReleases(all)
            val current = all.mapNotNull { r -> EpisodeRules.fingerprint(r)?.let { r.id to it } }.toMap()
            val previous = readEpisodeFingerprints()
            val changed = EpisodeRules.changed(previous, current)
            if (previous.isNotEmpty()) {
                val updates = store.episodeUpdates.value.toMutableMap()
                var notified = 0
                changed.forEach { id ->
                    val fp = current[id] ?: return@forEach
                    val release = all.firstOrNull { it.id == id } ?: return@forEach
                    val relevant = settings.notifyAll || id in targets
                    if (relevant) updates[id] = EpisodeUpdate(id, fp.episodeId, fp.ordinal)
                    if (notify && relevant && settings.notifications && EpisodeNotifications.allowed(context) && (!settings.hideAdult || !release.ageRating.adult)) {
                        val number = if (fp.ordinal % 1.0 == 0.0) fp.ordinal.toInt().toString() else fp.ordinal.toString()
                        if (EpisodeNotifications.show(context, id, release.name.main, "Вышла $number серия. Уже можно смотреть.", id)) notified++
                    }
                }
                if (updates != store.episodeUpdates.value) store.saveEpisodeUpdates(updates)
                saveEpisodeFingerprints(previous + current)
                store.saveDiagnostics(store.notificationDiagnostics.value.copy(lastSuccessAt = System.currentTimeMillis(), lastError = "", seenEpisodes = current.size, lastNotifiedAt = if (notified > 0) System.currentTimeMillis() else store.notificationDiagnostics.value.lastNotifiedAt))
                changed.size
            } else {
                saveEpisodeFingerprints(current)
                store.saveDiagnostics(store.notificationDiagnostics.value.copy(lastSuccessAt = System.currentTimeMillis(), lastError = "", seenEpisodes = current.size))
                0
            }
        } catch (e: Exception) {
            store.saveDiagnostics(store.notificationDiagnostics.value.copy(lastError = errorMessage(e)))
            -1
        }
    }
    fun clearEpisodeUpdate(id: Int) { store.clearEpisodeUpdate(id) }
    private fun readEpisodeFingerprints(): Map<Int, EpisodeFingerprint> = runCatching {
        Network.json.decodeFromString<Map<String, EpisodeFingerprint>>(context.getSharedPreferences("aniliberty_settings", Context.MODE_PRIVATE).getString("episode_fingerprints", "{}") ?: "{}").mapKeys { it.key.toInt() }
    }.getOrDefault(emptyMap())
    private fun saveEpisodeFingerprints(value: Map<Int, EpisodeFingerprint>) {
        context.getSharedPreferences("aniliberty_settings", Context.MODE_PRIVATE).edit().putString("episode_fingerprints", Network.json.encodeToString(kotlinx.serialization.serializer<Map<String, EpisodeFingerprint>>(), value.mapKeys { it.key.toString() })).apply()
    }
    fun errorMessage(e: Throwable): String = when {
        e is HttpException && e.code() == 401 -> "Неверный логин или пароль"
        e is HttpException && e.code() == 429 -> "Слишком много попыток. Подождите немного."
        e is HttpException && e.code() in 500..599 -> "AniLiberty временно недоступен. Попробуйте позже."
        e is IOException -> "Нет соединения с AniLiberty. Проверьте интернет."
        else -> "Не удалось выполнить запрос. Попробуйте ещё раз."
    }
}
