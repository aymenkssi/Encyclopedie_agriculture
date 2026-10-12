package org.agripedia.core.engine

import org.agripedia.core.model.Hemisphere
import org.agripedia.core.model.Region

/** Helpers for month arithmetic (months are 1..12, wrapping around the year). */
object Months {
    val ALL: List<Int> = (1..12).toList()

    fun wrap(month: Int): Int = ((month - 1) % 12 + 12) % 12 + 1

    fun shift(months: Collection<Int>, by: Int): Set<Int> = months.map { wrap(it + by) }.toSet()

    /** Consecutive months starting at [start] (inclusive), [count] long, wrapping. */
    fun span(start: Int, count: Int): List<Int> = (0 until count).map { wrap(start + it) }

    /** Groups a set of months into contiguous, wrap-aware ranges, e.g. {11,12,1,4} -> [(11..1),(4..4)]. */
    fun ranges(months: Set<Int>): List<Pair<Int, Int>> {
        if (months.isEmpty()) return emptyList()
        if (months.size == 12) return listOf(1 to 12)
        // Start from a month whose predecessor is not in the set.
        val starts = months.filter { wrap(it - 1) !in months }.sorted()
        return starts.map { s ->
            var e = s
            while (wrap(e + 1) in months) e = wrap(e + 1)
            s to e
        }
    }
}

/** Seasons offered in the UI. Tropical regions use rainy / dry seasons. */
enum class Season { SPRING, SUMMER, AUTUMN, WINTER, RAINY, DRY;

    companion object {
        /** Seasons that make sense for a region. */
        fun availableFor(region: Region?): List<Season> =
            if (region?.rainySeasonMonths?.isNotEmpty() == true) listOf(RAINY, DRY, SPRING, SUMMER, AUTUMN, WINTER)
            else listOf(SPRING, SUMMER, AUTUMN, WINTER)

        /** Season containing [month] for the region (rainy/dry first for tropical regions). */
        fun current(month: Int, region: Region?, hemisphere: Hemisphere): Season {
            val rainy = region?.rainySeasonMonths
            if (!rainy.isNullOrEmpty()) return if (month in rainy) RAINY else DRY
            return entries.first { it != RAINY && it != DRY && month in monthsOf(it, region, hemisphere) }
        }

        /** Calendar months covered by a season. Astronomical seasons are shifted by 6 months in the south. */
        fun monthsOf(season: Season, region: Region?, hemisphere: Hemisphere): Set<Int> {
            val north = when (season) {
                SPRING -> setOf(3, 4, 5)
                SUMMER -> setOf(6, 7, 8)
                AUTUMN -> setOf(9, 10, 11)
                WINTER -> setOf(12, 1, 2)
                RAINY -> return region?.rainySeasonMonths?.toSet() ?: emptySet()
                DRY -> return region?.rainySeasonMonths?.takeIf { it.isNotEmpty() }
                    ?.let { r -> Months.ALL.filter { it !in r }.toSet() } ?: emptySet()
            }
            return if (hemisphere == Hemisphere.SOUTH) Months.shift(north, 6) else north
        }
    }
}
