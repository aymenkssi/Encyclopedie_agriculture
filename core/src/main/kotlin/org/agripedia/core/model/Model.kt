package org.agripedia.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain model of the AgriPedia knowledge base.
 *
 * The same classes are used to (1) deserialize the seed dataset shipped with the
 * app (`data/seed`), (2) feed the recommendation / calendar engines and (3) be
 * mapped to the Room entities of the Android app. Identifiers are stable string
 * slugs (e.g. `solanum-lycopersicum`) so that user data (favorites, crops, notes)
 * survives content updates.
 */

/** Map of BCP-47 language code -> text, e.g. {"fr": "Tomate", "ar": "طماطم"}. */
typealias LocalizedText = Map<String, String>

/** Map of language code -> list of names (first one is the preferred name). */
typealias LocalizedNames = Map<String, List<String>>

object Lang {
    const val FR = "fr"
    const val EN = "en"
    const val AR = "ar"
    val SUPPORTED = listOf(FR, AR, EN)

    /** Resolves a localized text with fallback: requested -> en -> fr -> first available. */
    fun pick(text: LocalizedText?, lang: String): String? {
        if (text.isNullOrEmpty()) return null
        return text[lang] ?: text[EN] ?: text[FR] ?: text.values.firstOrNull()
    }

    fun pickNames(names: LocalizedNames?, lang: String): List<String> {
        if (names.isNullOrEmpty()) return emptyList()
        return names[lang]?.takeIf { it.isNotEmpty() }
            ?: names[EN]?.takeIf { it.isNotEmpty() }
            ?: names[FR]?.takeIf { it.isNotEmpty() }
            ?: names.values.firstOrNull { it.isNotEmpty() }
            ?: emptyList()
    }
}

/** Validation status of a piece of agronomic information. */
@Serializable
enum class ValidationStatus {
    /** Checked against a cited, authoritative source for the stated scope. */
    @SerialName("verified") VERIFIED,
    /** Plausible general guidance; must be adapted locally. */
    @SerialName("indicative") INDICATIVE,
    /** Not yet checked (e.g. demonstration data). */
    @SerialName("to_verify") TO_VERIFY,
}

@Serializable
enum class Hemisphere { @SerialName("north") NORTH, @SerialName("south") SOUTH }

@Serializable
enum class Lifecycle { @SerialName("annual") ANNUAL, @SerialName("biennial") BIENNIAL, @SerialName("perennial") PERENNIAL }

@Serializable
enum class Difficulty { @SerialName("easy") EASY, @SerialName("medium") MEDIUM, @SerialName("hard") HARD }

@Serializable
enum class FrostTolerance {
    /** Killed or severely damaged by any frost. */
    @SerialName("none") NONE,
    /** Tolerates light, short frosts (≈ down to -2/-3 °C). */
    @SerialName("light") LIGHT,
    /** Tolerates moderate to hard frosts. */
    @SerialName("hardy") HARDY,
}

@Serializable
enum class SunExposure { @SerialName("full_sun") FULL_SUN, @SerialName("partial_shade") PARTIAL_SHADE, @SerialName("shade") SHADE }

@Serializable
enum class WaterNeed { @SerialName("low") LOW, @SerialName("medium") MEDIUM, @SerialName("high") HIGH }

@Serializable
enum class SowingMode {
    /** Sown directly in place. */
    @SerialName("direct") DIRECT,
    /** Sown in nursery then transplanted. */
    @SerialName("transplant") TRANSPLANT,
    /** Either direct sowing or transplanting is common. */
    @SerialName("both") BOTH,
    /** Propagated from tubers, bulbs, runners, cuttings or nursery trees. */
    @SerialName("vegetative") VEGETATIVE,
}

/** Agricultural activities shown in calendars and used for tasks. */
@Serializable
enum class Activity {
    @SerialName("soil_preparation") SOIL_PREPARATION,
    @SerialName("nursery_sowing") NURSERY_SOWING,
    @SerialName("direct_sowing") DIRECT_SOWING,
    @SerialName("transplanting") TRANSPLANTING,
    @SerialName("planting") PLANTING,
    @SerialName("watering") WATERING,
    @SerialName("fertilizing") FERTILIZING,
    @SerialName("pruning") PRUNING,
    @SerialName("flowering") FLOWERING,
    @SerialName("health_monitoring") HEALTH_MONITORING,
    @SerialName("treatment") TREATMENT,
    @SerialName("harvest") HARVEST,
    @SerialName("rotation_planning") ROTATION_PLANNING,
}

