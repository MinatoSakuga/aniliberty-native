package top.aniliberty.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable data class LocalizedTitle(val main: String = "Без названия", val english: String? = null, val alternative: String? = null)
@Serializable data class ImageSource(val src: String? = null, val preview: String? = null, val thumbnail: String? = null, val optimized: ImageSource? = null) {
    val best: String? get() = optimized?.src ?: optimized?.preview ?: src ?: preview
}
@Serializable data class Genre(val id: Int = 0, val name: String = "")
@Serializable data class TypeRef(val value: String = "TV", val description: String? = null)
@Serializable data class AgeRating(val value: String = "R16_PLUS", val label: String = "16+", @SerialName("is_adult") val adult: Boolean = false)
@Serializable data class PublishDay(val value: Int = 1, val description: String? = null)
@Serializable data class ExternalRating(val rating: Double? = null, val url: String? = null)
@Serializable data class MemberRole(val value: String = "", val description: String? = null)
@Serializable data class ReleaseMember(val id: String = "", val role: MemberRole = MemberRole(), val nickname: String? = null)
@Serializable data class TimeRange(val start: Double? = null, val stop: Double? = null)
@Serializable data class Episode(
    val id: String = "",
    val ordinal: Double = 0.0,
    val name: String? = null,
    @SerialName("name_english") val englishName: String? = null,
    val duration: Double? = null,
    val preview: ImageSource? = null,
    val opening: TimeRange? = null,
    val ending: TimeRange? = null,
    @SerialName("hls_480") val hls480: String? = null,
    @SerialName("hls_720") val hls720: String? = null,
    @SerialName("hls_1080") val hls1080: String? = null,
    @SerialName("release_id") val releaseId: Int? = null
) {
    val number: String get() = if (ordinal % 1.0 == 0.0) ordinal.toInt().toString() else ordinal.toString()
    fun url(quality: Int): String? = when (quality) { 1080 -> hls1080 ?: hls720 ?: hls480; 480 -> hls480 ?: hls720; else -> hls720 ?: hls480 ?: hls1080 }
}
@Serializable data class Release(
    val id: Int,
    val name: LocalizedTitle = LocalizedTitle(),
    val alias: String? = null,
    val poster: ImageSource? = null,
    val year: Int? = null,
    val type: TypeRef? = null,
    val genres: List<Genre> = emptyList(),
    val description: String? = null,
    @SerialName("is_ongoing") val ongoing: Boolean = false,
    @SerialName("episodes_total") val episodesTotal: Int? = null,
    @SerialName("age_rating") val ageRating: AgeRating = AgeRating(),
    @SerialName("fresh_at") val freshAt: String? = null,
    @SerialName("publish_day") val publishDay: PublishDay? = null,
    @SerialName("latest_episode") val latestEpisode: Episode? = null,
    val episodes: List<Episode>? = null,
    @SerialName("is_blocked_by_geo") val geoBlocked: Boolean = false,
    @SerialName("is_blocked_by_copyrights") val copyrightBlocked: Boolean = false,
    @SerialName("average_duration_of_episode") val averageDuration: Int? = null,
    @SerialName("added_in_users_favorites") val favoritesCount: Int? = null,
    val shikimori: ExternalRating? = null,
    val members: List<ReleaseMember> = emptyList()
)
@Serializable data class Pagination(val total: Int = 0, @SerialName("total_pages") val totalPages: Int = 1, val page: Int = 1)
@Serializable data class PageMeta(val pagination: Pagination = Pagination())
@Serializable data class CatalogPage(val data: List<Release> = emptyList(), val meta: PageMeta = PageMeta())
@Serializable data class ScheduleItem(val release: Release, @SerialName("full_season_is_released") val complete: Boolean = false, @SerialName("next_release_episode_number") val next: Int? = null, @SerialName("published_release_episode") val published: Episode? = null)
@Serializable data class ScheduleResponse(val data: List<ScheduleItem> = emptyList())
@Serializable data class TokenResponse(val token: String = "")
@Serializable data class LoginRequest(val login: String, val password: String)
@Serializable data class EmptyRequest(val placeholder: String? = null)
@Serializable data class OtpStartRequest(@SerialName("device_id") val deviceId: String)
@Serializable data class OtpCode(val code: String = "", @SerialName("device_id") val deviceId: String = "")
@Serializable data class OtpStartResponse(val otp: OtpCode = OtpCode(), @SerialName("remaining_time") val remaining: Int = 120)
@Serializable data class OtpLoginRequest(val code: Int, @SerialName("device_id") val deviceId: String)
@Serializable data class SocialResponse(val url: String = "", val state: String = "")
@Serializable data class Profile(val id: Int = 0, val login: String = "", val nickname: String? = null, val avatar: ImageSource? = null) {
    val displayName: String get() = nickname?.takeIf { it.isNotBlank() } ?: login
}
@Serializable data class ReleaseId(@SerialName("release_id") val releaseId: Int)
@Serializable data class CollectionChange(@SerialName("release_id") val releaseId: Int, @SerialName("type_of_collection") val type: String)
@Serializable data class TimecodeRequest(val time: Long, @SerialName("is_watched") val watched: Boolean, @SerialName("release_episode_id") val episodeId: String)
@Serializable data class HistoryEpisode(val id: String = "", val ordinal: Double = 0.0, val duration: Double? = null, val release: Release? = null, @SerialName("release_id") val releaseId: Int? = null)
@Serializable data class HistoryItem(@SerialName("release_episode") val episode: HistoryEpisode? = null, val time: Double = 0.0, @SerialName("is_watched") val watched: Boolean = false, @SerialName("updated_at") val updatedAt: String? = null)
@Serializable data class HistoryPage(val data: List<HistoryItem> = emptyList(), val meta: PageMeta = PageMeta())
@Serializable data class WatchEntry(val releaseId: Int, val ordinal: Double, val episodeId: String, val time: Double, val duration: Double, val watched: Boolean = false, val opened: Boolean = false, val updatedAt: Long = System.currentTimeMillis()) {
    val key: String get() = "$releaseId:$ordinal"
    val progress: Float get() = if (duration > 0) (time / duration).toFloat().coerceIn(0f, 1f) else 0f
}
@Serializable data class LibraryState(
    val collections: Map<String, List<Int>> = CollectionKind.entries.associate { it.key to emptyList() },
    val history: Map<String, WatchEntry> = emptyMap(),
    val reminders: List<Int> = emptyList(),
    val searches: List<String> = emptyList(),
    val favoriteOrder: List<Int> = emptyList()
)
enum class CollectionKind(val key: String, val api: String, val title: String) {
    WATCHING("watching", "WATCHING", "Смотрю"), PLANNED("planned", "PLANNED", "В планах"), WATCHED("watched", "WATCHED", "Просмотрено"), FAVORITE("favorite", "", "Избранное"), POSTPONED("postponed", "POSTPONED", "Отложено"), ABANDONED("abandoned", "ABANDONED", "Брошено");
    companion object { fun fromApi(value: String) = entries.firstOrNull { it.api == value } }
}
@Serializable data class BundledSchedule(@SerialName("release_id") val releaseId: String = "", @SerialName("full_season_is_released") val complete: Boolean = false, @SerialName("next_release_episode_number") val next: Int? = null)
@Serializable data class Snapshot(val items: Map<String, Release> = emptyMap(), val details: Map<String, Release> = emptyMap(), val latest: List<String> = emptyList(), val popular: List<String> = emptyList(), val hero: List<String> = emptyList(), val genres: List<Genre> = emptyList(), val schedule: List<BundledSchedule> = emptyList())
@Serializable data class CachedDetail(val release: Release, val at: Long)

