package org.agripedia.core.engine

import org.agripedia.core.model.Activity
import org.agripedia.core.model.Catalog
import org.agripedia.core.model.FrostTolerance
import org.agripedia.core.model.Hemisphere
import org.agripedia.core.model.Lifecycle
import org.agripedia.core.model.Methods
import org.agripedia.core.model.Plant
import org.agripedia.core.model.Region
import org.agripedia.core.model.SeasonalCalendarEntry
import org.agripedia.core.model.SowingMode
import org.agripedia.core.model.ValidationStatus
import org.agripedia.core.model.WaterNeed
import kotlin.math.ceil
import kotlin.math.floor

/** Where a calendar window comes from. */
enum class CalendarOrigin {
    /** Explicit entry of the dataset scoped to the region. */
    EXPLICIT_REGION,
    /** Explicit entry scoped to the climate zone (and hemisphere). */
    EXPLICIT_ZONE,
    /** Computed from the plant requirements and the regional climate normals. */
    COMPUTED,
    /** Perennial phenology, shifted for the hemisphere. */
    PHENOLOGY,
    /** Controlled environment (hydroponics): not climate dependent. */
    CONTROLLED,
}

/**
 * Machine-readable explanation attached to calendars and recommendations.
 * The UI turns these codes into localized sentences; the engine never produces
 * human text so that every language gets the same reasoning.
 */
enum class NoteCode {
    NO_CLIMATE_DATA,
    NO_CYCLE_DATA,
    GREENHOUSE_OFFSET_ASSUMED,
    CONTROLLED_ENVIRONMENT,
    NURSERY_SHELTERED_ASSUMED,
    FROST_RISK_MONTHS,
    IRRIGATION_REQUIRED,
    RAINY_SEASON_ALIGNED,
    NO_WINTER_CHILL,
    WINTER_TOO_COLD,
    SUMMER_TOO_COOL,
    TOO_HOT_MONTHS,
    SOUTHERN_HEMISPHERE_SHIFT,
    TROPICAL_PERENNIAL_VARIABLE,
    NO_WINDOW_FOUND,
    METHOD_NOT_SUITED,
    CLIMATE_ZONE_NOT_LISTED,
    DIFFICULT_FOR_BEGINNER,
    EXPLICIT_DATA_USED,
    INDICATIVE_ONLY,
}

data class Note(val code: NoteCode, val months: Set<Int> = emptySet(), val value: Double? = null)

data class ActivityWindow(
    val activity: Activity,
    val months: Set<Int>,
    /** Subset of [months] that is preferable (e.g. aligned with the rainy season). */
    val preferredMonths: Set<Int> = emptySet(),
    val origin: CalendarOrigin,
    val status: ValidationStatus,
)

data class PlantCalendar(
    val plantId: String,
    val regionId: String,
    val methodId: String,
    /** False when the calendar could not be computed (missing data). */
    val available: Boolean,
    val windows: List<ActivityWindow>,
    val notes: List<Note>,
) {
    fun monthsFor(activity: Activity): Set<Int> = windows.filter { it.activity == activity }.flatMap { it.months }.toSet()
    fun activitiesIn(month: Int): List<Activity> = windows.filter { month in it.months }.map { it.activity }.distinct()

    /** Activities that start a crop (sowing / planting). */
    fun startActivitiesIn(month: Int): List<Activity> = activitiesIn(month).filter { it in START_ACTIVITIES }

    /** Overall confidence: the weakest status among windows. */
    val status: ValidationStatus
        get() = windows.map { it.status }.maxByOrNull { it.ordinal } ?: ValidationStatus.TO_VERIFY

    companion object {
        val START_ACTIVITIES = setOf(Activity.NURSERY_SOWING, Activity.DIRECT_SOWING, Activity.TRANSPLANTING, Activity.PLANTING)
    }
}