/** Cultivation methods (ids used in the dataset). */
object Methods {
    const val OPEN_FIELD = "open_field"
    const val GREENHOUSE = "greenhouse"
    const val CONTAINER = "container"
    const val HYDROPONICS = "hydroponics"
    val ALL = listOf(OPEN_FIELD, GREENHOUSE, CONTAINER, HYDROPONICS)
}

@Serializable
data class DataSource(
    val id: String,
    val title: String,
    val publisher: String? = null,
    val url: String? = null,
    val license: String? = null,
    /** What this source is used for: botanical, agronomic, climate, health, images. */
    val kind: String,
    /** ISO date (yyyy-MM-dd) the source was last consulted, null if never consulted for this dataset. */
    val accessedOn: String? = null,
    val notes: LocalizedText = emptyMap(),
)

@Serializable
data class PlantCategory(
    val id: String,
    val names: LocalizedText,
    val order: Int = 0,
)

@Serializable
data class ClimateZone(
    val id: String,
    val names: LocalizedText,
    val description: LocalizedText = emptyMap(),
    /** Köppen-Geiger main codes covered by this zone, e.g. ["Cfb", "Dfb"]. */
    val koppen: List<String> = emptyList(),
)

@Serializable
data class SoilType(
    val id: String,
    val names: LocalizedText,
    val description: LocalizedText = emptyMap(),
)

@Serializable
data class CultivationMethod(
    val id: String,
    val names: LocalizedText,
    val description: LocalizedText = emptyMap(),
)

@Serializable
data class Region(
    val id: String,
    val countryId: String,
    val names: LocalizedText,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Int? = null,
    val climateZoneId: String,
    val koppen: String? = null,
    /** 12 monthly mean temperatures (°C), January first. Null if unknown. */
    val monthlyMeanTempC: List<Double>? = null,
    /** 12 monthly mean of daily minimum temperatures (°C). */
    val monthlyMinTempC: List<Double>? = null,
    /** 12 monthly precipitation totals (mm). */
    val monthlyPrecipMm: List<Double>? = null,
    /** Months (1..12) with a known risk of frost. Overrides the estimation from minimum temperatures. */
    val frostMonths: List<Int>? = null,
    /** Months (1..12) of the rainy season, for tropical / semi-arid regions. */
    val rainySeasonMonths: List<Int>? = null,
    val dominantSoilIds: List<String> = emptyList(),
    val climateStatus: ValidationStatus = ValidationStatus.TO_VERIFY,
    val sourceIds: List<String> = emptyList(),
    val notes: LocalizedText = emptyMap(),
) {
    init {
        monthlyMeanTempC?.let { require(it.size == 12) { "Region $id: monthlyMeanTempC must have 12 values" } }
        monthlyMinTempC?.let { require(it.size == 12) { "Region $id: monthlyMinTempC must have 12 values" } }
        monthlyPrecipMm?.let { require(it.size == 12) { "Region $id: monthlyPrecipMm must have 12 values" } }
    }
}

@Serializable
data class Country(
    /** ISO 3166-1 alpha-2 code. */
    val id: String,
    val names: LocalizedText,
    val hemisphere: Hemisphere,
    val regions: List<Region> = emptyList(),
)

@Serializable
data class TemperatureRange(
    /** Minimum mean temperature for active growth (°C). */
    val min: Double? = null,
    val optimalMin: Double? = null,
    val optimalMax: Double? = null,
    /** Mean temperature above which growth / fruit set is strongly impaired (°C). */
    val max: Double? = null,
    /** Minimum soil/air temperature for germination (°C). */
    val germinationMin: Double? = null,
    /** Lowest temperature a perennial typically survives when dormant (°C). */
    val hardinessMin: Double? = null,
)

@Serializable
data class Range(val min: Double, val max: Double) {
    init { require(min <= max) { "Range min ($min) must be <= max ($max)" } }
}

@Serializable
data class Spacing(
    val inRowCm: Range? = null,
    val betweenRowsCm: Range? = null,
)

@Serializable
data class PlantVariety(
    val id: String,
    val name: String,
    val description: LocalizedText = emptyMap(),
    val cycleDays: Range? = null,
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
)

@Serializable
data class GrowingStage(
    val order: Int,
    val title: LocalizedText,
    val body: LocalizedText,
    val tips: LocalizedText = emptyMap(),
    val mistakes: LocalizedText = emptyMap(),
    /** Optional: stage only applies to these cultivation methods. Empty = all. */
    val methods: List<String> = emptyList(),
)

