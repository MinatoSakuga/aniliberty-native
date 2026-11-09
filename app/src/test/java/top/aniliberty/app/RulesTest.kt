package top.aniliberty.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.*
import org.junit.Test
import top.aniliberty.app.data.*

class RulesTest {
    private fun release(id: Int, title: String, adult: Boolean = false) = Release(id = id, name = LocalizedTitle(main = title), ageRating = AgeRating(adult = adult, label = if (adult) "18+" else "16+"))

    @Test fun explicitSearchIncludesAdultMatches() {
        val cyberpunk = release(9307, "Киберпанк: Бегущие по краю", adult = true)
        assertTrue(Rules.matches(cyberpunk, "киберпанк"))
        assertTrue(Rules.visible(cyberpunk, hideAdult = false))
        assertFalse(Rules.visible(cyberpunk, hideAdult = true))
    }

    @Test fun releasedEpisodesAreSortedAndComplete() {
        val detail = Release(id = 1, episodes = listOf(Episode(id = "c", ordinal = 3.0), Episode(id = "a", ordinal = 1.0), Episode(id = "b", ordinal = 2.0)))
        assertEquals(listOf("a", "b", "c"), Rules.releasedEpisodes(detail).map { it.id })
        assertEquals(3, Rules.releasedEpisodes(detail).size)
    }

    @Test fun favoritesNewestFirstLocalAddAndRemove() {
        var order = emptyList<Int>()
        order = Rules.orderFavorites(order, listOf(10) + order)
        order = Rules.orderFavorites(order, listOf(20) + order)
        order = Rules.orderFavorites(order, listOf(30) + order)
        assertEquals(listOf(30, 20, 10), order)
        order = Rules.orderFavorites(order, order - 20)
        assertEquals(listOf(30, 10), order)
        order = Rules.orderFavorites(order, listOf(20) + order)
        assertEquals(listOf(20, 30, 10), order)
    }

    @Test fun syncPreservesKnownLocalAddOrder() {
        val local = listOf(30, 20, 10)
        val server = listOf(10, 20, 30, 40)
        assertEquals(listOf(40, 30, 20, 10), Rules.orderFavorites(local, server))
    }

    @Test fun favoriteDisplayDirectionTogglesAndPersists() {
        val order = listOf(30, 20, 10)
        assertEquals(listOf(30, 20, 10), Rules.displayFavorites(order, newestFirst = true))
        assertEquals(listOf(10, 20, 30), Rules.displayFavorites(order, newestFirst = false))
        val settings = Settings()
        assertTrue(settings.favoritesNewestFirst)
        val toggled = settings.copy(favoritesNewestFirst = !settings.favoritesNewestFirst)
        val decoded = Network.json.decodeFromString<Settings>(Network.json.encodeToString(Settings.serializer(), toggled))
        assertFalse(decoded.favoritesNewestFirst)
        assertEquals(listOf(10, 20, 30), Rules.displayFavorites(order, decoded.favoritesNewestFirst))
    }

    @Test fun mergeWithoutServerOrderPreservesKnownAndPrependsUnknown() {
        assertEquals(listOf(40, 30, 20, 10), Rules.orderFavorites(listOf(30, 20, 10), listOf(10, 20, 30, 40)))
    }

    @Test fun collectionTabsKeepSavedOrderAppendMissingAndDropUnknown() {
        val all = listOf("watching", "planned", "watched", "postponed", "abandoned")
        assertEquals(all, Rules.collectionTabOrder(emptyList()))
        assertEquals(listOf("watched", "watching", "planned", "postponed", "abandoned"), Rules.collectionTabOrder(listOf("watched", "watching")))
        assertEquals(all, Rules.collectionTabOrder(listOf("favorite", "bogus") + all))
        assertEquals(listOf("planned", "watching", "watched", "postponed", "abandoned"), Rules.collectionTabOrder(listOf("planned", "planned", "watching")))
        assertFalse(Rules.collectionTabOrder(all).contains("favorite"))
    }

    @Test fun openingSkipRespectsBoundaries() {
        val opening = TimeRange(start = 10.0, stop = 100.0)
        assertFalse(Rules.shouldSkipOpening(9_000, opening, true))
        assertTrue(Rules.shouldSkipOpening(50_000, opening, true))
        assertFalse(Rules.shouldSkipOpening(100_000, opening, true))
        assertFalse(Rules.shouldSkipOpening(50_000, opening, false))
    }

