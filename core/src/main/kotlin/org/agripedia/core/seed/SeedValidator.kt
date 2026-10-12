package org.agripedia.core.seed

import org.agripedia.core.model.Catalog
import org.agripedia.core.model.Lang
import org.agripedia.core.model.Lifecycle
import org.agripedia.core.model.LocalizedText
import org.agripedia.core.model.Methods
import org.agripedia.core.model.ValidationStatus

/**
 * Checks the integrity of a dataset before it is shipped or imported:
 * referential integrity, required translations, plausible numeric ranges and
 * honesty rules (demo data must be flagged, "verified" needs a source).
 */
object SeedValidator {
    data class Issue(val severity: Severity, val where: String, val message: String) {
        override fun toString() = "[$severity] $where: $message"
    }
    enum class Severity { ERROR, WARNING }

    fun validate(c: Catalog): List<Issue> {
        val issues = mutableListOf<Issue>()
        fun err(w: String, m: String) { issues += Issue(Severity.ERROR, w, m) }
        fun warn(w: String, m: String) { issues += Issue(Severity.WARNING, w, m) }
        fun requireLangs(w: String, t: LocalizedText, field: String) {
            Lang.SUPPORTED.filter { t[it].isNullOrBlank() }.forEach { warn(w, "$field missing '$it' translation") }
        }

        // Unique ids
        fun dupes(kind: String, ids: List<String>) = ids.groupBy { it }.filter { it.value.size > 1 }.keys
            .forEach { err(kind, "duplicate id '$it'") }
        dupes("plant", c.plants.map { it.id })
        dupes("region", c.geography.countries.flatMap { it.regions }.map { it.id })
        dupes("country", c.geography.countries.map { it.id })
        dupes("source", c.reference.sources.map { it.id })
        dupes("disease", c.health.diseases.map { it.id })
        dupes("pest", c.health.pests.map { it.id })
        dupes("treatment", c.health.treatments.map { it.id })

        val sourceIds = c.sourcesById.keys
        fun checkSources(w: String, ids: List<String>, status: ValidationStatus) {
            ids.filter { it !in sourceIds }.forEach { err(w, "unknown source '$it'") }
            if (status == ValidationStatus.VERIFIED && ids.isEmpty()) err(w, "status 'verified' requires at least one source")
        }

        c.reference.methods.map { it.id }.let { ids -> Methods.ALL.filter { it !in ids }.forEach { err("reference", "missing method '$it'") } }

        for (country in c.geography.countries) {
            requireLangs("country ${country.id}", country.names, "names")
            for (r in country.regions) {
                val w = "region ${r.id}"
                if (r.countryId != country.id) err(w, "countryId '${r.countryId}' != parent '${country.id}'")
                if (r.climateZoneId !in c.climateZonesById) err(w, "unknown climate zone '${r.climateZoneId}'")
                requireLangs(w, r.names, "names")
                r.dominantSoilIds.filter { it !in c.soilTypesById }.forEach { err(w, "unknown soil '$it'") }
                checkSources(w, r.sourceIds, r.climateStatus)
                if (r.latitude !in -90.0..90.0 || r.longitude !in -180.0..180.0) err(w, "invalid coordinates")
                r.monthlyMeanTempC?.let { t ->
                    r.monthlyMinTempC?.let { mn -> (0 until 12).filter { mn[it] > t[it] }.forEach { m -> err(w, "min > mean in month ${m + 1}") } }
                    if (t.any { it < -40 || it > 45 }) err(w, "implausible mean temperature")
                }
                r.monthlyPrecipMm?.let { p -> if (p.any { it < 0 || it > 1500 }) err(w, "implausible precipitation") }
                (r.frostMonths.orEmpty() + r.rainySeasonMonths.orEmpty()).filter { it !in 1..12 }.forEach { err(w, "invalid month $it") }
                if (r.monthlyMeanTempC == null) warn(w, "no climate normals: calendars cannot be computed")
            }
        }

        val plantIds = c.plantsById.keys
        for (p in c.plants) {
            val w = "plant ${p.id}"
            if (!Regex("^[a-z0-9-]+$").matches(p.id)) err(w, "id must be a lowercase slug")
            if (p.categoryId !in c.categoriesById) err(w, "unknown category '${p.categoryId}'")
            Lang.SUPPORTED.filter { p.names[it].isNullOrEmpty() }.forEach { err(w, "missing common name in '$it'") }
            requireLangs(w, p.description, "description")
            p.soilIds.filter { it !in c.soilTypesById }.forEach { err(w, "unknown soil '$it'") }
            p.methods.filter { it !in c.methodsById }.forEach { err(w, "unknown method '$it'") }
            p.climateZoneIds.filter { it !in c.climateZonesById }.forEach { err(w, "unknown climate zone '$it'") }
            p.diseaseIds.filter { it !in c.diseasesById }.forEach { err(w, "unknown disease '$it'") }
            p.pestIds.filter { it !in c.pestsById }.forEach { err(w, "unknown pest '$it'") }
            p.companions.filter { it.otherPlantId !in plantIds }.forEach { warn(w, "companion '${it.otherPlantId}' not in catalog") }
            p.companions.filter { it.otherPlantId == p.id }.forEach { err(w, "plant cannot be its own companion") }
            checkSources(w, p.sourceIds, p.status)
            if (c.manifest.demo && !p.isDemo) err(w, "demo dataset: plant must be flagged isDemo=true")
            if (p.isDemo && p.status == ValidationStatus.VERIFIED) err(w, "demo content cannot be marked verified")
            p.temperature.run {
                val ordered = listOfNotNull(min, optimalMin, optimalMax, max)
                if (ordered != ordered.sorted()) err(w, "temperatures must satisfy min <= optimalMin <= optimalMax <= max")
            }
            p.ph?.let { if (it.min < 3.0 || it.max > 10.0) err(w, "implausible pH range") }
            if (p.lifecycle == Lifecycle.PERENNIAL && p.phenology == null && p.calendar.isEmpty())
                warn(w, "perennial without phenology or calendar")
            if (p.lifecycle != Lifecycle.PERENNIAL && p.cycleDays == null) warn(w, "annual without cycleDays")
            p.calendar.forEach { e ->
                if (e.regionId != null && e.regionId !in c.regionsById) err(w, "calendar: unknown region '${e.regionId}'")
                if (e.climateZoneId != null && e.climateZoneId !in c.climateZonesById) err(w, "calendar: unknown zone '${e.climateZoneId}'")
                if (e.regionId == null && e.climateZoneId == null) err(w, "calendar entry must be scoped to a region or a climate zone")
                if (e.varietyId != null && p.varieties.none { it.id == e.varietyId }) err(w, "calendar: unknown variety '${e.varietyId}'")
                checkSources("$w calendar", e.sourceIds, e.status)
            }
            p.guides.forEach { g ->
                if (g.stages.map { it.order } != g.stages.map { it.order }.sorted()) err(w, "guide ${g.id}: stages not ordered")
                g.stages.forEach { s -> requireLangs("$w guide ${g.id} stage ${s.order}", s.title, "title"); requireLangs("$w guide ${g.id} stage ${s.order}", s.body, "body") }
            }
        }
        for (d in c.health.diseases) {
            d.treatmentIds.filter { it !in c.treatmentsById }.forEach { err("disease ${d.id}", "unknown treatment '$it'") }
            checkSources("disease ${d.id}", d.sourceIds, d.status)
            requireLangs("disease ${d.id}", d.names, "names")
        }
        for (pe in c.health.pests) {
            pe.treatmentIds.filter { it !in c.treatmentsById }.forEach { err("pest ${pe.id}", "unknown treatment '$it'") }
            checkSources("pest ${pe.id}", pe.sourceIds, pe.status)
            requireLangs("pest ${pe.id}", pe.names, "names")
        }
        for (t in c.health.treatments) {
            checkSources("treatment ${t.id}", t.sourceIds, t.status)
            if (t.type == org.agripedia.core.model.TreatmentType.CHEMICAL && t.caution.isEmpty())
                err("treatment ${t.id}", "chemical treatment requires a caution text")
        }
        return issues
    }
}
