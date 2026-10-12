package org.agripedia.app.data.seed

import android.content.res.AssetManager
import androidx.room.withTransaction
import kotlinx.serialization.encodeToString
import org.agripedia.app.data.db.AppDatabase
import org.agripedia.app.data.db.ClimateZoneEntity
import org.agripedia.app.data.db.ContentMetaEntity
import org.agripedia.app.data.db.CountryEntity
import org.agripedia.app.data.db.CropRotationEntity
import org.agripedia.app.data.db.CultivationMethodEntity
import org.agripedia.app.data.db.DataSourceEntity
import org.agripedia.app.data.db.GrowingGuideEntity
import org.agripedia.app.data.db.GrowingStageEntity
import org.agripedia.app.data.db.PestEntity
import org.agripedia.app.data.db.PlantCategoryEntity
import org.agripedia.app.data.db.PlantCompanionEntity
import org.agripedia.app.data.db.PlantDiseaseEntity
import org.agripedia.app.data.db.PlantEntity
import org.agripedia.app.data.db.PlantLinkEntity
import org.agripedia.app.data.db.PlantTranslationEntity
import org.agripedia.app.data.db.PlantVarietyEntity
import org.agripedia.app.data.db.RegionEntity
import org.agripedia.app.data.db.SeasonalCalendarEntity
import org.agripedia.app.data.db.SoilTypeEntity
import org.agripedia.app.data.db.TreatmentEntity
import org.agripedia.core.model.Catalog
import org.agripedia.core.model.ClimateZone
import org.agripedia.core.model.Country
import org.agripedia.core.model.CultivationMethod
import org.agripedia.core.model.DataSource
import org.agripedia.core.model.GeographyData
import org.agripedia.core.model.HealthData
import org.agripedia.core.model.Pest
import org.agripedia.core.model.Plant
import org.agripedia.core.model.PlantCategory
import org.agripedia.core.model.PlantDisease
import org.agripedia.core.model.ReferenceData
import org.agripedia.core.model.Region
import org.agripedia.core.model.SeedManifest
import org.agripedia.core.model.SoilType
import org.agripedia.core.model.Treatment
import org.agripedia.core.search.TextNormalizer
import org.agripedia.core.seed.SeedLoader
import org.agripedia.core.seed.SeedReader
import org.agripedia.core.seed.SeedValidator

/** Reads the dataset bundled in assets/seed. */
class AssetSeedReader(private val assets: AssetManager) : SeedReader {
    override fun read(path: String): String = assets.open("seed/$path").bufferedReader().use { it.readText() }
}

/**
 * Imports the bundled dataset into Room when the database is empty or when the
 * bundled version is newer, then rebuilds the in-memory [Catalog] from Room.
 * Only content tables are replaced; user tables are untouched.
 */
class ContentImporter(private val db: AppDatabase, private val reader: SeedReader) {
    private val json = SeedLoader.json
    private val dao = db.contentDao()

    companion object { const val KEY_VERSION = "dataset_version"; const val KEY_MANIFEST = "manifest" }

    suspend fun ensureImported(): Int {
        val bundled = SeedLoader.loadManifest(reader)
        val installed = dao.meta(KEY_VERSION)?.toIntOrNull() ?: 0
        if (installed >= bundled.version) return installed
        val catalog = SeedLoader.load(reader)
        val errors = SeedValidator.validate(catalog).filter { it.severity == SeedValidator.Severity.ERROR }
        require(errors.isEmpty()) { "Bundled dataset is invalid: ${errors.take(5).joinToString()}" }
        import(catalog)
        return bundled.version
    }

