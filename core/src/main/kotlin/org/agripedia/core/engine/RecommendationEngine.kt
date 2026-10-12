package org.agripedia.core.engine

import org.agripedia.core.model.Activity
import org.agripedia.core.model.Catalog
import org.agripedia.core.model.Difficulty
import org.agripedia.core.model.Lifecycle
import org.agripedia.core.model.Methods
import org.agripedia.core.model.Plant
import org.agripedia.core.model.Region

/** User experience level. */
enum class ExperienceLevel { BEGINNER, INTERMEDIATE, EXPERT }

/** Type of growing space / farm. */
enum class FarmType(val defaultMethod: String) {
    BALCONY(Methods.CONTAINER),
    GARDEN(Methods.OPEN_FIELD),
    SMALL_FARM(Methods.OPEN_FIELD),
    MEDIUM_FARM(Methods.OPEN_FIELD),
    LARGE_FARM(Methods.OPEN_FIELD),
}

enum class SuitabilityLevel {
    /** Suitable in the region and a sowing / planting action is possible in the requested period. */
    RECOMMENDED,
    /** Can be grown in the region, but not started in the requested period, or with precautions. */
    POSSIBLE,
    /** Requirements not met according to the available data. */
    NOT_RECOMMENDED,
    /** Not enough data to conclude. */
    UNKNOWN,
}

data class Recommendation(
    val plant: Plant,
    val level: SuitabilityLevel,
    /** 0..100, only used for ranking. */
    val score: Int,
    /** Activities possible during the requested months. */
    val activities: List<Activity>,
    val calendar: PlantCalendar,
    val notes: List<Note>,
)

data class RecommendationQuery(
    val region: Region,
    /** Months considered (one month, or the months of a season). */
    val months: Set<Int>,
    val methodId: String = Methods.OPEN_FIELD,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val farmType: FarmType? = null,
    val categoryId: String? = null,
)

/**
 * Rule-based recommendation engine. It only uses the dataset values and the rules
 * documented in docs/RECOMMENDATION_RULES.md; it never claims success is guaranteed.
 */
class RecommendationEngine(private val catalog: Catalog) {
    private val calendarEngine = CalendarEngine(catalog)

    fun evaluate(plant: Plant, query: RecommendationQuery): Recommendation {
        val region = query.region
        val notes = mutableListOf<Note>()
        val calendar = calendarEngine.compute(plant, region, query.methodId)
        notes += calendar.notes

        var blocking = false
        if (query.methodId !in plant.methods) {
            notes += Note(NoteCode.METHOD_NOT_SUITED)
            blocking = true
        }
        if (plant.climateZoneIds.isNotEmpty() && region.climateZoneId !in plant.climateZoneIds && query.methodId != Methods.HYDROPONICS) {
            notes += Note(NoteCode.CLIMATE_ZONE_NOT_LISTED)
        }
        if (plant.lifecycle == Lifecycle.PERENNIAL) blocking = blocking or perennialBlocking(plant, region, notes)

        val activities = query.months.flatMap { calendar.activitiesIn(it) }.distinct().sortedBy { it.ordinal }
        val starts = activities.filter { it in PlantCalendar.START_ACTIVITIES }

        val level = when {
            region.monthlyMeanTempC == null && plant.calendar.none { it.regionId == region.id || it.climateZoneId == region.climateZoneId } ->
                SuitabilityLevel.UNKNOWN
            blocking -> SuitabilityLevel.NOT_RECOMMENDED
            !calendar.available -> SuitabilityLevel.NOT_RECOMMENDED
            // Outside the climate zones where the crop is usually grown: never more than "possible".
            starts.isNotEmpty() && notes.none { it.code == NoteCode.CLIMATE_ZONE_NOT_LISTED } -> SuitabilityLevel.RECOMMENDED
            else -> SuitabilityLevel.POSSIBLE
        }

        if (query.experience == ExperienceLevel.BEGINNER && plant.difficulty == Difficulty.HARD) {
            notes += Note(NoteCode.DIFFICULT_FOR_BEGINNER)
        }

        var score = when (level) {
            SuitabilityLevel.RECOMMENDED -> 70
            SuitabilityLevel.POSSIBLE -> 40
            SuitabilityLevel.UNKNOWN -> 15
            SuitabilityLevel.NOT_RECOMMENDED -> 0
        }
        if (level != SuitabilityLevel.NOT_RECOMMENDED) {
            score += when (plant.difficulty) {
                Difficulty.EASY -> if (query.experience == ExperienceLevel.BEGINNER) 15 else 8
                Difficulty.MEDIUM -> 6
                Difficulty.HARD -> if (query.experience == ExperienceLevel.EXPERT) 6 else -10
            }
            if (calendar.windows.any { w -> w.activity in PlantCalendar.START_ACTIVITIES && w.preferredMonths.any { it in query.months } }) score += 10
            if (notes.any { it.code == NoteCode.CLIMATE_ZONE_NOT_LISTED }) score -= 10
            if (notes.any { it.code == NoteCode.IRRIGATION_REQUIRED && it.months.any { m -> m in query.months } }) score -= 3
        }
        return Recommendation(plant, level, score.coerceIn(0, 100), activities, calendar, notes.distinct())
    }

    /** Ranked recommendations; NOT_RECOMMENDED plants are excluded unless [includeAll]. */
    fun recommend(query: RecommendationQuery, includeAll: Boolean = false): List<Recommendation> {
        val method = if (query.farmType == FarmType.BALCONY) Methods.CONTAINER else query.methodId
        val q = query.copy(methodId = method)
        return catalog.plants
            .asSequence()
            .filter { q.categoryId == null || it.categoryId == q.categoryId }
            .map { evaluate(it, q) }
            .filter { includeAll || it.level != SuitabilityLevel.NOT_RECOMMENDED }
            .sortedWith(compareBy<Recommendation> { it.level.ordinal }.thenByDescending { it.score }.thenBy { it.plant.id })
            .toList()
    }

    private fun perennialBlocking(plant: Plant, region: Region, notes: MutableList<Note>): Boolean {
        var blocking = false
        val means = region.monthlyMeanTempC
        val mins = region.monthlyMinTempC
        plant.temperature.hardinessMin?.let { hardiness ->
            // Typical absolute minima are several degrees below the coldest monthly mean minimum.
            val coldest = mins?.min()?.minus(CalendarEngine.EXTREME_MIN_MARGIN_C) ?: means?.min()?.minus(CalendarEngine.EXTREME_MIN_MARGIN_C + 5)
            if (coldest != null && coldest < hardiness) {
                notes += Note(NoteCode.WINTER_TOO_COLD, value = hardiness)
                blocking = true
            }
        }
        val ph = plant.phenology
        if (ph?.requiresWinterChill == true && means != null) {
            val limit = ph.chillColdestMonthMaxC ?: 7.0
            if (means.min() > limit) {
                notes += Note(NoteCode.NO_WINTER_CHILL, value = limit)
                blocking = true
            }
        }
        plant.temperature.optimalMin?.let { opt ->
            if (means != null && means.max() < opt) {
                notes += Note(NoteCode.SUMMER_TOO_COOL, value = opt)
            }
        }
        return blocking
    }
}
