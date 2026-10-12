package org.agripedia.app.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.agripedia.app.data.db.AgriculturalTaskEntity
import org.agripedia.app.data.db.AppDatabase
import org.agripedia.app.data.db.GardenPlotEntity
import org.agripedia.app.data.db.UserCropEntity
import org.agripedia.app.data.db.UserFavoriteEntity
import org.agripedia.app.data.db.UserGardenEntity
import org.agripedia.app.data.db.UserNoteEntity
import org.agripedia.app.data.db.UserProfileEntity
import org.agripedia.core.sync.SyncMerger
import org.agripedia.core.sync.SyncMeta

@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val profile: UserProfileEntity? = null,
    val favorites: List<UserFavoriteEntity> = emptyList(),
    val gardens: List<UserGardenEntity> = emptyList(),
    val plots: List<GardenPlotEntity> = emptyList(),
    val crops: List<UserCropEntity> = emptyList(),
    val tasks: List<AgriculturalTaskEntity> = emptyList(),
    val notes: List<UserNoteEntity> = emptyList(),
) { companion object { const val FORMAT = "agripedia-backup" } }

data class RestoreReport(val applied: Int, val skipped: Int)

/**
 * Export / import of personal data as a JSON file chosen by the user (Storage
 * Access Framework: no storage permission needed). Import merges with the
 * last-writer-wins rule of [SyncMerger], so importing twice never duplicates.
 */
class BackupManager(private val db: AppDatabase, private val resolver: ContentResolver) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val dao = db.userDao()

    suspend fun export(uri: Uri) {
        val file = BackupFile(
            exportedAt = System.currentTimeMillis(), profile = dao.profileOnce(), favorites = dao.allFavorites(),
            gardens = dao.allGardens(), plots = dao.allPlots(), crops = dao.allCrops(), tasks = dao.allTasks(), notes = dao.allNotes(),
        )
        resolver.openOutputStream(uri, "wt")?.use { it.write(json.encodeToString(file).toByteArray()) }
            ?: error("Cannot open destination")
    }

    suspend fun import(uri: Uri): RestoreReport {
        val text = resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Cannot open file")
        val file = json.decodeFromString<BackupFile>(text)
        require(file.format == BackupFile.FORMAT) { "Not an AgriPedia backup" }
        var applied = 0; var skipped = 0
        fun <T> select(local: List<T>, remote: List<T>, meta: (T) -> SyncMeta): List<T> {
            val toApply = SyncMerger.merge(local.map(meta), remote.map(meta)).applyLocally.map { it.id }.toSet()
            return remote.filter { meta(it).id in toApply }.also { applied += it.size; skipped += remote.size - it.size }
        }
        db.withTransaction {
            if (dao.profileOnce() == null) file.profile?.let { dao.saveProfile(it) }
            select(dao.allFavorites(), file.favorites) { SyncMeta(it.id, it.updatedAt, it.deleted, it.deviceId) }.forEach {
                // A favorite is unique per plant: keep the local row id when the plant is already present.
                val existing = dao.favorite(it.plantId)
                dao.saveFavorite(if (existing != null) it.copy(id = existing.id) else it)
            }
            select(dao.allGardens(), file.gardens) { SyncMeta(it.id, it.updatedAt, it.deleted, it.deviceId) }.forEach { dao.saveGarden(it) }
            select(dao.allPlots(), file.plots) { SyncMeta(it.id, it.updatedAt, it.deleted, it.deviceId) }.forEach { dao.savePlot(it) }
            select(dao.allCrops(), file.crops) { SyncMeta(it.id, it.updatedAt, it.deleted, it.deviceId) }.forEach { dao.saveCrop(it) }
            dao.saveTasks(select(dao.allTasks(), file.tasks) { SyncMeta(it.id, it.updatedAt, it.deleted, it.deviceId) })
            select(dao.allNotes(), file.notes) { SyncMeta(it.id, it.updatedAt, it.deleted, it.deviceId) }.forEach { dao.saveNote(it) }
        }
        return RestoreReport(applied, skipped)
    }
}