@Serializable
data class GrowingGuide(
    val id: String,
    val title: LocalizedText,
    val stages: List<GrowingStage>,
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
)

/**
 * Explicit calendar entry. Takes precedence over computed windows.
 * Scope: a region, or a climate zone + hemisphere, optionally a variety / method.
 */
@Serializable
data class SeasonalCalendarEntry(
    val activity: Activity,
    val months: List<Int>,
    val regionId: String? = null,
    val climateZoneId: String? = null,
    val hemisphere: Hemisphere? = null,
    val varietyId: String? = null,
    val methodId: String? = null,
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
    val assumptions: LocalizedText = emptyMap(),
    val sourceIds: List<String> = emptyList(),
) {
    init { require(months.all { it in 1..12 }) { "Calendar months must be within 1..12" } }
}

@Serializable
enum class CompanionRelation { @SerialName("good") GOOD, @SerialName("bad") BAD }

@Serializable
data class PlantCompanion(
    val otherPlantId: String,
    val relation: CompanionRelation,
    val reason: LocalizedText = emptyMap(),
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
)

@Serializable
data class CropRotation(
    /** Rotation group, e.g. fruit_vegetable, root, leaf, legume, cereal, perennial. */
    val group: String,
    /** Minimum number of years before growing the same family again on the plot. */
    val minYearsBeforeReturn: Int? = null,
    val goodPredecessorFamilies: List<String> = emptyList(),
    val avoidPredecessorFamilies: List<String> = emptyList(),
    val notes: LocalizedText = emptyMap(),
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
)

/** Fixed calendar for perennials expressed for the northern hemisphere (shifted by 6 months in the south). */
@Serializable
data class PerennialPhenology(
    val plantingMonthsNorth: List<Int> = emptyList(),
    val pruningMonthsNorth: List<Int> = emptyList(),
    val floweringMonthsNorth: List<Int> = emptyList(),
    val harvestMonthsNorth: List<Int> = emptyList(),
    /** Whether the crop needs a cold winter (chilling) to flower properly. */
    val requiresWinterChill: Boolean = false,
    /** Coldest-month mean temperature (°C) at or below which chilling is considered plausible. */
    val chillColdestMonthMaxC: Double? = null,
)

@Serializable
data class Plant(
    /** Stable identifier (slug of the accepted scientific name). */
    val id: String,
    val scientificName: String,
    val scientificAuthority: String? = null,
    val synonyms: List<String> = emptyList(),
    val family: String,
    val categoryId: String,
    val names: LocalizedNames,
    val description: LocalizedText = emptyMap(),
    val lifecycle: Lifecycle,
    val difficulty: Difficulty,
    val temperature: TemperatureRange = TemperatureRange(),
    val frostTolerance: FrostTolerance,
    val sun: SunExposure,
    val soilIds: List<String> = emptyList(),
    val ph: Range? = null,
    val water: WaterNeed,
    val cycleDays: Range? = null,
    val spacing: Spacing? = null,
    val sowingDepthCm: Range? = null,
    val sowingMode: SowingMode,
    /** Weeks spent in nursery before transplanting (for TRANSPLANT / BOTH). */
    val nurseryWeeks: Range? = null,
    val methods: List<String> = listOf(Methods.OPEN_FIELD),
    val climateZoneIds: List<String> = emptyList(),
    val yieldInfo: LocalizedText = emptyMap(),
    val yieldFactors: LocalizedText = emptyMap(),
    val storage: LocalizedText = emptyMap(),
    val varieties: List<PlantVariety> = emptyList(),
    val guides: List<GrowingGuide> = emptyList(),
    val calendar: List<SeasonalCalendarEntry> = emptyList(),
    val phenology: PerennialPhenology? = null,
    val companions: List<PlantCompanion> = emptyList(),
    val rotation: CropRotation? = null,
    val diseaseIds: List<String> = emptyList(),
    val pestIds: List<String> = emptyList(),
    val image: String? = null,
    val imageCredit: String? = null,
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
    val sourceIds: List<String> = emptyList(),
    /** ISO date of the last review of this record. */
    val lastReviewed: String? = null,
    /** True for demonstration content not validated by an agronomist. */
    val isDemo: Boolean = true,
) {
    fun displayName(lang: String): String = Lang.pickNames(names, lang).firstOrNull() ?: scientificName
    val cycleMonthsMin: Int? get() = cycleDays?.let { kotlin.math.ceil(it.min / 30.0).toInt().coerceAtLeast(1) }
    val cycleMonthsMax: Int? get() = cycleDays?.let { kotlin.math.ceil(it.max / 30.0).toInt().coerceAtLeast(1) }
}

