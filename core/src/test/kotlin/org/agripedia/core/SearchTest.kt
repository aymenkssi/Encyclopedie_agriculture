package org.agripedia.core

import org.agripedia.core.model.Difficulty
import org.agripedia.core.search.EditDistance
import org.agripedia.core.search.PlantFilter
import org.agripedia.core.search.PlantSearch
import org.agripedia.core.search.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    private val c = TestData.catalog
    private val search = PlantSearch(c)
    private fun top(q: String) = search.search(q).firstOrNull()?.plant?.id

    @Test fun findsByFrenchName() = assertEquals("solanum-lycopersicum", top("tomate"))
    @Test fun findsByEnglishName() = assertEquals("solanum-lycopersicum", top("Tomato"))
    @Test fun findsByArabicNameWithVariants() {
        assertEquals("solanum-lycopersicum", top("طماطم"))
        assertEquals("solanum-lycopersicum", top("بندوره")) // ة written as ه
    }
    @Test fun findsByScientificNameAndSynonym() {
        assertEquals("solanum-lycopersicum", top("solanum lyco"))
        assertEquals("solanum-lycopersicum", top("Lycopersicon esculentum"))
    }
    @Test fun toleratesTypos() {
        assertEquals("solanum-lycopersicum", top("tomtae"))
        assertEquals("solanum-lycopersicum", top("tommate"))
    }
    @Test fun noMatchForUnrelatedQuery() = assertTrue(search.search("xyzxyz").isEmpty())

    @Test fun filtersCombine() {
        assertTrue(search.search("", PlantFilter(categoryIds = setOf("vegetable"))).isNotEmpty())
        assertTrue(search.search("", PlantFilter(categoryIds = setOf("vegetable"), difficulties = setOf(Difficulty.EASY)))
            .all { it.plant.difficulty == Difficulty.EASY })
        val region = c.regionsById.getValue("fr-bre")
        val inMay = search.search("tomate", PlantFilter(region = region, months = setOf(5)))
        val inJan = search.search("tomate", PlantFilter(region = region, months = setOf(1)))
        assertEquals(1, inMay.size)
        assertTrue(inJan.isEmpty())
    }

    @Test fun normalizer() {
        assertEquals("pommier", TextNormalizer.normalize("Pommier"))
        assertEquals("ble", TextNormalizer.normalize("Blé"))
        assertEquals("coeur de boeuf", TextNormalizer.normalize("Cœur-de-bœuf"))
        assertEquals("طماطم", TextNormalizer.normalize("طَمَاطِم"))
    }

    @Test fun editDistance() {
        assertEquals(1, EditDistance.osa("tomate", "tomtae".let { "tomaet" }))
        assertEquals(1, EditDistance.osa("abc", "acb"))
        assertEquals(3, EditDistance.osa("", "abc"))
    }
}
