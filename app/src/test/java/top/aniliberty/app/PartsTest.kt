package top.aniliberty.app

import org.junit.Assert.*
import org.junit.Test
import top.aniliberty.app.data.Franchise
import top.aniliberty.app.data.LocalizedTitle
import top.aniliberty.app.data.Release

class PartsTest {
    private fun r(id: Int, name: String, year: Int? = null) = Release(id = id, name = LocalizedTitle(main = name), year = year)

    @Test fun baseStripsSuffixesAndColonTails() {
        assertEquals("магическая битва", Franchise.base("Магическая битва 2"))
        assertEquals("магическая битва", Franchise.base("Магическая битва"))
        assertEquals("атака титанов", Franchise.base("Атака титанов: Сезон 3"))
        assertEquals("атака титанов", Franchise.base("Атака титанов"))
        assertEquals("ванпанчмен", Franchise.base("Ванпанчмен II"))
        assertEquals("ванпанчмен", Franchise.base("Ванпанчмен: Часть 2"))
        assertEquals("мастер меча онлайн", Franchise.base("Мастер меча онлайн: Алисизация — Война в Подземелье"))
        assertEquals("лог горизонт", Franchise.base("Лог горизонт: Второй прогон"))
    }

    @Test fun baseKeepsShortPrefixesBeforeColon() {
        assertEquals("re:zero", Franchise.base("Re:Zero"))
        assertEquals("тетрадь смерти", Franchise.base("Тётрадь смерти (2006)"))
    }

    @Test fun partsFindAllSeasonsFromAnyPart() {
        val all = listOf(
            r(1, "Лог горизонт", 2013),
            r(2, "Лог горизонт 2", 2014),
            r(3, "Лог горизонт: Второй прогон", 2021),
            r(9, "Ванпанчмен", 2015)
        )
        assertEquals(listOf(1, 2, 3), Franchise.parts(all, all[0]).map { it.id })
        assertEquals(listOf(1, 2, 3), Franchise.parts(all, all[1]).map { it.id })
        assertEquals(listOf(1, 2, 3), Franchise.parts(all, all[2]).map { it.id })
    }

    @Test fun partsIncludeColonSpinoffs() {
        val all = listOf(
            r(1, "Мастер меча онлайн", 2012),
            r(2, "Мастер меча онлайн II", 2014),
            r(3, "Мастер меча онлайн: Алисизация", 2018),
            r(4, "Мастер меча онлайн: Алисизация — Война в Подземелье", 2020),
            r(5, "Мастер меча онлайн: Прогрессив", 2021),
            r(8, "Мастер мечей", 2024)
        )
        val parts = Franchise.parts(all, all[2])
        assertEquals(listOf(1, 2, 3, 4, 5), parts.map { it.id })
    }

    @Test fun partsIgnoreHyphenatedOtherFranchises() {
        val all = listOf(r(1, "Монстр", 2004), r(2, "Монстр-хантер", 2021), r(3, "Токийские мстители", 2021))
        val parts = Franchise.parts(all, all[0])
        assertFalse(parts.any { it.id == 2 })
        assertFalse(Franchise.parts(all, all[2]).any { it.id == 1 })
    }

    @Test fun partsReturnEmptyForShortNames() {
        val all = listOf(r(1, "86", 2021), r(2, "86 2", 2022))
        assertTrue(Franchise.parts(all, all[0]).isEmpty())
    }
}
