import { isValidIso, IsoDate } from './dates';
import { FarmType, FARM_TYPES } from './recommend';
import { mergeRecords, SyncMeta } from './sync';
import { Activity, ACTIVITIES } from './types';

/** Personal data, stored on the phone. Every record carries sync metadata (see lib/sync.ts). */
export interface Base extends SyncMeta { createdAt: number }

export type CropStage = 'planned' | 'sown' | 'growing' | 'flowering' | 'harvesting' | 'finished';
export const CROP_STAGES: CropStage[] = ['planned', 'sown', 'growing', 'flowering', 'harvesting', 'finished'];

export interface Favorite extends Base { plantId: string }
export interface Garden extends Base { name: string; farmType: FarmType; areaM2: number | null; regionId: string | null }
export interface Plot extends Base { gardenId: string; name: string; areaM2: number | null; soilId: string | null; methodId: string }
export interface Crop extends Base {
  gardenId: string;
  plotId: string | null;
  plantId: string;
  varietyId: string | null;
  methodId: string;
  startActivity: Activity;
  startDate: IsoDate;
  stage: CropStage;
  quantity: string | null;
  harvestedOn: IsoDate | null;
}
export interface Task extends Base {
  title: string;
  description: string | null;
  activity: Activity;
  dueDate: IsoDate;
  done: boolean;
  doneAt: number | null;
  plantId: string | null;
  cropId: string | null;
  reminder: boolean;
  /** Repeat N days after completion (e.g. watering), null = once. */
  repeatDays: number | null;
  /** "user" or "planned" (derived from the crop dates). */
  origin: 'user' | 'planned';
  notificationId: string | null;
}
export interface Note extends Base { plantId: string | null; cropId: string | null; title: string; body: string }

export interface UserData {
  favorites: Favorite[];
  gardens: Garden[];
  plots: Plot[];
  crops: Crop[];
  tasks: Task[];
  notes: Note[];
}

export const EMPTY_DATA: UserData = { favorites: [], gardens: [], plots: [], crops: [], tasks: [], notes: [] };

export const newId = (): string =>
  `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}-${Math.random().toString(36).slice(2, 10)}`;

const isBase = (r: unknown): r is Base => {
  const b = r as Base;
  return !!b && typeof b.id === 'string' && typeof b.updatedAt === 'number' && typeof b.deleted === 'boolean';
};

/** Drops malformed records so that a corrupted file or an old backup never breaks the app. */
export function sanitizeUserData(raw: unknown): UserData {
  const d = (raw && typeof raw === 'object' ? raw : {}) as Partial<Record<keyof UserData, unknown[]>>;
  const list = <T,>(key: keyof UserData, ok: (r: T) => boolean): T[] =>
    (Array.isArray(d[key]) ? (d[key] as unknown[]) : []).filter((r) => isBase(r) && ok(r as T)) as T[];
  return {
    favorites: list<Favorite>('favorites', (r) => typeof r.plantId === 'string'),
    gardens: list<Garden>('gardens', (r) => typeof r.name === 'string' && FARM_TYPES.includes(r.farmType)),
    plots: list<Plot>('plots', (r) => typeof r.gardenId === 'string' && typeof r.name === 'string'),
    crops: list<Crop>('crops', (r) => typeof r.plantId === 'string' && isValidIso(r.startDate) && ACTIVITIES.includes(r.startActivity)),
    tasks: list<Task>('tasks', (r) => typeof r.title === 'string' && isValidIso(r.dueDate) && ACTIVITIES.includes(r.activity)),
    notes: list<Note>('notes', (r) => typeof r.body === 'string'),
  };
}

export const BACKUP_FORMAT = 'agripedia-backup';

/** Merges a backup into the local data (last writer wins): importing twice never duplicates. */
export function mergeBackup(local: UserData, backup: UserData): { data: UserData; applied: number } {
  let applied = 0;
  const merge = <T extends Base>(a: T[], b: T[]): T[] => {
    const incoming = mergeRecords(a, b).applyLocally;
    applied += incoming.length;
    const ids = new Set(incoming.map((r) => r.id));
    return [...a.filter((r) => !ids.has(r.id)), ...incoming];
  };
  // A favorite is unique per plant: keep the local record when the plant is already a favorite.
  const favPlants = new Set(local.favorites.map((f) => f.plantId));
  const favorites = merge(local.favorites, backup.favorites.filter((f) => !favPlants.has(f.plantId) || local.favorites.some((l) => l.id === f.id)));
  return {
    data: {
      favorites,
      gardens: merge(local.gardens, backup.gardens),
      plots: merge(local.plots, backup.plots),
      crops: merge(local.crops, backup.crops),
      tasks: merge(local.tasks, backup.tasks).map((t) => ({ ...t, notificationId: local.tasks.find((l) => l.id === t.id)?.notificationId ?? null })),
      notes: merge(local.notes, backup.notes),
    },
    applied,
  };
}

export const alive = <T extends Base>(list: T[]): T[] => list.filter((r) => !r.deleted);
