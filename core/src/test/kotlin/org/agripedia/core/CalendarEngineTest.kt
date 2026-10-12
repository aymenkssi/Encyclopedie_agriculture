package org.agripedia.core

import org.agripedia.core.engine.CalendarEngine
import org.agripedia.core.engine.CalendarOrigin
import org.agripedia.core.engine.Months
import org.agripedia.core.engine.NoteCode
import org.agripedia.core.model.Activity
import org.agripedia.core.model.FrostTolerance
import org.agripedia.core.model.Methods
import org.agripedia.core.model.Range
import org.agripedia.core.model.Region
import org.agripedia.core.model.SowingMode
import org.agripedia.core.model.TemperatureRange
import org.agripedia.core.model.WaterNeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEngineTest {
    private val c = TestData.catalog
    private val engine = CalendarEngine(c)
    private val tomato = c.plantsById.getValue("solanum-lycopersicum")

    @Test
    fun explicitZoneEntriesTakePrecedence() {
        val cal = engine.compute(tomato, c.regionsById.getValue("fr-bre"), Methods.OPEN_FIELD)
        val transplant = cal.windows.single { it.activity == Activity.TRANSPLANTING }
        assertEquals(CalendarOrigin.EXPLICIT_ZONE, transplant.origin)
        assertEquals(setOf(5, 6), transplant.months)
        assertTrue(cal.notes.any { it.code == NoteCode.EXPLICIT_DATA_USED })
    }

    @Test
    fun computedCalendarNeverPlantsFrostSensitiveCropDuringFrostMonths() {
        // Greenhouse is not covered by explicit entries -> computed.
        val region = c.regionsById.getValue("ca-qc")
        val cal = engine.compute(tomato, region, Methods.OPEN_FIELD)
        val frost = engine.frostMonths(region)
        val transplant = cal.monthsFor(Activity.TRANSPLANTING)
        assertTrue("Quebec should allow a summer window: $transplant", transplant.isNotEmpty())
        assertTrue(transplant.none { it in frost })
        assertTrue(transplant.all { it in 6..8 })
    }

    @Test
    fun tropicalRegionAvoidsHottestMonthsForTomato() {
        val cal = engine.compute(tomato, c.regionsById.getValue("sn-kao"), Methods.OPEN_FIELD)
        val transplant = cal.monthsFor(Activity.TRANSPLANTING)
        assertTrue(transplant.isNotEmpty())
        assertFalse("May is too hot in Kaolack", 5 in transplant)
        assertTrue(cal.notes.any { it.code == NoteCode.TOO_HOT_MONTHS })
    }

    @Test
    fun hydroponicsIsAllYear() {
        val cal = engine.compute(tomato, c.regionsById.getValue("ca-qc"), Methods.HYDROPONICS)
        assertEquals(12, cal.monthsFor(Activity.DIRECT_SOWING).size)
        assertTrue(cal.notes.any { it.code == NoteCode.CONTROLLED_ENVIRONMENT })
    }

    @Test
    fun greenhouseExtendsTheSeason() {
        val region = c.regionsById.getValue("fr-als")
        val open = engine.compute(tomato.copy(calendar = emptyList()), region, Methods.OPEN_FIELD).monthsFor(Activity.TRANSPLANTING)
        val gh = engine.compute(tomato.copy(calendar = emptyList()), region, Methods.GREENHOUSE).monthsFor(Activity.TRANSPLANTING)
        assertTrue("greenhouse $gh should include open-field $open", gh.containsAll(open))
        assertTrue(gh.size > open.size)
    }

    @Test
    fun missingClimateDataYieldsUnavailableCalendar() {
        val region = c.regionsById.getValue("fr-idf").copy(monthlyMeanTempC = null, monthlyMinTempC = null)
        val cal = engine.compute(tomato.copy(calendar = emptyList()), region)
        assertFalse(cal.available)
        assertTrue(cal.notes.any { it.code == NoteCode.NO_CLIMATE_DATA })
    }

    @Test
    fun rainySeasonStartsArePreferred() {
        val millet = tomato.copy(
            id = "test-crop", calendar = emptyList(), sowingMode = SowingMode.DIRECT, water = WaterNeed.MEDIUM,
            temperature = TemperatureRange(min = 18.0, max = 35.0), cycleDays = Range(90.0, 100.0), frostTolerance = FrostTolerance.NONE,
        )
        val cal = engine.compute(millet, c.regionsById.getValue("sn-kao"), Methods.OPEN_FIELD)
        val sowing = cal.windows.single { it.activity == Activity.DIRECT_SOWING }
        assertTrue(sowing.preferredMonths.isNotEmpty())
        assertTrue(sowing.preferredMonths.all { it in listOf(6, 7) })
        assertTrue(cal.notes.any { it.code == NoteCode.RAINY_SEASON_ALIGNED })
    }

    @Test
    fun monthsHelpers() {
        assertEquals(1, Months.wrap(13)); assertEquals(12, Months.wrap(0)); assertEquals(11, Months.wrap(-1))
        assertEquals(listOf(11, 12, 1), Months.span(11, 3))
        assertEquals(listOf(4 to 4, 11 to 1), Months.ranges(setOf(11, 12, 1, 4)))
        assertEquals(listOf(1 to 12), Months.ranges(Months.ALL.toSet()))
    }

    @Test
    fun regionWithoutFrostDataFallsBackToMinima() {
        val r: Region = c.regionsById.getValue("fr-als").copy(frostMonths = null)
        val frost = engine.frostMonths(r)
        assertTrue(1 in frost && 7 !in frost)
    }
}