/**
 * Computes indicative cultivation calendars for a plant in a region.
 *
 * Priority: explicit regional entries > explicit climate-zone entries > computed windows.
 * The computation never relies on calendar seasons alone: it checks, month by month,
 * the regional mean temperatures, frost risk, precipitation and rainy season against the
 * plant requirements over the whole crop cycle. All results are indicative.
 */
class CalendarEngine(private val catalog: Catalog) {

    companion object {
        /** Assumed mean warming provided by an unheated greenhouse / tunnel (°C). */
        const val GREENHOUSE_OFFSET_C = 4.0
        /** Mean daily minimum below which frost is considered possible during the month (°C). */
        const val FROST_MIN_THRESHOLD_C = 2.0
        /** Fallback when only mean temperatures are known. */
        const val FROST_MEAN_THRESHOLD_C = 7.0
        /** Gap between the coldest monthly mean minimum and a typical absolute winter minimum (°C). */
        const val EXTREME_MIN_MARGIN_C = 7.0
        /** Default cycle used when the plant has no cycle data (months). */
        const val DEFAULT_CYCLE_MONTHS = 3

        fun rainfedThresholdMm(water: WaterNeed): Double = when (water) {
            WaterNeed.LOW -> 20.0
            WaterNeed.MEDIUM -> 50.0
            WaterNeed.HIGH -> 80.0
        }
    }

    fun compute(plant: Plant, region: Region, methodId: String = Methods.OPEN_FIELD, varietyId: String? = null): PlantCalendar {
        val hemisphere = catalog.hemisphereOf(region)
        val notes = mutableListOf<Note>()

        val base: List<ActivityWindow> = when {
            methodId == Methods.HYDROPONICS -> controlled(plant, notes)
            plant.lifecycle == Lifecycle.PERENNIAL && plant.phenology != null -> perennial(plant, region, hemisphere, notes)
            else -> annual(plant, region, methodId, notes)
        }

        // Explicit data replaces computed windows activity by activity.
        val explicit = explicitWindows(plant, region, hemisphere, methodId, varietyId)
        val windows = if (explicit.isEmpty()) base else {
            notes += Note(NoteCode.EXPLICIT_DATA_USED)
            val replaced = explicit.map { it.activity }.toSet()
            base.filter { it.activity !in replaced } + explicit
        }
        val available = windows.isNotEmpty()
        if (!available && notes.none { it.code == NoteCode.NO_CLIMATE_DATA }) notes += Note(NoteCode.NO_WINDOW_FOUND)
        notes += Note(NoteCode.INDICATIVE_ONLY)
        return PlantCalendar(plant.id, region.id, methodId, available, windows.sortedBy { it.activity.ordinal }, notes.distinct())
    }

    // ---------------------------------------------------------------- explicit

    private fun explicitWindows(
        plant: Plant, region: Region, hemisphere: Hemisphere, methodId: String, varietyId: String?,
    ): List<ActivityWindow> {
        fun SeasonalCalendarEntry.matchesFilters() =
            (this.methodId == null || this.methodId == methodId) &&
                (this.varietyId == null || this.varietyId == varietyId)

        val regional = plant.calendar.filter { it.regionId == region.id && it.matchesFilters() }
        val entries: List<SeasonalCalendarEntry>
        val origin: CalendarOrigin
        if (regional.isNotEmpty()) {
            entries = regional; origin = CalendarOrigin.EXPLICIT_REGION
        } else {
            entries = plant.calendar.filter {
                it.regionId == null && it.climateZoneId == region.climateZoneId &&
                    (it.hemisphere == null || it.hemisphere == hemisphere) && it.matchesFilters()
            }
            origin = CalendarOrigin.EXPLICIT_ZONE
        }
        // Most specific entries first (variety / method), then merge per activity.
        return entries.groupBy { it.activity }.map { (activity, list) ->
            val specific = list.filter { it.varietyId != null || it.methodId != null }.ifEmpty { list }
            ActivityWindow(
                activity = activity,
                months = specific.flatMap { it.months }.toSet(),
                origin = origin,
                status = specific.maxOf { it.status },
            )
        }
    }