    private suspend fun import(c: Catalog) = db.withTransaction {
        dao.clearPlants(); dao.clearCountries(); dao.clearSources(); dao.clearCategories(); dao.clearClimateZones()
        dao.clearSoilTypes(); dao.clearMethods(); dao.clearDiseases(); dao.clearPests(); dao.clearTreatments()

        val r = c.reference
        dao.insertSources(r.sources.map { DataSourceEntity(it.id, it.kind, it.title, it.url, it.license, it.accessedOn, json.encodeToString(it)) })
        dao.insertCategories(r.categories.map { PlantCategoryEntity(it.id, it.order, json.encodeToString(it)) })
        dao.insertClimateZones(r.climateZones.map { ClimateZoneEntity(it.id, it.koppen.joinToString(","), json.encodeToString(it)) })
        dao.insertSoilTypes(r.soilTypes.map { SoilTypeEntity(it.id, json.encodeToString(it)) })
        dao.insertMethods(r.methods.map { CultivationMethodEntity(it.id, json.encodeToString(it)) })

        dao.insertCountries(c.geography.countries.map { CountryEntity(it.id, it.hemisphere.name, json.encodeToString(it.copy(regions = emptyList()))) })
        dao.insertRegions(c.geography.countries.flatMap { it.regions }.map {
            RegionEntity(it.id, it.countryId, it.climateZoneId, it.latitude, it.longitude, it.climateStatus.name, json.encodeToString(it))
        })

        dao.insertDiseases(c.health.diseases.map { PlantDiseaseEntity(it.id, it.causalAgent, it.type.name, it.status.name, json.encodeToString(it)) })
        dao.insertPests(c.health.pests.map { PestEntity(it.id, it.scientificName, it.type.name, it.status.name, json.encodeToString(it)) })
        dao.insertTreatments(c.health.treatments.map { TreatmentEntity(it.id, it.type.name, it.status.name, json.encodeToString(it)) })

        val plants = c.plants
        dao.insertPlants(plants.map { p ->
            PlantEntity(p.id, p.scientificName, p.family, p.categoryId, p.lifecycle.name, p.difficulty.name, p.water.name,
                p.frostTolerance.name, p.cycleDays?.min, p.cycleDays?.max, p.status.name, p.isDemo, p.lastReviewed, json.encodeToString(p))
        })
        dao.insertTranslations(plants.flatMap { p ->
            p.names.map { (lang, names) ->
                PlantTranslationEntity(p.id, lang, names.firstOrNull() ?: p.scientificName, names.drop(1).joinToString("|"),
                    TextNormalizer.normalize(names.joinToString(" ")), p.description[lang])
            }
        })
        dao.insertVarieties(plants.flatMap { p -> p.varieties.map { PlantVarietyEntity("${p.id}:${it.id}", p.id, it.name, it.status.name) } })
        dao.insertGuides(plants.flatMap { p -> p.guides.map { GrowingGuideEntity("${p.id}:${it.id}", p.id, it.status.name) } })
        dao.insertStages(plants.flatMap { p -> p.guides.flatMap { g -> g.stages.map { GrowingStageEntity("${p.id}:${g.id}", it.order, it.methods.joinToString(",")) } } })
        dao.insertCalendars(plants.flatMap { p ->
            p.calendar.map {
                SeasonalCalendarEntity(plantId = p.id, activity = it.activity.name, months = it.months.joinToString(","), regionId = it.regionId,
                    climateZoneId = it.climateZoneId, hemisphere = it.hemisphere?.name, varietyId = it.varietyId, methodId = it.methodId, status = it.status.name)
            }
        })
        // Companions may reference plants not (yet) in the catalog: keep only resolvable ones.
        dao.insertCompanions(plants.flatMap { p ->
            p.companions.filter { it.otherPlantId in c.plantsById }.map { PlantCompanionEntity(p.id, it.otherPlantId, it.relation.name, it.status.name) }
        })
        dao.insertRotations(plants.mapNotNull { p ->
            p.rotation?.let { CropRotationEntity(p.id, it.group, it.minYearsBeforeReturn, it.goodPredecessorFamilies.joinToString(","), it.avoidPredecessorFamilies.joinToString(","), it.status.name) }
        })
        dao.insertLinks(plants.flatMap { p ->
            p.soilIds.map { PlantLinkEntity(p.id, PlantLinkEntity.SOIL, it) } +
                p.methods.map { PlantLinkEntity(p.id, PlantLinkEntity.METHOD, it) } +
                p.climateZoneIds.map { PlantLinkEntity(p.id, PlantLinkEntity.CLIMATE_ZONE, it) } +
                p.diseaseIds.map { PlantLinkEntity(p.id, PlantLinkEntity.DISEASE, it) } +
                p.pestIds.map { PlantLinkEntity(p.id, PlantLinkEntity.PEST, it) } +
                p.sourceIds.map { PlantLinkEntity(p.id, PlantLinkEntity.SOURCE, it) }
        }.distinct())

        dao.putMeta(ContentMetaEntity(KEY_MANIFEST, json.encodeToString(c.manifest)))
        dao.putMeta(ContentMetaEntity(KEY_VERSION, c.manifest.version.toString()))
    }

    /** Rebuilds the catalog from the local database (works fully offline). */
    suspend fun loadFromDatabase(): Catalog {
        val manifest = json.decodeFromString<SeedManifest>(requireNotNull(dao.meta(KEY_MANIFEST)) { "No content imported" })
        val countries = dao.countryPayloads().map { json.decodeFromString<Country>(it) }
        // Regions are stored in their own table; reattach them to their country.
        val allRegions = dao.regionPayloads().map { json.decodeFromString<Region>(it) }
        val geography = GeographyData(countries.map { c -> c.copy(regions = allRegions.filter { it.countryId == c.id }) })
        val reference = ReferenceData(
            categories = dao.categoryPayloads().map { json.decodeFromString<PlantCategory>(it) },
            climateZones = dao.climateZonePayloads().map { json.decodeFromString<ClimateZone>(it) },
            soilTypes = dao.soilTypePayloads().map { json.decodeFromString<SoilType>(it) },
            methods = dao.methodPayloads().map { json.decodeFromString<CultivationMethod>(it) },
            sources = dao.sourcePayloads().map { json.decodeFromString<DataSource>(it) },
        )
        val health = HealthData(
            diseases = dao.diseasePayloads().map { json.decodeFromString<PlantDisease>(it) },
            pests = dao.pestPayloads().map { json.decodeFromString<Pest>(it) },
            treatments = dao.treatmentPayloads().map { json.decodeFromString<Treatment>(it) },
        )
        val plants = dao.plantPayloads().map { json.decodeFromString<Plant>(it) }
        return Catalog(manifest, reference, geography, health, plants)
    }
}
