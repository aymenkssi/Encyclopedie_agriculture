package org.agripedia.core

import org.agripedia.core.engine.ExperienceLevel
import org.agripedia.core.engine.FarmType
import org.agripedia.core.engine.NoteCode
import org.agripedia.core.engine.RecommendationEngine
import org.agripedia.core.engine.RecommendationQuery
import org.agripedia.core.engine.Season
import org.agripedia.core.engine.SuitabilityLevel
import org.agripedia.core.model.Hemisphere
import org.agripedia.core.model.Methods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private val c = TestData.catalog
    private val engine = RecommendationEngine(c)
    private val tomato = c.plantsById.getValue("solanum-lycopersicum")

    @Test
    fun tomatoIsRecommendedInMayInBrittany() {
        val r = engine.evaluate(tomato, RecommendationQuery(c.regionsById.getValue("fr-bre"), setOf(5)))
        assertEquals(SuitabilityLevel.RECOMMENDED, r.level)
    }

    @Test
    fun tomatoIsOnlyPossibleInJanuaryInBrittany() {
        val r = engine.evaluate(tomato, RecommendationQuery(c.regionsById.getValue("fr-bre"), setOf(1)))
        assertEquals(SuitabilityLevel.POSSIBLE, r.level)
    }

    @Test
    fun unsupportedMethodIsNotRecommended() {
        val r = engine.evaluate(tomato.copy(methods = listOf(Methods.OPEN_FIELD)), RecommendationQuery(c.regionsById.getValue("fr-bre"), setOf(5), methodId = Methods.CONTAINER))
        assertEquals(SuitabilityLevel.NOT_RECOMMENDED, r.level)
        assertTrue(r.notes.any { it.code == NoteCode.METHOD_NOT_SUITED })
    }

    @Test
    fun balconyForcesContainerMethod() {
        val list = engine.recommend(RecommendationQuery(c.regionsById.getValue("fr-paca"), setOf(4), farmType = FarmType.BALCONY))
        assertTrue(list.all { Methods.CONTAINER in it.plant.methods })
    }

    @Test
    fun resultsAreRankedByLevel() {
        val list = engine.recommend(RecommendationQuery(c.regionsById.getValue("ma-rsk"), setOf(3), experience = ExperienceLevel.BEGINNER), includeAll = true)
        assertEquals(list.map { it.level.ordinal }, list.map { it.level.ordinal }.sorted())
    }

    @Test
    fun seasonsAreShiftedInSouthernHemisphere() {
        assertEquals(setOf(12, 1, 2), Season.monthsOf(Season.SUMMER, null, Hemisphere.SOUTH))
        assertEquals(setOf(6, 7, 8), Season.monthsOf(Season.SUMMER, null, Hemisphere.NORTH))
        val dakar = c.regionsById.getValue("sn-dkr")
        assertEquals(setOf(7, 8, 9, 10), Season.monthsOf(Season.RAINY, dakar, Hemisphere.NORTH))
        assertEquals(Season.DRY, Season.current(2, dakar, Hemisphere.NORTH))
    }
}