object Rules {
    fun releasedEpisodes(detail: Release?): List<Episode> = detail?.episodes?.sortedBy { it.ordinal }.orEmpty()
    fun matches(release: Release, query: String): Boolean {
        val q = query.trim().lowercase()
        return q.length >= 2 && listOf(release.name.main, release.name.english, release.name.alternative, release.alias).any { it?.lowercase()?.contains(q) == true }
    }
    fun visible(release: Release, hideAdult: Boolean) = !hideAdult || !release.ageRating.adult
    fun watched(entry: WatchEntry?) = entry != null && (entry.watched || entry.progress >= .9f)
    fun seen(entry: WatchEntry?) = entry != null && (entry.opened || entry.watched || entry.time > 0.0 || entry.progress >= .9f)
    fun shouldSkipOpening(positionMs: Long, opening: TimeRange?, enabled: Boolean): Boolean {
        if (!enabled || opening == null) return false
        val start = opening.start ?: return false; val stop = opening.stop ?: return false
        return positionMs / 1000.0 >= start && positionMs / 1000.0 < stop - 1
    }
    fun parseCollections(array: JsonArray): Map<String, List<Int>> {
        val result = CollectionKind.entries.filter { it != CollectionKind.FAVORITE }.associate { it.key to mutableListOf<Int>() }
        array.forEach { row ->
            if (row is JsonArray && row.size == 2) {
                val id = row[0].toString().trim('"').toIntOrNull(); val kind = row[1].toString().trim('"')
                val key = CollectionKind.fromApi(kind)?.key
                if (id != null && key != null) result.getValue(key).add(id)
            }
        }
        return result
    }
    /** Canonical order is newest-first. Known local additions keep their position across sync. */
    fun orderFavorites(current: List<Int>, incoming: List<Int>): List<Int> {
        val unique = incoming.distinct()
        return unique.filterNot { it in current } + current.filter { it in unique }
    }
    fun displayFavorites(order: List<Int>, newestFirst: Boolean): List<Int> = if (newestFirst) order.distinct() else order.distinct().asReversed()
    /** Вкладки коллекций после закреплённого «Избранного»: сохранённый порядок, затем недостающие, без дублей и чужих ключей. */
    fun collectionTabOrder(saved: List<String>): List<String> {
        val keys = CollectionKind.entries.filter { it != CollectionKind.FAVORITE }.map { it.key }
        return (saved.filter { it in keys } + keys.filterNot { it in saved }).distinct()
    }
    fun newEpisodeIds(previous: Map<String, String>, current: Map<String, String>): Set<String> = if (previous.isEmpty()) emptySet() else current.filter { (id, ep) -> previous[id] != null && previous[id] != ep }.keys
    fun voiceActors(release: Release?): List<String> = release?.members.orEmpty().filter { it.role.value.equals("voicing", true) }.mapNotNull { it.nickname?.trim()?.takeIf(String::isNotEmpty) }.distinct()
    fun profileScore(library: LibraryState): Int {
        val c = library.collections
        val collectionPoints = c["watched"].orEmpty().size * 12 + c["watching"].orEmpty().size * 5 + c["planned"].orEmpty().size * 3 + c["favorite"].orEmpty().size * 2 - c["abandoned"].orEmpty().size * 4
        val watchedEpisodes = library.history.values.count { it.watched || it.progress >= .9f }
        val completedSeasons = c["watched"].orEmpty().count { id -> library.history.values.any { it.releaseId == id && (it.watched || it.progress >= .9f) } }
        return (collectionPoints + watchedEpisodes * 2 + completedSeasons * 10).coerceAtLeast(0)
    }
    fun profileLevel(score: Int): Int = (kotlin.math.sqrt(score.coerceAtLeast(0) / 120.0).toInt() + 1).coerceAtLeast(1)
    fun levelProgress(score: Int): Float { val level = profileLevel(score); val floor = (level - 1) * (level - 1) * 120; val next = level * level * 120; return ((score - floor).toFloat() / (next - floor).coerceAtLeast(1)).coerceIn(0f, 1f) }
    fun seekTarget(currentMs: Long, durationMs: Long, deltaMs: Long): Long = (currentMs + deltaMs).coerceIn(0L, durationMs.coerceAtLeast(0L))
}

