package top.aniliberty.app

import org.junit.Assert.*
import org.junit.Test
import top.aniliberty.app.data.*
import java.time.LocalDate
import java.time.ZoneId

class ProfileRulesTest {

    private fun entry(id: Int, ordinal: Double, at: Long, watched: Boolean = false, opened: Boolean = true, time: Double = 0.0, duration: Double = 1420.0) =
        WatchEntry(id, ordinal, "e$id-$ordinal", time, duration, watched = watched, opened = opened, updatedAt = at)

    private fun day(hour: Int, minusDays: Long = 0): Long =
        LocalDate.now(ZoneId.systemDefault()).atTime(hour, 0).minusDays(minusDays)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun statsCountsOpenedWatchedAndFavorites() {
        val library = LibraryState(
            collections = mapOf("favorite" to listOf(1, 2, 3), "watching" to listOf(1, 4)),
            history = mapOf(
                "1:1" to entry(1, 1.0, day(12), watched = true, time = 1420.0),
                "1:2" to entry(1, 2.0, day(13), watched = true, time = 1420.0),
                "2:1" to entry(2, 1.0, day(14), time = 300.0)
            )
        )
        val s = ProfileRules.stats(library, customImages = false, installedAt = 0)
        assertEquals(3, s.opened)
        assertEquals(2, s.watched)
        assertEquals(3, s.favorites)
        assertEquals(2, s.uniqueTitles)
        assertEquals(4, s.titlesInCollections)
    }

    @Test fun statsSplitsNightAndMorning() {
        val library = LibraryState(history = mapOf(
            "1:1" to entry(1, 1.0, day(2), watched = true, time = 1420.0),
            "1:2" to entry(1, 2.0, day(6), watched = true, time = 1420.0),
            "1:3" to entry(1, 3.0, day(20), watched = true, time = 1420.0)
        ))
        val s = ProfileRules.stats(library, false, 0)
        assertEquals(1, s.night)
        assertEquals(1, s.morning)
        assertEquals(3, s.maxPerDay)
    }

    @Test fun streakCountsConsecutiveDays() {
        val library = LibraryState(history = mapOf(
            "1:1" to entry(1, 1.0, day(12, minusDays = 3)),
            "1:2" to entry(1, 2.0, day(12, minusDays = 2)),
            "1:3" to entry(1, 3.0, day(12, minusDays = 1)),
            "1:4" to entry(1, 4.0, day(12, minusDays = 0)),
            "2:1" to entry(2, 1.0, day(12, minusDays = 9))
        ))
        assertEquals(4, ProfileRules.stats(library, false, 0).streakDays)
    }

    @Test fun weekRunCountsOneTitleInSevenDays() {
        val base = day(12)
        val week = 7L * 24 * 60 * 60 * 1000
        val history = (1..12).associate { i -> "$i:1" to entry(i, 1.0, base + i * 60_000L, watched = true, time = 1420.0) } +
            ("13:1" to entry(13, 1.0, base + week * 3, watched = true, time = 1420.0))
        // 12 episodes of different ids — maxWeekRun counts per title, so use same title instead
        val sameTitle = (1..12).associate { i -> "7:$i" to entry(7, i.toDouble(), base + i * 60_000L, watched = true, time = 1420.0) }
        assertEquals(12, ProfileRules.stats(LibraryState(history = sameTitle), false, 0).maxWeekRun)
        assertEquals(1, ProfileRules.stats(LibraryState(history = history), false, 0).maxWeekRun)
    }

    @Test fun badgesUnlockByThresholds() {
        val s = ProfileStats(opened = 62, watched = 40, hours = 78.0, favorites = 23, titlesInCollections = 30, uniqueTitles = 47, reminders = 7, maxPerDay = 6, night = 17, morning = 9, streakDays = 22, maxWeekRun = 8, ageDays = 240, customImages = true)
        val byId = ProfileRules.badges(s, level = 12).associateBy { it.id }
        assertTrue(byId.getValue("estet").unlocked)
        assertTrue(byId.getValue("first").unlocked)
        assertTrue(byId.getValue("viewer").unlocked)
        assertTrue(byId.getValue("kinoman").unlocked)
        assertTrue(byId.getValue("collector").unlocked)
        assertTrue(byId.getValue("fan").unlocked)
        assertFalse(byId.getValue("sprint").unlocked)
        assertTrue(byId.getValue("finisher").unlocked)
        assertFalse(byId.getValue("marathoner").unlocked)
        assertFalse(byId.getValue("legend").unlocked)
        assertFalse(byId.getValue("veteran").unlocked)
        assertEquals(19, byId.size)
    }

    @Test fun framesUnlockByProgress() {
        val s = ProfileStats(opened = 62, hours = 78.0, favorites = 23, uniqueTitles = 47, night = 17)
        val frames = ProfileRules.frames(s, level = 12).associateBy { it.id }
        assertTrue(frames.getValue(0).unlocked)   // always
        assertTrue(frames.getValue(2).unlocked)   // opened >= 10
        assertTrue(frames.getValue(1).unlocked)   // opened >= 50
        assertFalse(frames.getValue(4).unlocked)  // night < 50
        assertFalse(frames.getValue(5).unlocked)  // level < 15
        assertTrue(frames.getValue(6).unlocked)   // uniqueTitles >= 25
        assertFalse(frames.getValue(7).unlocked) // hours 78 < 100
    }

    @Test fun levelTitleScalesWithLevel() {
        assertEquals("Новичок", ProfileRules.levelTitle(1))
        assertEquals("Зритель", ProfileRules.levelTitle(5))
        assertEquals("Любитель аниме", ProfileRules.levelTitle(10))
        assertEquals("Ветеран", ProfileRules.levelTitle(15))
        assertEquals("Мастер аниме", ProfileRules.levelTitle(20))
        assertEquals("Легенда экрана", ProfileRules.levelTitle(30))
    }

    @Test fun scoreToNextIsNonNegative() {
        assertEquals(0, ProfileRules.scoreToNext(0).let { if (it < 0) -1 else 0 })
        assertTrue(ProfileRules.scoreToNext(2480) > 0)
    }
}
