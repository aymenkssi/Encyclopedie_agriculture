package org.agripedia.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContentMetaEntity::class, DataSourceEntity::class, PlantCategoryEntity::class, ClimateZoneEntity::class,
        SoilTypeEntity::class, CultivationMethodEntity::class, CountryEntity::class, RegionEntity::class,
        PlantEntity::class, PlantTranslationEntity::class, PlantVarietyEntity::class, GrowingGuideEntity::class,
        GrowingStageEntity::class, SeasonalCalendarEntity::class, PlantDiseaseEntity::class, PestEntity::class,
        TreatmentEntity::class, PlantCompanionEntity::class, CropRotationEntity::class, PlantLinkEntity::class,
        UserProfileEntity::class, UserFavoriteEntity::class, RecentViewEntity::class, UserGardenEntity::class,
        GardenPlotEntity::class, UserCropEntity::class, AgriculturalTaskEntity::class, UserNoteEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "agripedia.db"

        /**
         * Schema migrations are declared here (exported schemas live in app/schemas).
         * Never use destructive migration: user data must survive upgrades.
         */
        val MIGRATIONS = arrayOf<androidx.room.migration.Migration>()

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
