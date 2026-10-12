package org.agripedia.app

import org.agripedia.app.ui.components.parsePositiveDecimal
import org.agripedia.app.ui.crops.startActivitiesFor
import org.agripedia.core.model.Activity
import org.agripedia.core.seed.SeedLoader
import org.agripedia.core.seed.SeedReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UiLogicTest {
    @Test
    fun parsesDecimalsWithCommaOrDot() {
        assertEquals(12.5, parsePositiveDecimal("12,5")!!, 0.0)
        assertEquals(3.0, parsePositiveDecimal(" 3 ")!!, 0.0)
        assertNull(parsePositiveDecimal("-1"))
        assertNull(parsePositiveDecimal("abc"))
        assertNull(parsePositiveDecimal(""))
    }

    @Test
    fun bundledDatasetLoadsAndEveryPlantHasAStartActivity() {
        // Same files as the APK assets (assets srcDir = /data).
        val catalog = SeedLoader.load(SeedReader { File("../data/seed/$it").readText() })
        assertTrue(catalog.plants.isNotEmpty())
        catalog.plants.forEach { p ->
            val starts = startActivitiesFor(p)
            assertTrue(p.id, starts.isNotEmpty())
            assertTrue(p.id, starts.all { it in setOf(Activity.NURSERY_SOWING, Activity.DIRECT_SOWING, Activity.TRANSPLANTING, Activity.PLANTING) })
        }
    }
}