@Serializable
enum class HealthIssueType {
    @SerialName("fungal") FUNGAL,
    @SerialName("oomycete") OOMYCETE,
    @SerialName("bacterial") BACTERIAL,
    @SerialName("viral") VIRAL,
    @SerialName("physiological") PHYSIOLOGICAL,
    @SerialName("insect") INSECT,
    @SerialName("mite") MITE,
    @SerialName("mollusc") MOLLUSC,
    @SerialName("nematode") NEMATODE,
    @SerialName("other") OTHER,
}

@Serializable
data class PlantDisease(
    val id: String,
    val names: LocalizedText,
    val causalAgent: String? = null,
    val type: HealthIssueType,
    val symptoms: LocalizedText = emptyMap(),
    val favorableConditions: LocalizedText = emptyMap(),
    val prevention: LocalizedText = emptyMap(),
    val treatmentIds: List<String> = emptyList(),
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
    val sourceIds: List<String> = emptyList(),
)

@Serializable
data class Pest(
    val id: String,
    val names: LocalizedText,
    val scientificName: String? = null,
    val type: HealthIssueType,
    val description: LocalizedText = emptyMap(),
    val damage: LocalizedText = emptyMap(),
    val prevention: LocalizedText = emptyMap(),
    val treatmentIds: List<String> = emptyList(),
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
    val sourceIds: List<String> = emptyList(),
)

@Serializable
enum class TreatmentType {
    @SerialName("cultural") CULTURAL,
    @SerialName("mechanical") MECHANICAL,
    @SerialName("biological") BIOLOGICAL,
    @SerialName("chemical") CHEMICAL,
}

@Serializable
data class Treatment(
    val id: String,
    val names: LocalizedText,
    val type: TreatmentType,
    val description: LocalizedText = emptyMap(),
    /** Mandatory safety / regulatory caution. */
    val caution: LocalizedText = emptyMap(),
    val status: ValidationStatus = ValidationStatus.TO_VERIFY,
    val sourceIds: List<String> = emptyList(),
)

/** Reference tables shipped in `reference.json`. */
@Serializable
data class ReferenceData(
    val categories: List<PlantCategory>,
    val climateZones: List<ClimateZone>,
    val soilTypes: List<SoilType>,
    val methods: List<CultivationMethod>,
    val sources: List<DataSource>,
)

@Serializable
data class HealthData(
    val diseases: List<PlantDisease>,
    val pests: List<Pest>,
    val treatments: List<Treatment>,
)

@Serializable
data class GeographyData(val countries: List<Country>)

@Serializable
data class SeedManifest(
    /** Monotonic dataset version. The app re-imports the content when it increases. */
    val version: Int,
    val generatedOn: String,
    /** True when the dataset is demonstration content (displayed as such in the UI). */
    val demo: Boolean,
    val plantFiles: List<String>,
    val disclaimer: LocalizedText = emptyMap(),
)

/** The whole knowledge base, in memory. */
data class Catalog(
    val manifest: SeedManifest,
    val reference: ReferenceData,
    val geography: GeographyData,
    val health: HealthData,
    val plants: List<Plant>,
) {
    val plantsById: Map<String, Plant> = plants.associateBy { it.id }
    val regionsById: Map<String, Region> = geography.countries.flatMap { it.regions }.associateBy { it.id }
    val countriesById: Map<String, Country> = geography.countries.associateBy { it.id }
    val diseasesById: Map<String, PlantDisease> = health.diseases.associateBy { it.id }
    val pestsById: Map<String, Pest> = health.pests.associateBy { it.id }
    val treatmentsById: Map<String, Treatment> = health.treatments.associateBy { it.id }
    val sourcesById: Map<String, DataSource> = reference.sources.associateBy { it.id }
    val categoriesById: Map<String, PlantCategory> = reference.categories.associateBy { it.id }
    val climateZonesById: Map<String, ClimateZone> = reference.climateZones.associateBy { it.id }
    val soilTypesById: Map<String, SoilType> = reference.soilTypes.associateBy { it.id }
    val methodsById: Map<String, CultivationMethod> = reference.methods.associateBy { it.id }

    fun hemisphereOf(region: Region): Hemisphere = countriesById[region.countryId]?.hemisphere ?: Hemisphere.NORTH
}
