package org.agripedia.core.engine

import org.agripedia.core.model.Activity
import org.agripedia.core.model.Lifecycle
import org.agripedia.core.model.Plant
import org.agripedia.core.model.SowingMode
import java.time.LocalDate
import kotlin.math.roundToLong

/** A task proposed from the user's own crop dates (never a universal reminder). */
data class PlannedTask(val activity: Activity, val date: LocalDate, val estimated: Boolean)

/**
 * Derives milestone tasks for a crop from the date the user actually sowed or
 * planted it, using the plant's own cycle data. When data is missing no task is
 * invented.
 */
object TaskPlanner {
    const val HEALTH_CHECK_INTERVAL_DAYS = 14L
    const val MAX_HEALTH_CHECKS = 8

    fun plan(plant: Plant, startActivity: Activity, startDate: LocalDate, today: LocalDate = LocalDate.now()): List<PlannedTask> {
        val tasks = mutableListOf<PlannedTask>()
        var inPlaceDate = startDate
        if (startActivity == Activity.NURSERY_SOWING &&
            (plant.sowingMode == SowingMode.TRANSPLANT || plant.sowingMode == SowingMode.BOTH)
        ) {
            val weeks = plant.nurseryWeeks?.min ?: return tasks // no data -> no invented date
            inPlaceDate = startDate.plusDays((weeks * 7).roundToLong())
            tasks += PlannedTask(Activity.TRANSPLANTING, inPlaceDate, estimated = true)
        }
        val cycle = plant.cycleDays
        if (cycle != null && plant.lifecycle != Lifecycle.PERENNIAL) {
            val harvestStart = inPlaceDate.plusDays(cycle.min.roundToLong())
            tasks += PlannedTask(Activity.HARVEST, harvestStart, estimated = true)
            var d = inPlaceDate.plusDays(HEALTH_CHECK_INTERVAL_DAYS)
            var n = 0
            while (d.isBefore(harvestStart) && n < MAX_HEALTH_CHECKS) {
                tasks += PlannedTask(Activity.HEALTH_MONITORING, d, estimated = true)
                d = d.plusDays(HEALTH_CHECK_INTERVAL_DAYS); n++
            }
            tasks += PlannedTask(Activity.ROTATION_PLANNING, inPlaceDate.plusDays(cycle.max.roundToLong()), estimated = true)
        }
        return tasks.filter { !it.date.isBefore(today) }.sortedBy { it.date }
    }
}