    // ---------------------------------------------------------------- controlled

    private fun controlled(plant: Plant, notes: MutableList<Note>): List<ActivityWindow> {
        notes += Note(NoteCode.CONTROLLED_ENVIRONMENT)
        val start = if (plant.sowingMode == SowingMode.VEGETATIVE) Activity.PLANTING else Activity.DIRECT_SOWING
        val all = Months.ALL.toSet()
        return listOf(
            ActivityWindow(start, all, origin = CalendarOrigin.CONTROLLED, status = ValidationStatus.INDICATIVE),
            ActivityWindow(Activity.HARVEST, all, origin = CalendarOrigin.CONTROLLED, status = ValidationStatus.INDICATIVE),
            ActivityWindow(Activity.HEALTH_MONITORING, all, origin = CalendarOrigin.CONTROLLED, status = ValidationStatus.INDICATIVE),
        )
    }

    // ---------------------------------------------------------------- climate helpers

    /** Effective monthly mean temperature (index 1..12). */
    private fun effectiveTemps(region: Region, methodId: String): List<Double>? {
        val t = region.monthlyMeanTempC ?: return null
        val offset = if (methodId == Methods.GREENHOUSE) GREENHOUSE_OFFSET_C else 0.0
        return t.map { it + offset }
    }

    /** Months with frost risk, estimated from explicit data, monthly minima or monthly means. */
    fun frostMonths(region: Region, methodId: String = Methods.OPEN_FIELD): Set<Int> {
        val offset = if (methodId == Methods.GREENHOUSE) GREENHOUSE_OFFSET_C else 0.0
        if (methodId != Methods.GREENHOUSE) region.frostMonths?.let { return it.toSet() }
        region.monthlyMinTempC?.let { mins ->
            return Months.ALL.filter { mins[it - 1] + offset < FROST_MIN_THRESHOLD_C }.toSet()
        }
        region.monthlyMeanTempC?.let { means ->
            return Months.ALL.filter { means[it - 1] + offset < FROST_MEAN_THRESHOLD_C }.toSet()
        }
        return emptySet()
    }

    private fun isGrowable(plant: Plant, month: Int, temps: List<Double>, frost: Set<Int>, region: Region, offset: Double): Boolean {
        val t = temps[month - 1]
        val tr = plant.temperature
        if (tr.min != null && t < tr.min) return false
        if (tr.max != null && t > tr.max) return false
        if (month in frost) {
            when (plant.frostTolerance) {
                FrostTolerance.NONE -> return false
                FrostTolerance.LIGHT -> {
                    val mins = region.monthlyMinTempC
                    if (mins != null && mins[month - 1] + offset < -3.0) return false
                }
                FrostTolerance.HARDY -> Unit
            }
        }
        return true
    }

    // ---------------------------------------------------------------- annual crops

