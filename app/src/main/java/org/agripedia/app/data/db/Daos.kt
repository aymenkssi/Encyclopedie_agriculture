package org.agripedia.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT value FROM content_meta WHERE `key` = :key")
    suspend fun meta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeta(meta: ContentMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSources(items: List<DataSourceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(items: List<PlantCategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertClimateZones(items: List<ClimateZoneEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSoilTypes(items: List<SoilTypeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMethods(items: List<CultivationMethodEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCountries(items: List<CountryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRegions(items: List<RegionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlants(items: List<PlantEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTranslations(items: List<PlantTranslationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVarieties(items: List<PlantVarietyEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGuides(items: List<GrowingGuideEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStages(items: List<GrowingStageEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCalendars(items: List<SeasonalCalendarEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDiseases(items: List<PlantDiseaseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPests(items: List<PestEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTreatments(items: List<TreatmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCompanions(items: List<PlantCompanionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRotations(items: List<CropRotationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLinks(items: List<PlantLinkEntity>)

    @Query("DELETE FROM plant") suspend fun clearPlants()
    @Query("DELETE FROM country") suspend fun clearCountries()
    @Query("DELETE FROM data_source") suspend fun clearSources()
    @Query("DELETE FROM plant_category") suspend fun clearCategories()
    @Query("DELETE FROM climate_zone") suspend fun clearClimateZones()
    @Query("DELETE FROM soil_type") suspend fun clearSoilTypes()
    @Query("DELETE FROM cultivation_method") suspend fun clearMethods()
    @Query("DELETE FROM plant_disease") suspend fun clearDiseases()
    @Query("DELETE FROM pest") suspend fun clearPests()
    @Query("DELETE FROM treatment") suspend fun clearTreatments()

    @Query("SELECT payload FROM plant ORDER BY id") suspend fun plantPayloads(): List<String>
    @Query("SELECT payload FROM country ORDER BY id") suspend fun countryPayloads(): List<String>
    @Query("SELECT payload FROM region ORDER BY id") suspend fun regionPayloads(): List<String>
    @Query("SELECT payload FROM data_source ORDER BY id") suspend fun sourcePayloads(): List<String>
    @Query("SELECT payload FROM plant_category ORDER BY sortOrder") suspend fun categoryPayloads(): List<String>
    @Query("SELECT payload FROM climate_zone ORDER BY id") suspend fun climateZonePayloads(): List<String>
    @Query("SELECT payload FROM soil_type ORDER BY id") suspend fun soilTypePayloads(): List<String>
    @Query("SELECT payload FROM cultivation_method ORDER BY id") suspend fun methodPayloads(): List<String>
    @Query("SELECT payload FROM plant_disease ORDER BY id") suspend fun diseasePayloads(): List<String>
    @Query("SELECT payload FROM pest ORDER BY id") suspend fun pestPayloads(): List<String>
    @Query("SELECT payload FROM treatment ORDER BY id") suspend fun treatmentPayloads(): List<String>

    @Query("SELECT plantId FROM plant_link WHERE kind = :kind AND targetId = :targetId")
    suspend fun plantsLinkedTo(kind: String, targetId: String): List<String>
}

@Dao
interface UserDao {
    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 'me'") fun profile(): Flow<UserProfileEntity?>
    @Upsert suspend fun saveProfile(p: UserProfileEntity)

    // Favorites
    @Query("SELECT * FROM user_favorite WHERE deleted = 0 ORDER BY createdAt DESC") fun favorites(): Flow<List<UserFavoriteEntity>>
    @Query("SELECT * FROM user_favorite WHERE plantId = :plantId") suspend fun favorite(plantId: String): UserFavoriteEntity?
    @Upsert suspend fun saveFavorite(f: UserFavoriteEntity)

    // History
    @Query("SELECT * FROM recent_view ORDER BY viewedAt DESC LIMIT :limit") fun recent(limit: Int): Flow<List<RecentViewEntity>>
    @Upsert suspend fun saveRecent(r: RecentViewEntity)
    @Query("DELETE FROM recent_view") suspend fun clearRecent()

    // Gardens & plots
    @Query("SELECT * FROM user_garden WHERE deleted = 0 ORDER BY createdAt") fun gardens(): Flow<List<UserGardenEntity>>
    @Query("SELECT * FROM user_garden WHERE id = :id") fun garden(id: String): Flow<UserGardenEntity?>
    @Upsert suspend fun saveGarden(g: UserGardenEntity)
    @Query("SELECT * FROM garden_plot WHERE gardenId = :gardenId AND deleted = 0 ORDER BY createdAt") fun plots(gardenId: String): Flow<List<GardenPlotEntity>>
    @Query("SELECT * FROM garden_plot WHERE id = :id") suspend fun plot(id: String): GardenPlotEntity?
    @Upsert suspend fun savePlot(p: GardenPlotEntity)

    // Crops
    @Query("SELECT * FROM user_crop WHERE deleted = 0 ORDER BY startDate DESC") fun crops(): Flow<List<UserCropEntity>>
    @Query("SELECT * FROM user_crop WHERE gardenId = :gardenId AND deleted = 0 ORDER BY startDate DESC") fun cropsOfGarden(gardenId: String): Flow<List<UserCropEntity>>
    @Query("SELECT * FROM user_crop WHERE id = :id") fun crop(id: String): Flow<UserCropEntity?>
    @Query("SELECT * FROM user_crop WHERE plotId = :plotId AND deleted = 0 ORDER BY startDate DESC") suspend fun cropsOfPlot(plotId: String): List<UserCropEntity>
    @Upsert suspend fun saveCrop(c: UserCropEntity)

    // Tasks
    @Query("SELECT * FROM agricultural_task WHERE deleted = 0 AND dueDate BETWEEN :from AND :to ORDER BY done, dueDate")
    fun tasksBetween(from: Long, to: Long): Flow<List<AgriculturalTaskEntity>>
    @Query("SELECT * FROM agricultural_task WHERE deleted = 0 AND done = 0 AND dueDate <= :day ORDER BY dueDate")
    fun openTasksUntil(day: Long): Flow<List<AgriculturalTaskEntity>>
    @Query("SELECT * FROM agricultural_task WHERE deleted = 0 AND done = 0 AND reminder = 1 AND dueDate <= :day ORDER BY dueDate")
    suspend fun dueReminders(day: Long): List<AgriculturalTaskEntity>
    @Query("SELECT * FROM agricultural_task WHERE deleted = 0 AND cropId = :cropId ORDER BY dueDate") fun tasksOfCrop(cropId: String): Flow<List<AgriculturalTaskEntity>>
    @Query("SELECT * FROM agricultural_task WHERE id = :id") suspend fun task(id: String): AgriculturalTaskEntity?
    @Upsert suspend fun saveTask(t: AgriculturalTaskEntity)
    @Upsert suspend fun saveTasks(t: List<AgriculturalTaskEntity>)

    // Notes
    @Query("SELECT * FROM user_note WHERE deleted = 0 ORDER BY updatedAt DESC") fun notes(): Flow<List<UserNoteEntity>>
    @Query("SELECT * FROM user_note WHERE deleted = 0 AND plantId = :plantId ORDER BY updatedAt DESC") fun notesOfPlant(plantId: String): Flow<List<UserNoteEntity>>
    @Query("SELECT * FROM user_note WHERE deleted = 0 AND cropId = :cropId ORDER BY updatedAt DESC") fun notesOfCrop(cropId: String): Flow<List<UserNoteEntity>>
    @Upsert suspend fun saveNote(n: UserNoteEntity)

    // Backup (all rows, including tombstones)
    @Query("SELECT * FROM user_favorite") suspend fun allFavorites(): List<UserFavoriteEntity>
    @Query("SELECT * FROM user_garden") suspend fun allGardens(): List<UserGardenEntity>
    @Query("SELECT * FROM garden_plot") suspend fun allPlots(): List<GardenPlotEntity>
    @Query("SELECT * FROM user_crop") suspend fun allCrops(): List<UserCropEntity>
    @Query("SELECT * FROM agricultural_task") suspend fun allTasks(): List<AgriculturalTaskEntity>
    @Query("SELECT * FROM user_note") suspend fun allNotes(): List<UserNoteEntity>
    @Query("SELECT * FROM user_profile WHERE id = 'me'") suspend fun profileOnce(): UserProfileEntity?
}
