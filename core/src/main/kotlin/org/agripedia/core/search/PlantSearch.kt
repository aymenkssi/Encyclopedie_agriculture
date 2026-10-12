package org.agripedia.core.search

import org.agripedia.core.engine.CalendarEngine
import org.agripedia.core.engine.PlantCalendar
import org.agripedia.core.model.Catalog
import org.agripedia.core.model.Difficulty
import org.agripedia.core.model.Lang
import org.agripedia.core.model.Plant
import org.agripedia.core.model.Region
import org.agripedia.core.model.WaterNeed

/** Cycle length buckets used by the filters. */
enum class CycleLength(val maxDays: Int) { SHORT(75), MEDIUM(150), LONG(Int.MAX_VALUE) }

/** Combinable filters (all non-null criteria must match). */
data class PlantFilter(
    val categoryIds: Set<String> = emptySet(),
    val difficulties: Set<Difficulty> = emptySet(),
    val waterNeeds: Set<WaterNeed> = emptySet(),
    val methodIds: Set<String> = emptySet(),
    val soilIds: Set<String> = emptySet(),
    val climateZoneIds: Set<String> = emptySet(),
    val cycle: CycleLength? = null,
    /** Region whose calendar must allow a start (sowing/planting) in one of [months]. */
    val region: Region? = null,
    val months: Set<Int> = emptySet(),
    /** Country: plants having explicit data or a computable calendar in at least one region of the country. */
    val countryId: String? = null,
) {
    val isEmpty: Boolean
        get() = categoryIds.isEmpty() && difficulties.isEmpty() && waterNeeds.isEmpty() && methodIds.isEmpty() &&
            soilIds.isEmpty() && climateZoneIds.isEmpty() && cycle == null && (region == null || months.isEmpty()) && countryId == null

    val activeCount: Int
        get() = listOf(categoryIds.isNotEmpty(), difficulties.isNotEmpty(), waterNeeds.isNotEmpty(), methodIds.isNotEmpty(),
            soilIds.isNotEmpty(), climateZoneIds.isNotEmpty(), cycle != null, region != null && months.isNotEmpty(), countryId != null).count { it }
}

data class SearchHit(val plant: Plant, val score: Double, val matchedName: String?)

/**
 * In-memory search over the catalog: tolerant to typos (OSA distance), accents and
 * Arabic orthographic variants, searching common names in every language, scientific
 * names, synonyms, family, category names and variety names.
 */
class PlantSearch(private val catalog: Catalog) {
    private val calendarEngine = CalendarEngine(catalog)

    private data class IndexEntry(val plant: Plant, val terms: List<Pair<String, String>>) // normalized -> original

    private val index: List<IndexEntry> = catalog.plants.map { p ->
        val originals = buildList {
            p.names.values.flatten().forEach { add(it) }
            add(p.scientificName)
            addAll(p.synonyms)
            add(p.family)
            catalog.categoriesById[p.categoryId]?.names?.values?.forEach { add(it) }
            p.varieties.forEach { add(it.name) }
        }
        IndexEntry(p, originals.map { TextNormalizer.normalize(it) to it }.filter { it.first.isNotEmpty() }.distinct())
    }

    fun search(query: String, filter: PlantFilter = PlantFilter(), lang: String = Lang.FR): List<SearchHit> {
        val q = TextNormalizer.normalize(query)
        val candidates = index.filter { matches(it.plant, filter) }
        if (q.isEmpty()) {
            return candidates.map { SearchHit(it.plant, 0.0, null) }.sortedBy { it.plant.displayName(lang).lowercase() }
        }
        return candidates.mapNotNull { entry ->
            var best = 0.0
            var bestName: String? = null
            for ((norm, original) in entry.terms) {
                val s = termScore(q, norm)
                if (s > best) { best = s; bestName = original }
            }
            if (best > 0) SearchHit(entry.plant, best, bestName) else null
        }.sortedWith(compareByDescending<SearchHit> { it.score }.thenBy { it.plant.displayName(lang) })
    }

    /** Scores how well normalized query [q] matches normalized term [t] (0 = no match). */
    internal fun termScore(q: String, t: String): Double {
        if (t == q) return 100.0
        if (t.startsWith(q)) return 90.0 - (t.length - q.length) * 0.1
        val words = t.split(' ')
        if (words.any { it.startsWith(q) }) return 80.0
        if (q.length >= 3 && t.contains(q)) return 70.0
        // Multi-word query: every query token must match some word.
        val qTokens = q.split(' ').filter { it.isNotEmpty() }
        if (qTokens.size > 1 && qTokens.all { qt -> words.any { w -> w.startsWith(qt) || fuzzy(qt, w) } }) return 65.0
        // Typo tolerance on whole term and on each word (also on prefixes of the same length).
        if (q.length >= 3) {
            val allowed = if (q.length <= 5) 1 else 2
            var best = Int.MAX_VALUE
            for (w in words + t) {
                best = minOf(best, EditDistance.osa(q, w, allowed))
                if (w.length > q.length) best = minOf(best, EditDistance.osa(q, w.substring(0, q.length), allowed))
            }
            if (best <= allowed) return 50.0 - best * 10
        }
        return 0.0
    }

    private fun fuzzy(q: String, w: String): Boolean {
        if (q.length < 3) return false
        val allowed = if (q.length <= 5) 1 else 2
        val target = if (w.length > q.length) w.substring(0, q.length) else w
        return EditDistance.osa(q, target, allowed) <= allowed
    }

    fun matches(p: Plant, f: PlantFilter): Boolean {
        if (f.categoryIds.isNotEmpty() && p.categoryId !in f.categoryIds) return false
        if (f.difficulties.isNotEmpty() && p.difficulty !in f.difficulties) return false
        if (f.waterNeeds.isNotEmpty() && p.water !in f.waterNeeds) return false
        if (f.methodIds.isNotEmpty() && p.methods.none { it in f.methodIds }) return false
        if (f.soilIds.isNotEmpty() && p.soilIds.none { it in f.soilIds }) return false
        if (f.climateZoneIds.isNotEmpty() && p.climateZoneIds.none { it in f.climateZoneIds }) return false
        f.cycle?.let { c ->
            val days = p.cycleDays?.min ?: return false
            val lower = CycleLength.entries.getOrNull(c.ordinal - 1)?.maxDays ?: 0
            if (days <= lower || days > c.maxDays) return false
        }
        if (f.region != null && f.months.isNotEmpty()) {
            val method = f.methodIds.singleOrNull() ?: p.methods.first()
            val cal: PlantCalendar = calendarEngine.compute(p, f.region, method)
            if (f.months.none { cal.startActivitiesIn(it).isNotEmpty() }) return false
        }
        f.countryId?.let { cid ->
            val regions = catalog.countriesById[cid]?.regions.orEmpty()
            if (regions.isEmpty()) return false
            val zones = regions.map { it.climateZoneId }.toSet()
            val ok = p.calendar.any { it.regionId in regions.map { r -> r.id } } ||
                p.climateZoneIds.isEmpty() || p.climateZoneIds.any { it in zones }
            if (!ok) return false
        }
        return true
    }
}
