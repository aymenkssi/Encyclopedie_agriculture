package org.agripedia.core.sync

/**
 * Minimal metadata carried by every user-generated record (favorites, gardens,
 * crops, tasks, notes) so that it can be synchronized between devices later.
 */
data class SyncMeta(
    /** Globally unique id (UUID) generated on the device that created the record. */
    val id: String,
    /** Last modification, epoch milliseconds. */
    val updatedAt: Long,
    /** Tombstone: deleted records are kept until the deletion has been synchronized. */
    val deleted: Boolean,
    /** Device that made the last modification (deterministic tie-breaker). */
    val deviceId: String,
)

data class MergeResult(
    /** Remote records that must be written locally (newer remote versions, incl. tombstones). */
    val applyLocally: List<SyncMeta>,
    /** Local records that must be sent to the server. */
    val pushRemote: List<SyncMeta>,
    /** Tombstones known on both sides with the same version, safe to purge locally. */
    val purgeable: List<String>,
)

/**
 * Last-writer-wins merge keyed by stable UUIDs. Because ids are UUIDs created
 * once, the same record can never be duplicated by a sync, and because deletions
 * are tombstones, a deletion is never lost or resurrected by an older copy.
 */
object SyncMerger {
    fun merge(local: List<SyncMeta>, remote: List<SyncMeta>): MergeResult {
        val l = local.associateBy { it.id }
        val r = remote.associateBy { it.id }
        val apply = mutableListOf<SyncMeta>()
        val push = mutableListOf<SyncMeta>()
        val purge = mutableListOf<String>()
        for (id in (l.keys + r.keys)) {
            val a = l[id]; val b = r[id]
            when {
                a == null -> apply += b!!
                b == null -> push += a
                newer(a, b) -> push += a
                newer(b, a) -> apply += b
                else -> if (a.deleted && b.deleted) purge += id
            }
        }
        return MergeResult(apply, push, purge)
    }

    /** True when [x] wins over [y]. */
    fun newer(x: SyncMeta, y: SyncMeta): Boolean = when {
        x.updatedAt != y.updatedAt -> x.updatedAt > y.updatedAt
        x.deleted != y.deleted -> x.deleted // on exact tie, deletion wins
        else -> x.deviceId > y.deviceId && x != y
    }
}