    @Test fun firstNotificationBaselineDoesNotSpamHistory() {
        assertTrue(Rules.newEpisodeIds(emptyMap(), mapOf("1" to "ep1")).isEmpty())
        assertEquals(setOf("1"), Rules.newEpisodeIds(mapOf("1" to "ep1"), mapOf("1" to "ep2", "2" to "ep1")))
    }

    @Test fun parsesPolymorphicCollections() {
        val array = Json.parseToJsonElement("[[1,\"WATCHING\"],[2,\"PLANNED\"],[3,\"WATCHED\"],[4,\"POSTPONED\"],[5,\"ABANDONED\"]]") as JsonArray
        val parsed = Rules.parseCollections(array)
        assertEquals(listOf(1), parsed["watching"])
        assertEquals(listOf(2), parsed["planned"])
        assertEquals(listOf(3), parsed["watched"])
        assertNull(parsed["favorite"])
    }

    @Test fun adultContentIsVisibleByDefault() {
        assertFalse(Settings().hideAdult)
        assertEquals("dark", Settings().themeMode)
    }

    @Test fun voiceActorsComeOnlyFromVoicingMembers() {
        val release = Release(id = 1, members = listOf(
            ReleaseMember(role = MemberRole("voicing", "Озвучка"), nickname = " MyAska "),
            ReleaseMember(role = MemberRole("translating"), nickname = "Translator"),
            ReleaseMember(role = MemberRole("voicing"), nickname = "MyAska"),
            ReleaseMember(role = MemberRole("voicing"), nickname = " ")
        ))
        assertEquals(listOf("MyAska"), Rules.voiceActors(release))
        assertTrue(Rules.voiceActors(Release(id = 2)).isEmpty())
    }

    @Test fun profileScoreRewardsCollectionsAndPenalizesAbandoned() {
        val library = LibraryState(collections = mapOf(
            "watched" to listOf(1, 2), "watching" to listOf(3), "planned" to listOf(4),
            "favorite" to listOf(1, 3), "abandoned" to listOf(5)
        ), history = mapOf("1:1" to WatchEntry(1, 1.0, "e", 100.0, 100.0, watched = true)))
        val score = Rules.profileScore(library)
        assertEquals(2 * 12 + 5 + 3 + 2 * 2 - 4 + 2 + 10, score)
        assertTrue(Rules.profileLevel(score) >= 1)
        assertTrue(Rules.levelProgress(score) in 0f..1f)
    }

    @Test fun doubleTapSeekClampsToBoundaries() {
        assertEquals(0L, Rules.seekTarget(5_000, 100_000, -10_000))
        assertEquals(15_000L, Rules.seekTarget(5_000, 100_000, 10_000))
        assertEquals(100_000L, Rules.seekTarget(95_000, 100_000, 10_000))
    }

    @Test fun openedEpisodeGetsCheckmarkEvenBeforeCompletion() {
        val opened = WatchEntry(1, 1.0, "e", 5.0, 100.0, opened = true)
        assertTrue(Rules.seen(opened))
        assertFalse(Rules.watched(opened))
        assertTrue(Rules.seen(WatchEntry(1, 1.0, "e", 0.0, 100.0, watched = true)))
    }

    @Test fun episodeFingerprintsDetectNewEpisodesWithoutFirstRunSpam() {
        val previous = mapOf(1 to EpisodeFingerprint("ep1", 1.0))
        val current = mapOf(1 to EpisodeFingerprint("ep2", 2.0), 2 to EpisodeFingerprint("x", 1.0))
        assertTrue(EpisodeRules.changed(emptyMap(), current).isEmpty())
        assertEquals(setOf(1), EpisodeRules.changed(previous, current))
        assertTrue(EpisodeRules.changed(previous, mapOf(1 to EpisodeFingerprint("ep1", 1.0))).isEmpty())
    }

    @Test fun episodeQualityFallbacks() {
        val episode = Episode(hls480 = "480", hls1080 = "1080")
        assertEquals("480", episode.url(720))
        assertEquals("1080", episode.url(1080))
        assertEquals("480", episode.url(480))
    }

    @Test fun snapshotBundleParsesAndContainsCyberpunk() {
        val file = java.io.File("src/main/assets/snapshot.json")
        assertTrue("bundled snapshot must exist", file.exists())
        val snapshot = Network.json.decodeFromString<Snapshot>(file.readText())
        assertEquals(114, snapshot.items.size)
        assertTrue(snapshot.details.values.sumOf { it.episodes?.size ?: 0 } >= 1700)
        assertTrue(snapshot.items.containsKey("9307"))
        assertTrue(snapshot.items.containsKey("10364"))
        assertTrue(snapshot.items.getValue("9307").ageRating.adult)
    }
}