    private fun annual(plant: Plant, region: Region, methodId: String, notes: MutableList<Note>): List<ActivityWindow> {
        val temps = effectiveTemps(region, methodId)
        if (temps == null) {
            notes += Note(NoteCode.NO_CLIMATE_DATA)
            return emptyList()
        }
        val offset = if (methodId == Methods.GREENHOUSE) GREENHOUSE_OFFSET_C else 0.0
        if (offset > 0) notes += Note(NoteCode.GREENHOUSE_OFFSET_ASSUMED, value = GREENHOUSE_OFFSET_C)

        val frost = frostMonths(region, methodId)
        if (frost.isNotEmpty() && plant.frostTolerance != FrostTolerance.HARDY) notes += Note(NoteCode.FROST_RISK_MONTHS, frost)
        plant.temperature.max?.let { max ->
            val hot = Months.ALL.filter { temps[it - 1] > max }.toSet()
            if (hot.isNotEmpty()) notes += Note(NoteCode.TOO_HOT_MONTHS, hot, max)
        }

        val cycle = plant.cycleDays
        if (cycle == null) notes += Note(NoteCode.NO_CYCLE_DATA)
        val cycleMonths = cycle?.let { ceil(((it.min + it.max) / 2.0) / 30.0).toInt().coerceIn(1, 12) } ?: DEFAULT_CYCLE_MONTHS

        val growable = Months.ALL.filter { isGrowable(plant, it, temps, frost, region, offset) }.toSet()
        // A start month is feasible when every month of the (average) cycle is growable.
        val feasibleStarts = Months.ALL.filter { s -> Months.span(s, cycleMonths).all { it in growable } }
            .toSet()
            .let { if (cycleMonths >= 12 && growable.size == 12) Months.ALL.toSet() else it }
        val germinationOk = Months.ALL.filter { m ->
            plant.temperature.germinationMin?.let { temps[m - 1] >= it } ?: true
        }.toSet()

        // Rainy season preference (tropical / semi-arid regions, rain-fed open field).
        val rainy = region.rainySeasonMonths?.toSet().orEmpty()
        val rainfed = methodId == Methods.OPEN_FIELD || methodId == Methods.CONTAINER
        fun preferred(starts: Set<Int>): Set<Int> {
            if (rainy.isEmpty() || !rainfed) return emptySet()
            // Starting during the first months of the rainy season lets the crop use the rains.
            val early = rainy.filter { Months.wrap(it - 1) !in rainy || Months.wrap(it - 2) !in rainy }.toSet()
            return starts.intersect(early)
        }

        val windows = mutableListOf<ActivityWindow>()
        val status = ValidationStatus.INDICATIVE
        val startMonths = mutableSetOf<Int>()
        val plantingMonths = mutableSetOf<Int>()

        val direct = feasibleStarts.intersect(germinationOk)
        if ((plant.sowingMode == SowingMode.DIRECT || plant.sowingMode == SowingMode.BOTH) && direct.isNotEmpty()) {
            windows += ActivityWindow(Activity.DIRECT_SOWING, direct, preferred(direct), CalendarOrigin.COMPUTED, status)
            startMonths += direct
        }
        if ((plant.sowingMode == SowingMode.TRANSPLANT || plant.sowingMode == SowingMode.BOTH) && feasibleStarts.isNotEmpty()) {
            windows += ActivityWindow(Activity.TRANSPLANTING, feasibleStarts, preferred(feasibleStarts), CalendarOrigin.COMPUTED, status)
            val weeks = plant.nurseryWeeks?.let { (it.min + it.max) / 2.0 } ?: 6.0
            val back = ceil(weeks / 4.345).toInt().coerceAtLeast(1)
            windows += ActivityWindow(Activity.NURSERY_SOWING, Months.shift(feasibleStarts, -back), emptySet(), CalendarOrigin.COMPUTED, status)
            notes += Note(NoteCode.NURSERY_SHELTERED_ASSUMED)
            startMonths += feasibleStarts
            plantingMonths += feasibleStarts
        }
        if (plant.sowingMode == SowingMode.VEGETATIVE && direct.isNotEmpty()) {
            windows += ActivityWindow(Activity.PLANTING, direct, preferred(direct), CalendarOrigin.COMPUTED, status)
            startMonths += direct
        }
        if (startMonths.isEmpty()) return emptyList()
        plantingMonths += startMonths

        // Harvest: from start + min cycle to start + max cycle.
        val harvest = mutableSetOf<Int>()
        val growing = mutableSetOf<Int>()
        for (s in plantingMonths) {
            val minM = cycle?.let { floor(it.min / 30.0).toInt() } ?: cycleMonths
            val maxM = cycle?.let { floor(it.max / 30.0).toInt() } ?: cycleMonths
            for (k in minM..maxOf(minM, maxM)) harvest += Months.wrap(s + k)
            for (k in 0 until cycleMonths) growing += Months.wrap(s + k)
        }
        windows += ActivityWindow(Activity.HARVEST, harvest, emptySet(), CalendarOrigin.COMPUTED, status)
        windows += ActivityWindow(Activity.SOIL_PREPARATION, Months.shift(plantingMonths, -1) - plantingMonths, emptySet(), CalendarOrigin.COMPUTED, status)
        windows += ActivityWindow(Activity.HEALTH_MONITORING, growing, emptySet(), CalendarOrigin.COMPUTED, status)
        windows += ActivityWindow(Activity.FERTILIZING, growing, emptySet(), CalendarOrigin.COMPUTED, status)
        windows += ActivityWindow(Activity.ROTATION_PLANNING, Months.shift(harvest, 1) - harvest, emptySet(), CalendarOrigin.COMPUTED, status)

        // Watering need: growing months where rainfall is below the plant threshold.
        region.monthlyPrecipMm?.let { p ->
            val threshold = rainfedThresholdMm(plant.water)
            val dry = growing.filter { methodId != Methods.OPEN_FIELD || p[it - 1] < threshold }.toSet()
            if (dry.isNotEmpty()) {
                windows += ActivityWindow(Activity.WATERING, dry, emptySet(), CalendarOrigin.COMPUTED, status)
                if (methodId == Methods.OPEN_FIELD) notes += Note(NoteCode.IRRIGATION_REQUIRED, dry)
            }
        } ?: run {
            windows += ActivityWindow(Activity.WATERING, growing, emptySet(), CalendarOrigin.COMPUTED, status)
        }
        if (windows.any { it.preferredMonths.isNotEmpty() }) notes += Note(NoteCode.RAINY_SEASON_ALIGNED, rainy)
        return windows
    }