data class ProfileStats(
    val opened: Int = 0, val watched: Int = 0, val hours: Double = 0.0, val favorites: Int = 0,
    val titlesInCollections: Int = 0, val uniqueTitles: Int = 0, val reminders: Int = 0,
    val maxPerDay: Int = 0, val night: Int = 0, val morning: Int = 0, val streakDays: Int = 0,
    val maxWeekRun: Int = 0, val ageDays: Int = 0, val customImages: Boolean = false
)

enum class BadgeRarity { COMMON, RARE, EPIC }

data class Badge(
    val id: String, val title: String, val desc: String, val rarity: BadgeRarity,
    val unlocked: Boolean, val progress: Float, val progressText: String
)

data class FrameInfo(val id: Int, val name: String, val unlockDesc: String, val unlocked: Boolean)

object ProfileRules {
    fun levelTitle(level: Int): String = when {
        level >= 30 -> "Легенда экрана"
        level >= 20 -> "Мастер аниме"
        level >= 15 -> "Ветеран"
        level >= 10 -> "Любитель аниме"
        level >= 5 -> "Зритель"
        else -> "Новичок"
    }

    fun scoreToNext(score: Int): Int {
        val level = Rules.profileLevel(score)
        return (level * level * 120 - score).coerceAtLeast(0)
    }

