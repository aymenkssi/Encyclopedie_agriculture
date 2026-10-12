package org.agripedia.core

import org.agripedia.core.engine.RegionLocator
import org.agripedia.core.engine.TaskPlanner
import org.agripedia.core.model.Activity
import org.agripedia.core.sync.SyncMerger
import org.agripedia.core.sync.SyncMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SyncAndTaskTest {
    @Test
    fun lastWriterWinsAndNoDuplicates() {
        val local = listOf(SyncMeta("a", 10, false, "d1"), SyncMeta("b", 5, false, "d1"), SyncMeta("c", 1, false, "d1"))
        val remote = listOf(SyncMeta("a", 5, false, "d2"), SyncMeta("b", 7, true, "d2"), SyncMeta("d", 3, false, "d2"))
        val r = SyncMerger.merge(local, remote)
        assertEquals(setOf("a", "c"), r.pushRemote.map { it.id }.toSet())
        assertEquals(setOf("b", "d"), r.applyLocally.map { it.id }.toSet())
        assertTrue(r.applyLocally.first { it.id == "b" }.deleted)
    }

    @Test
    fun deletionWinsOnTieAndIdenticalTombstonesArePurgeable() {
        val r = SyncMerger.merge(listOf(SyncMeta("x", 9, false, "d1")), listOf(SyncMeta("x", 9, true, "d2")))
        assertEquals(listOf("x"), r.applyLocally.map { it.id })
        val p = SyncMerger.merge(listOf(SyncMeta("y", 4, true, "d1")), listOf(SyncMeta("y", 4, true, "d1")))
        assertEquals(listOf("y"), p.purgeable)
    }

    @Test
    fun taskPlannerUsesUserDatesAndPlantData() {
        val tomato = TestData.catalog.plantsById.getValue("solanum-lycopersicum")
        val start = LocalDate.of(2026, 3, 1)
        val tasks = TaskPlanner.plan(tomato, Activity.NURSERY_SOWING, start, today = start)
        val transplant = tasks.first { it.activity == Activity.TRANSPLANTING }
        assertEquals(start.plusDays(35), transplant.date)
        val harvest = tasks.first { it.activity == Activity.HARVEST }
        assertEquals(transplant.date.plusDays(60), harvest.date)
        assertTrue(tasks.count { it.activity == Activity.HEALTH_MONITORING } in 1..TaskPlanner.MAX_HEALTH_CHECKS)
        assertTrue(tasks.all { it.estimated })
    }

    @Test
    fun taskPlannerDoesNotInventDatesWithoutData() {
        val tomato = TestData.catalog.plantsById.getValue("solanum-lycopersicum").copy(nurseryWeeks = null)
        assertTrue(TaskPlanner.plan(tomato, Activity.NURSERY_SOWING, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1)).isEmpty())
    }

    @Test
    fun nearestRegion() {
        val (region, km) = RegionLocator.nearest(TestData.catalog, 48.2, -1.5)!!
        assertEquals("fr-bre", region.id)
        assertTrue(km < 50)
        assertEquals(null, RegionLocator.nearest(TestData.catalog, -80.0, 0.0))
    }
}