    // ---------------------------------------------------------------- perennials

    private fun perennial(plant: Plant, region: Region, hemisphere: Hemisphere, notes: MutableList<Note>): List<ActivityWindow> {
        val ph = plant.phenology ?: return emptyList()
        val shift = if (hemisphere == Hemisphere.SOUTH) 6 else 0
        if (shift != 0) notes += Note(NoteCode.SOUTHERN_HEMISPHERE_SHIFT)
        val means = region.monthlyMeanTempC
        val tropical = !region.rainySeasonMonths.isNullOrEmpty() || (means != null && means.min() >= 18.0)
        if (tropical) notes += Note(NoteCode.TROPICAL_PERENNIAL_VARIABLE)
        val status = if (tropical) ValidationStatus.TO_VERIFY else ValidationStatus.INDICATIVE
        val list = mutableListOf<ActivityWindow>()
        fun add(a: Activity, m: List<Int>) {
            if (m.isNotEmpty()) list += ActivityWindow(a, Months.shift(m, shift), emptySet(), CalendarOrigin.PHENOLOGY, status)
        }
        add(Activity.PLANTING, ph.plantingMonthsNorth)
        add(Activity.PRUNING, ph.pruningMonthsNorth)
        add(Activity.FLOWERING, ph.floweringMonthsNorth)
        add(Activity.HARVEST, ph.harvestMonthsNorth)
        add(Activity.SOIL_PREPARATION, Months.shift(ph.plantingMonthsNorth, -1).toList() - ph.plantingMonthsNorth.toSet())
        val active = (ph.floweringMonthsNorth + ph.harvestMonthsNorth).toSet()
        if (active.isNotEmpty()) {
            // Monitoring from flowering through harvest.
            val f = ph.floweringMonthsNorth.minOrNull() ?: active.min()
            val h = ph.harvestMonthsNorth.maxOrNull() ?: active.max()
            val span = Months.span(f, Months.wrap(h - f + 1))
            add(Activity.HEALTH_MONITORING, span)
            region.monthlyPrecipMm?.let { p ->
                val threshold = rainfedThresholdMm(plant.water)
                val dry = Months.shift(span, shift).filter { p[it - 1] < threshold }
                if (dry.isNotEmpty()) {
                    list += ActivityWindow(Activity.WATERING, dry.toSet(), emptySet(), CalendarOrigin.PHENOLOGY, status)
                    notes += Note(NoteCode.IRRIGATION_REQUIRED, dry.toSet())
                }
            }
        }
        return list
    }
}