    fun stats(library: LibraryState, customImages: Boolean, installedAt: Long, now: Long = System.currentTimeMillis()): ProfileStats {
        val entries = library.history.values.filter { Rules.seen(it) }
        val opened = entries.size
        val watched = library.history.values.count { Rules.watched(it) }
        val seconds = library.history.values.sumOf { if (Rules.watched(it)) it.duration.coerceAtLeast(0.0) else it.time.coerceAtLeast(0.0) }
        val favorites = library.collections["favorite"].orEmpty().size
        val titlesInCollections = library.collections.values.flatten().distinct().size
        val uniqueTitles = entries.map { it.releaseId }.distinct().size
        val zone = java.time.ZoneId.systemDefault()
        val hoursOfDay = entries.map { java.time.Instant.ofEpochMilli(it.updatedAt).atZone(zone) }
        val night = hoursOfDay.count { it.hour < 5 }
        val morning = hoursOfDay.count { it.hour in 5..7 }
        val perDay = hoursOfDay.map { it.toLocalDate() }.groupingBy { it }.eachCount()
        val maxPerDay = perDay.values.maxOrNull() ?: 0
        var streak = 0; var best = 0; var prev: java.time.LocalDate? = null
        perDay.keys.sorted().forEach { day ->
            streak = if (prev != null && day == prev!!.plusDays(1)) streak + 1 else 1
            if (streak > best) best = streak
            prev = day
        }
        var maxWeek = 0
        library.history.values.filter { Rules.seen(it) }.groupBy { it.releaseId }.values.forEach { list ->
            val ts = list.map { it.updatedAt }.sorted()
            var left = 0
            for (right in ts.indices) {
                while (ts[right] - ts[left] > 7L * 24 * 60 * 60 * 1000) left++
                if (right - left + 1 > maxWeek) maxWeek = right - left + 1
            }
        }
        val ageDays = if (installedAt > 0) ((now - installedAt) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0) else 0
        return ProfileStats(
            opened = opened, watched = watched, hours = seconds / 3600.0, favorites = favorites,
            titlesInCollections = titlesInCollections, uniqueTitles = uniqueTitles,
            reminders = library.reminders.size, maxPerDay = maxPerDay, night = night,
            morning = morning, streakDays = best, maxWeekRun = maxWeek, ageDays = ageDays,
            customImages = customImages
        )
    }

    private fun badge(id: String, title: String, desc: String, rarity: BadgeRarity, value: Int, target: Int): Badge {
        val unlocked = value >= target
        return Badge(id, title, desc, rarity, unlocked, (value.toFloat() / target).coerceIn(0f, 1f), "${minOf(value, target)} / $target")
    }

    fun badges(s: ProfileStats, level: Int): List<Badge> = listOf(
        Badge("estet", "Эстет", "Своя обложка или аватар", BadgeRarity.RARE, s.customImages, if (s.customImages) 1f else 0f, if (s.customImages) "Получено" else "0 / 1"),
        badge("first", "Первые шаги", "Первая открытая серия", BadgeRarity.COMMON, s.opened, 1),
        badge("viewer", "Зритель", "10 открытых серий", BadgeRarity.COMMON, s.opened, 10),
        badge("kinoman", "Киноман", "25 тайтлов в коллекциях", BadgeRarity.RARE, s.titlesInCollections, 25),
        badge("collector", "Коллекционер", "20 тайтлов в избранном", BadgeRarity.COMMON, s.favorites, 20),
        badge("fan", "Фанат", "50 открытых серий", BadgeRarity.COMMON, s.opened, 50),
        badge("sprint", "Спринт", "10 серий за один день", BadgeRarity.RARE, s.maxPerDay, 10),
        badge("finisher", "Досмотрщик", "25 серий досмотрено", BadgeRarity.COMMON, s.watched, 25),
        badge("marathoner", "Марафонец", "100 часов просмотра", BadgeRarity.RARE, s.hours.toInt(), 100),
        badge("legend", "Легенда", "100 открытых серий", BadgeRarity.EPIC, s.opened, 100),
        badge("traveler", "Путешественник", "100 разных тайтлов", BadgeRarity.RARE, s.uniqueTitles, 100),
        badge("night", "Ночная сова", "50 серий после полуночи", BadgeRarity.COMMON, s.night, 50),
        badge("early", "Ранняя пташка", "25 серий до 8 утра", BadgeRarity.COMMON, s.morning, 25),
        badge("scheduler", "Знаток расписания", "20 напоминаний о премьерах", BadgeRarity.COMMON, s.reminders, 20),
        badge("steady", "Постоянный", "30 дней подряд с сериями", BadgeRarity.RARE, s.streakDays, 30),
        badge("keeper", "Хранитель", "50 тайтлов в избранном", BadgeRarity.EPIC, s.favorites, 50),
        badge("titan", "Титан", "250 открытых серий", BadgeRarity.EPIC, s.opened, 250),
        badge("speedy", "Скоростной", "Сезон за одну неделю", BadgeRarity.EPIC, s.maxWeekRun, 12),
        badge("veteran", "Ветеран", "2 года в приложении", BadgeRarity.EPIC, s.ageDays, 730)
    )

