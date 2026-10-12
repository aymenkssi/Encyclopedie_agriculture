package org.agripedia.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * User data. Every syncable record has a UUID primary key, an `updatedAt`
 * timestamp, a `deviceId` and a soft-delete flag (`deleted`) so that a future
 * synchronization (see core SyncMerger) never duplicates nor loses records.
 * Plant ids are stable string references to the content tables.
 */

@Serializable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = SINGLE_ID,
    val countryId: String?,
    val regionId: String?,
    val experience: String,
    val farmType: String,
    val preferredMethod: String,
    val updatedAt: Long,
) { companion object { const val SINGLE_ID = "me" } }

@Serializable
@Entity(tableName = "user_favorite", indices = [Index(value = ["plantId"], unique = true)])
data class UserFavoriteEntity(
    @PrimaryKey val id: String,
    val plantId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
)

/** Local consultation history (not synchronized). */
@Entity(tableName = "recent_view")
data class RecentViewEntity(@PrimaryKey val plantId: String, val viewedAt: Long)

@Serializable
@Entity(tableName = "user_garden")
data class UserGardenEntity(
    @PrimaryKey val id: String,
    val name: String,
    val farmType: String,
    val areaM2: Double?,
    val regionId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "garden_plot",
    foreignKeys = [ForeignKey(entity = UserGardenEntity::class, parentColumns = ["id"], childColumns = ["gardenId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gardenId")],
)
data class GardenPlotEntity(
    @PrimaryKey val id: String,
    val gardenId: String,
    val name: String,
    val areaM2: Double?,
    val soilId: String?,
    val methodId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "user_crop",
    foreignKeys = [ForeignKey(entity = UserGardenEntity::class, parentColumns = ["id"], childColumns = ["gardenId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gardenId"), Index("plotId"), Index("plantId")],
)
data class UserCropEntity(
    @PrimaryKey val id: String,
    val gardenId: String,
    val plotId: String?,
    val plantId: String,
    val varietyId: String?,
    val methodId: String,
    /** Activity of the start date (nursery_sowing, direct_sowing, planting...). */
    val startActivity: String,
    /** Epoch day of the start date. */
    val startDate: Long,
    val stage: String,
    val quantity: String?,
    val harvestedOn: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
)

@Serializable
@Entity(tableName = "agricultural_task", indices = [Index("dueDate"), Index("cropId"), Index("plantId")])
data class AgriculturalTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val activity: String,
    /** Epoch day. */
    val dueDate: Long,
    val done: Boolean,
    val doneAt: Long?,
    val plantId: String?,
    val cropId: String?,
    val reminder: Boolean,
    /** Repeat every N days after completion (e.g. watering), null = once. */
    val repeatDays: Int?,
    /** "user" or "planned" (generated from the crop dates). */
    val origin: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
)

@Serializable
@Entity(tableName = "user_note", indices = [Index("plantId"), Index("cropId")])
data class UserNoteEntity(
    @PrimaryKey val id: String,
    val plantId: String?,
    val cropId: String?,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
)
