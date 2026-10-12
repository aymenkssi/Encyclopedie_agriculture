package org.agripedia.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Reference content (the encyclopedia). Tables are normalized for querying and
 * indexing; each main row also keeps the canonical JSON record (`payload`) so the
 * domain model can be rebuilt losslessly. Content tables are fully replaced when a
 * newer dataset version is imported — user tables only reference content by stable
 * string ids (no cascading foreign keys), so user data is never lost.
 */

@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "data_source")
data class DataSourceEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val url: String?,
    val license: String?,
    val accessedOn: String?,
    val payload: String,
)

@Entity(tableName = "plant_category")
data class PlantCategoryEntity(@PrimaryKey val id: String, val sortOrder: Int, val payload: String)

@Entity(tableName = "climate_zone")
data class ClimateZoneEntity(@PrimaryKey val id: String, val koppen: String, val payload: String)

@Entity(tableName = "soil_type")
data class SoilTypeEntity(@PrimaryKey val id: String, val payload: String)

@Entity(tableName = "cultivation_method")
data class CultivationMethodEntity(@PrimaryKey val id: String, val payload: String)

@Entity(tableName = "country")
data class CountryEntity(@PrimaryKey val id: String, val hemisphere: String, val payload: String)

@Entity(
    tableName = "region",
    foreignKeys = [ForeignKey(entity = CountryEntity::class, parentColumns = ["id"], childColumns = ["countryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("countryId"), Index("climateZoneId")],
)
data class RegionEntity(
    @PrimaryKey val id: String,
    val countryId: String,
    val climateZoneId: String,
    val latitude: Double,
    val longitude: Double,
    val climateStatus: String,
    val payload: String,
)

@Entity(
    tableName = "plant",
    indices = [Index("categoryId"), Index("family"), Index("scientificName"), Index("difficulty")],
)
data class PlantEntity(
    @PrimaryKey val id: String,
    val scientificName: String,
    val family: String,
    val categoryId: String,
    val lifecycle: String,
    val difficulty: String,
    val water: String,
    val frostTolerance: String,
    val cycleMinDays: Double?,
    val cycleMaxDays: Double?,
    val status: String,
    val isDemo: Boolean,
    val lastReviewed: String?,
    val payload: String,
)

/** Localized names, used for indexed lookups (search is done in memory with typo tolerance). */
@Entity(
    tableName = "plant_translation",
    primaryKeys = ["plantId", "lang"],
    foreignKeys = [ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("commonName"), Index("normalizedNames")],
)
data class PlantTranslationEntity(
    val plantId: String,
    val lang: String,
    val commonName: String,
    val otherNames: String,
    val normalizedNames: String,
    val description: String?,
)

@Entity(
    tableName = "plant_variety",
    foreignKeys = [ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("plantId")],
)
data class PlantVarietyEntity(@PrimaryKey val id: String, val plantId: String, val name: String, val status: String)

@Entity(
    tableName = "growing_guide",
    foreignKeys = [ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("plantId")],
)
data class GrowingGuideEntity(@PrimaryKey val id: String, val plantId: String, val status: String)

@Entity(
    tableName = "growing_stage",
    primaryKeys = ["guideId", "stageOrder"],
    foreignKeys = [ForeignKey(entity = GrowingGuideEntity::class, parentColumns = ["id"], childColumns = ["guideId"], onDelete = ForeignKey.CASCADE)],
)
data class GrowingStageEntity(val guideId: String, val stageOrder: Int, val methods: String)

@Entity(
    tableName = "seasonal_calendar",
    foreignKeys = [ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("plantId"), Index("regionId"), Index("climateZoneId")],
)
data class SeasonalCalendarEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val plantId: String,
    val activity: String,
    val months: String,
    val regionId: String?,
    val climateZoneId: String?,
    val hemisphere: String?,
    val varietyId: String?,
    val methodId: String?,
    val status: String,
)

@Entity(tableName = "plant_disease")
data class PlantDiseaseEntity(@PrimaryKey val id: String, val causalAgent: String?, val type: String, val status: String, val payload: String)

@Entity(tableName = "pest")
data class PestEntity(@PrimaryKey val id: String, val scientificName: String?, val type: String, val status: String, val payload: String)

@Entity(tableName = "treatment")
data class TreatmentEntity(@PrimaryKey val id: String, val type: String, val status: String, val payload: String)

@Entity(
    tableName = "plant_companion",
    primaryKeys = ["plantId", "otherPlantId"],
    foreignKeys = [ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("otherPlantId")],
)
data class PlantCompanionEntity(val plantId: String, val otherPlantId: String, val relation: String, val status: String)

@Entity(
    tableName = "crop_rotation",
    foreignKeys = [ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE)],
)
data class CropRotationEntity(
    @PrimaryKey val plantId: String,
    val rotationGroup: String,
    val minYearsBeforeReturn: Int?,
    val goodPredecessorFamilies: String,
    val avoidPredecessorFamilies: String,
    val status: String,
)

/** Generic plant <-> reference link (soil, method, climate zone, disease, pest, source). */
@Entity(
    tableName = "plant_link",
    primaryKeys = ["plantId", "kind", "targetId"],
    foreignKeys = [ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["kind", "targetId"])],
)
data class PlantLinkEntity(val plantId: String, val kind: String, val targetId: String) {
    companion object {
        const val SOIL = "soil"
        const val METHOD = "method"
        const val CLIMATE_ZONE = "climate_zone"
        const val DISEASE = "disease"
        const val PEST = "pest"
        const val SOURCE = "source"
    }
}
