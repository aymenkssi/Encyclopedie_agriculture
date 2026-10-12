package org.agripedia.app.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.agripedia.app.data.db.AgriculturalTaskEntity
import org.agripedia.app.data.db.AppDatabase
import org.agripedia.app.data.db.GardenPlotEntity
import org.agripedia.app.data.db.RecentViewEntity
import org.agripedia.app.data.db.UserCropEntity
import org.agripedia.app.data.db.UserFavoriteEntity
import org.agripedia.app.data.db.UserGardenEntity
import org.agripedia.app.data.db.UserNoteEntity
import org.agripedia.app.data.db.UserProfileEntity
import org.agripedia.app.data.prefs.SettingsStore
import org.agripedia.core.engine.ExperienceLevel
import org.agripedia.core.engine.FarmType
import org.agripedia.core.engine.TaskPlanner
import org.agripedia.core.model.Activity
import org.agripedia.core.model.Methods
import org.agripedia.core.model.Plant
import java.time.LocalDate
import java.util.UUID

/** Growth stage of a user crop, used to follow its progress. */
enum class CropStage { PLANNED, SOWN, GROWING, FLOWERING, HARVESTING, FINISHED }

data class UserProfile(
    val countryId: String? = null,
    val regionId: String? = null,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val farmType: FarmType = FarmType.GARDEN,
    val preferredMethod: String = Methods.OPEN_FIELD,
)

object TaskOrigin { const val USER = "user"; const val PLANNED = "planned" }

/** Personal data (offline-first, sync-ready). All writes stamp `updatedAt` and the device id. */
class UserRepository(private val db: AppDatabase, private val settings: SettingsStore) {
    private val dao = db.userDao()
    private fun now() = System.currentTimeMillis()
    private fun newId() = UUID.randomUUID().toString()

    // ------------------------------------------------------------ profile
    val profile: Flow<UserProfile> = dao.profile().map { e ->
        if (e == null) UserProfile() else UserProfile(
            e.countryId, e.regionId,
            runCatching { ExperienceLevel.valueOf(e.experience) }.getOrDefault(ExperienceLevel.BEGINNER),
            runCatching { FarmType.valueOf(e.farmType) }.getOrDefault(FarmType.GARDEN),
            e.preferredMethod,
        )
    }

    suspend fun saveProfile(p: UserProfile) = dao.saveProfile(
        UserProfileEntity(countryId = p.countryId, regionId = p.regionId, experience = p.experience.name,
            farmType = p.farmType.name, preferredMethod = p.preferredMethod, updatedAt = now())
    )

    // ------------------------------------------------------------ favorites & history
    val favoritePlantIds: Flow<List<String>> = dao.favorites().map { list -> list.map { it.plantId } }

    suspend fun toggleFavorite(plantId: String) {
        val existing = dao.favorite(plantId)
        val t = now()
        if (existing == null) dao.saveFavorite(UserFavoriteEntity(newId(), plantId, t, t, settings.deviceId()))
        else dao.saveFavorite(existing.copy(deleted = !existing.deleted, updatedAt = t, deviceId = settings.deviceId()))
    }

    fun recentPlantIds(limit: Int = 20): Flow<List<String>> = dao.recent(limit).map { l -> l.map { it.plantId } }
    suspend fun markViewed(plantId: String) = dao.saveRecent(RecentViewEntity(plantId, now()))
    suspend fun clearHistory() = dao.clearRecent()

    // ------------------------------------------------------------ gardens & plots
    val gardens: Flow<List<UserGardenEntity>> = dao.gardens()
    fun garden(id: String) = dao.garden(id)
    fun plots(gardenId: String) = dao.plots(gardenId)

    suspend fun saveGarden(id: String?, name: String, type: FarmType, areaM2: Double?, regionId: String?): String {
        val t = now()
        val gid = id ?: newId()
        dao.saveGarden(UserGardenEntity(gid, name.trim(), type.name, areaM2, regionId, t, t, settings.deviceId()))
        return gid
    }

    suspend fun deleteGarden(g: UserGardenEntity) = dao.saveGarden(g.copy(deleted = true, updatedAt = now(), deviceId = settings.deviceId()))

    suspend fun savePlot(gardenId: String, name: String, areaM2: Double?, soilId: String?, methodId: String) {
        val t = now()
        dao.savePlot(GardenPlotEntity(newId(), gardenId, name.trim(), areaM2, soilId, methodId, t, t, settings.deviceId()))
    }

    suspend fun deletePlot(p: GardenPlotEntity) = dao.savePlot(p.copy(deleted = true, updatedAt = now(), deviceId = settings.deviceId()))

    /** Families grown on a plot, most recent first (for rotation advice). */
    suspend fun cropHistoryOfPlot(plotId: String): List<UserCropEntity> = dao.cropsOfPlot(plotId)

