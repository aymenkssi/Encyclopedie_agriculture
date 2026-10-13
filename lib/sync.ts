/**
 * Metadata carried by every user record (gardens, crops, tasks, notes, favorites) so that
 * backups and a future multi-device sync never duplicate nor lose data.
 */
export interface SyncMeta { id: string; updatedAt: number; deleted: boolean; deviceId: string }

/** True when x wins over y (last writer wins; on an exact tie, a deletion wins). */
export function isNewer(x: SyncMeta, y: SyncMeta): boolean {
  if (x.updatedAt !== y.updatedAt) return x.updatedAt > y.updatedAt;
  if (x.deleted !== y.deleted) return x.deleted;
  return x.deviceId > y.deviceId;
}

/** Last-writer-wins merge keyed by stable UUIDs; returns the records to apply locally and to push. */
export function mergeRecords<T extends SyncMeta>(local: T[], remote: T[]): { applyLocally: T[]; pushRemote: T[] } {
  const l = new Map(local.map((r) => [r.id, r]));
  const r = new Map(remote.map((x) => [x.id, x]));
  const applyLocally: T[] = [];
  const pushRemote: T[] = [];
  for (const id of new Set([...l.keys(), ...r.keys()])) {
    const a = l.get(id);
    const b = r.get(id);
    if (!a) applyLocally.push(b!);
    else if (!b) pushRemote.push(a);
    else if (isNewer(a, b)) pushRemote.push(a);
    else if (isNewer(b, a)) applyLocally.push(b);
  }
  return { applyLocally, pushRemote };
}