    fun frames(s: ProfileStats, level: Int): List<FrameInfo> = listOf(
        FrameInfo(0, "Руны", "Доступна сразу", true),
        FrameInfo(1, "Мандала", "50 открытых серий", s.opened >= 50),
        FrameInfo(2, "Орбита", "10 открытых серий", s.opened >= 10),
        FrameInfo(3, "Эфир", "50 часов просмотра", s.hours >= 50.0),
        FrameInfo(4, "Созвездие", "50 серий после полуночи", s.night >= 50),
        FrameInfo(5, "Печать", "15 уровень", level >= 15),
        FrameInfo(6, "Луна", "25 тайтлов", s.uniqueTitles >= 25),
        FrameInfo(7, "Аура", "100 часов просмотра", s.hours >= 100.0),
        FrameInfo(8, "Цветок", "20 в избранном", s.favorites >= 20),
        FrameInfo(9, "Сеть", "300 открытых серий", s.opened >= 300)
    )
}
@Serializable data class EpisodeFingerprint(val episodeId: String = "", val ordinal: Double = 0.0)
object EpisodeRules {
    fun fingerprint(release: Release): EpisodeFingerprint? {
        val ep = release.latestEpisode ?: release.episodes?.maxByOrNull { it.ordinal } ?: return null
        if (ep.id.isBlank() && ep.ordinal <= 0.0) return null
        return EpisodeFingerprint(ep.id, ep.ordinal)
    }
    fun changed(previous: Map<Int, EpisodeFingerprint>, current: Map<Int, EpisodeFingerprint>): Set<Int> =
        if (previous.isEmpty()) emptySet() else current.filter { (id, fp) -> previous[id]?.let { fp.episodeId != it.episodeId || fp.ordinal > it.ordinal } == true }.keys
}
fun imageUrl(source: ImageSource?): String? = source?.best?.let { if (it.startsWith("http")) it else "https://aniliberty.top$it" }
fun formatTime(seconds: Double): String { val s = seconds.toLong().coerceAtLeast(0); return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60) }

/** Части франшизы (сиквелы/сезоны) подбираются по основе названия. */
object Franchise {
    private val bracketYear = Regex("\\(\\d{4}\\)\\s*$")
    private val separators = Regex("[:—–]| - ")
    private val wordPart = Regex("[:\\-–—\\s]+(сезон|season|часть|part|финал|final)\\s*\\d*\\s*$", RegexOption.IGNORE_CASE)
    private val numPart = Regex("[:\\-–—\\s]*\\d+\\s*$")
    private val romanPart = Regex("[:\\-–—\\s]+(xiii|xii|xi|ix|viii|vii|vi|iv|iii|ii|i|v|x)\\s*$", RegexOption.IGNORE_CASE)

    /** Основа названия: «Мастер меча онлайн: Алисизация» и «Мастер меча онлайн II» -> «мастер меча онлайн». */
    fun base(name: String): String {
        var s = name.lowercase().replace('ё', 'е').trim().replace(bracketYear, "").trim()
        val cut = separators.find(s)?.range?.first
        if (cut != null) {
            val left = s.substring(0, cut).trim()
            val words = left.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 2 || left.length >= 10) s = left
        }
        while (true) {
            val n = s.replace(wordPart, "").replace(numPart, "").replace(romanPart, "").trim().trimEnd(':', '-', '–', '—', ' ')
            if (n == s || n.length < 3) break
            s = n
        }
        return s.trim()
    }

    /** Все части франшизы: название начинается с основы (по границе слова), включая текущую. */
    fun parts(all: Collection<Release>, current: Release): List<Release> {
        val b = base(current.name.main)
        if (b.length < 3) return emptyList()
        return all.filter { r ->
            val n = r.name.main.lowercase().replace('ё', 'е').trim()
            if (n == b) true
            else if (!n.startsWith(b)) false
            else {
                val c = n.getOrNull(b.length)
                c == null || c == ' ' || c == ':' || c == '(' || c.isDigit()
            }
        }.distinctBy { it.id }.sortedWith(compareBy({ it.year ?: 9999 }, { it.id }))
    }
}