    // ------------------------------------------------------------ crops
    val crops: Flow<List<UserCropEntity>> = dao.crops()
    fun cropsOfGarden(gardenId: String) = dao.cropsOfGarden(gardenId)
    fun crop(id: String) = dao.crop(id)

    /**
     * Adds a crop and, if requested, generates milestone tasks from the user's own
     * start date and the plant's data (see core TaskPlanner).
     */
    suspend fun addCrop(
        gardenId: String, plotId: String?, plant: Plant, varietyId: String?, methodId: String,
        startActivity: Activity, startDate: LocalDate, stage: CropStage, quantity: String?,
        planTasks: Boolean, taskTitle: (Activity) -> String,
    ): String {
        val t = now(); val device = settings.deviceId(); val id = newId()
        db.withTransaction {
            dao.saveCrop(UserCropEntity(id, gardenId, plotId, plant.id, varietyId, methodId, startActivity.name,
                startDate.toEpochDay(), stage.name, quantity?.takeIf { it.isNotBlank() }, null, t, t, device))
            if (planTasks) {
                val planned = TaskPlanner.plan(plant, startActivity, startDate)
                dao.saveTasks(planned.map {
                    AgriculturalTaskEntity(newId(), taskTitle(it.activity), null, it.activity.name, it.date.toEpochDay(), false, null,
                        plant.id, id, reminder = true, repeatDays = null, origin = TaskOrigin.PLANNED, createdAt = t, updatedAt = t, deviceId = device)
                })
            }
        }
        return id
    }

    suspend fun updateCropStage(c: UserCropEntity, stage: CropStage) = dao.saveCrop(
        c.copy(stage = stage.name, harvestedOn = if (stage == CropStage.FINISHED) LocalDate.now().toEpochDay() else c.harvestedOn,
            updatedAt = now(), deviceId = settings.deviceId())
    )

    suspend fun deleteCrop(c: UserCropEntity) = dao.saveCrop(c.copy(deleted = true, updatedAt = now(), deviceId = settings.deviceId()))

    // ------------------------------------------------------------ tasks
    fun tasksBetween(from: LocalDate, to: LocalDate) = dao.tasksBetween(from.toEpochDay(), to.toEpochDay())
    fun openTasksUntil(day: LocalDate) = dao.openTasksUntil(day.toEpochDay())
    fun tasksOfCrop(cropId: String) = dao.tasksOfCrop(cropId)
    suspend fun dueReminders(day: LocalDate) = dao.dueReminders(day.toEpochDay())

    suspend fun saveTask(
        existing: AgriculturalTaskEntity?, title: String, description: String?, activity: Activity, due: LocalDate,
        plantId: String?, cropId: String?, reminder: Boolean, repeatDays: Int?,
    ) {
        val t = now()
        val base = existing ?: AgriculturalTaskEntity(newId(), "", null, "", 0, false, null, null, null, false, null, TaskOrigin.USER, t, t, "")
        dao.saveTask(base.copy(title = title.trim(), description = description?.trim()?.takeIf { it.isNotEmpty() }, activity = activity.name,
            dueDate = due.toEpochDay(), plantId = plantId, cropId = cropId ?: base.cropId, reminder = reminder, repeatDays = repeatDays,
            updatedAt = t, deviceId = settings.deviceId()))
    }

    /** Marks a task done (or not). A repeating task spawns its next occurrence. */
    suspend fun setTaskDone(task: AgriculturalTaskEntity, done: Boolean) {
        val t = now(); val device = settings.deviceId()
        db.withTransaction {
            dao.saveTask(task.copy(done = done, doneAt = if (done) t else null, updatedAt = t, deviceId = device))
            if (done && !task.done && task.repeatDays != null && task.repeatDays > 0) {
                val next = maxOf(task.dueDate, LocalDate.now().toEpochDay()) + task.repeatDays
                dao.saveTask(task.copy(id = newId(), dueDate = next, done = false, doneAt = null, createdAt = t, updatedAt = t, deviceId = device))
            }
        }
    }

    suspend fun deleteTask(task: AgriculturalTaskEntity) = dao.saveTask(task.copy(deleted = true, updatedAt = now(), deviceId = settings.deviceId()))

    // ------------------------------------------------------------ notes
    val notes: Flow<List<UserNoteEntity>> = dao.notes()
    fun notesOfPlant(plantId: String) = dao.notesOfPlant(plantId)
    fun notesOfCrop(cropId: String) = dao.notesOfCrop(cropId)

    suspend fun saveNote(existing: UserNoteEntity?, title: String, body: String, plantId: String?, cropId: String?) {
        val t = now()
        val n = existing?.copy(title = title.trim(), body = body.trim(), updatedAt = t, deviceId = settings.deviceId())
            ?: UserNoteEntity(newId(), plantId, cropId, title.trim(), body.trim(), t, t, settings.deviceId())
        dao.saveNote(n)
    }

    suspend fun deleteNote(n: UserNoteEntity) = dao.saveNote(n.copy(deleted = true, updatedAt = now(), deviceId = settings.deviceId()))
}
