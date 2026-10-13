import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, ReactNode } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { addDays, IsoDate, today } from '../lib/dates';
import { cancelReminder, scheduleTaskReminder } from '../lib/notify';
import { FarmType } from '../lib/recommend';
import { planTasks } from '../lib/tasks';
import { Activity, Plant } from '../lib/types';
import {
  alive, Base, BACKUP_FORMAT, Crop, CropStage, EMPTY_DATA, Garden, mergeBackup, newId, Note, Plot, sanitizeUserData, Task, UserData,
} from '../lib/userData';
import { useSettings } from './SettingsContext';

const DATA_KEY = 'user_data_v1';
const HISTORY_KEY = 'history_v1';
const DEVICE_KEY = 'device_id';
const MAX_HISTORY = 100;

export interface TaskInput {
  title: string;
  description?: string | null;
  activity: Activity;
  dueDate: IsoDate;
  plantId?: string | null;
  cropId?: string | null;
  reminder: boolean;
  repeatDays?: number | null;
}

export interface CropInput {
  gardenId: string | null;
  plotId: string | null;
  plant: Plant;
  methodId: string;
  startActivity: Activity;
  startDate: IsoDate;
  stage: CropStage;
  quantity: string | null;
  planTasks: boolean;
  /** Title of the generated tasks, e.g. "Harvest · Tomato". */
  taskTitle: (activity: Activity) => string;
  defaultGardenName: string;
  defaultRegionId: string | null;
  defaultFarmType: FarmType;
}

interface UserDataContextType {
  loaded: boolean;
  data: UserData;
  history: string[];
  favoriteIds: string[];
  isFavorite: (plantId: string) => boolean;
  toggleFavorite: (plantId: string) => void;
  markViewed: (plantId: string) => void;
  clearHistory: () => void;
  saveGarden: (id: string | null, fields: Pick<Garden, 'name' | 'farmType' | 'areaM2' | 'regionId'>) => string;
  deleteGarden: (id: string) => void;
  savePlot: (fields: Pick<Plot, 'gardenId' | 'name' | 'areaM2' | 'soilId' | 'methodId'>) => void;
  deletePlot: (id: string) => void;
  addCrop: (input: CropInput) => Promise<string>;
  setCropStage: (id: string, stage: CropStage) => void;
  deleteCrop: (id: string) => void;
  saveTask: (id: string | null, input: TaskInput, reminderBody: string) => Promise<void>;
  setTaskDone: (id: string, done: boolean, reminderBody: string) => Promise<void>;
  deleteTask: (id: string) => void;
  saveNote: (id: string | null, fields: Pick<Note, 'title' | 'body' | 'plantId' | 'cropId'>) => void;
  deleteNote: (id: string) => void;
  exportBackup: () => string;
  importBackup: (json: string) => number;
}

const UserDataContext = createContext<UserDataContextType | undefined>(undefined);

export const UserDataProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const settings = useSettings();
  const [data, setData] = useState<UserData>(EMPTY_DATA);
  const [history, setHistory] = useState<string[]>([]);
  const [loaded, setLoaded] = useState(false);
  const deviceId = useRef('device');
  // Latest data for async actions (avoids stale closures).
  const dataRef = useRef(data);
  useEffect(() => { dataRef.current = data; }, [data]);

  useEffect(() => {
    Promise.all([AsyncStorage.getItem(DATA_KEY), AsyncStorage.getItem(HISTORY_KEY), AsyncStorage.getItem(DEVICE_KEY)])
      .then(([saved, hist, device]) => {
        if (saved) setData(sanitizeUserData(JSON.parse(saved)));
        if (hist) setHistory((JSON.parse(hist) as unknown[]).filter((x): x is string => typeof x === 'string'));
        if (device) deviceId.current = device;
        else {
          deviceId.current = newId();
          AsyncStorage.setItem(DEVICE_KEY, deviceId.current).catch(() => undefined);
        }
      })
      .catch((error) => console.error('Error loading user data:', error))
      .finally(() => setLoaded(true));
  }, []);

  /** Applies a change and saves it on the phone. */
  const commit = useCallback((fn: (d: UserData) => UserData) => {
    setData((current) => {
      const next = fn(current);
      dataRef.current = next;
      AsyncStorage.setItem(DATA_KEY, JSON.stringify(next)).catch((error) => console.error('Error saving user data:', error));
      return next;
    });
  }, []);

  const stamp = useCallback(<T extends Base>(r: T, patch: Partial<T> = {}): T =>
    ({ ...r, ...patch, updatedAt: Date.now(), deviceId: deviceId.current }), []);
  const fresh = useCallback((): Base => {
    const now = Date.now();
    return { id: newId(), createdAt: now, updatedAt: now, deleted: false, deviceId: deviceId.current };
  }, []);
  const replace = <T extends Base>(list: T[], id: string, fn: (r: T) => T): T[] => list.map((r) => (r.id === id ? fn(r) : r));

  const favoriteIds = useMemo(() => alive(data.favorites).map((f) => f.plantId), [data.favorites]);
  const isFavorite = useCallback((plantId: string) => favoriteIds.includes(plantId), [favoriteIds]);

  const toggleFavorite = useCallback((plantId: string) => commit((d) => {
    const existing = d.favorites.find((f) => f.plantId === plantId);
    if (!existing) return { ...d, favorites: [...d.favorites, { ...fresh(), plantId }] };
    return { ...d, favorites: replace(d.favorites, existing.id, (f) => stamp(f, { deleted: !f.deleted })) };
  }), [commit, fresh, stamp]);

  const markViewed = useCallback((plantId: string) => setHistory((h) => {
    const next = [plantId, ...h.filter((x) => x !== plantId)].slice(0, MAX_HISTORY);
    AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(next)).catch(() => undefined);
    return next;
  }), []);
  const clearHistory = useCallback(() => { setHistory([]); AsyncStorage.removeItem(HISTORY_KEY).catch(() => undefined); }, []);

  const saveGarden = useCallback((id: string | null, fields: Pick<Garden, 'name' | 'farmType' | 'areaM2' | 'regionId'>) => {
    const gid = id ?? newId();
    commit((d) => id
      ? { ...d, gardens: replace(d.gardens, id, (g) => stamp(g, fields)) }
      : { ...d, gardens: [...d.gardens, { ...fresh(), id: gid, ...fields }] });
    return gid;
  }, [commit, fresh, stamp]);
  const deleteGarden = useCallback((id: string) => commit((d) => ({ ...d, gardens: replace(d.gardens, id, (g) => stamp(g, { deleted: true })) })), [commit, stamp]);
  const savePlot = useCallback((fields: Pick<Plot, 'gardenId' | 'name' | 'areaM2' | 'soilId' | 'methodId'>) =>
    commit((d) => ({ ...d, plots: [...d.plots, { ...fresh(), ...fields }] })), [commit, fresh]);
  const deletePlot = useCallback((id: string) => commit((d) => ({ ...d, plots: replace(d.plots, id, (p) => stamp(p, { deleted: true })) })), [commit, stamp]);

  const remind = useCallback(async (task: Pick<Task, 'title' | 'dueDate' | 'reminder' | 'done'>, body: string) =>
    task.reminder && !task.done && settings.notificationsEnabled ? scheduleTaskReminder(task.title, body, task.dueDate) : null,
  [settings.notificationsEnabled]);

  const addCrop = useCallback(async (input: CropInput) => {
    const d0 = dataRef.current;
    let gardenId = input.gardenId;
    const gardens: Garden[] = [];
    if (!gardenId || !d0.gardens.some((g) => g.id === gardenId && !g.deleted)) {
      const garden: Garden = { ...fresh(), name: input.defaultGardenName, farmType: input.defaultFarmType, areaM2: null, regionId: input.defaultRegionId };
      gardens.push(garden);
      gardenId = garden.id;
    }
    const crop: Crop = {
      ...fresh(), gardenId, plotId: input.plotId, plantId: input.plant.id, varietyId: null, methodId: input.methodId,
      startActivity: input.startActivity, startDate: input.startDate, stage: input.stage, quantity: input.quantity?.trim() || null, harvestedOn: null,
    };
    const tasks: Task[] = [];
    if (input.planTasks) {
      for (const p of planTasks(input.plant, input.startActivity, input.startDate, today())) {
        const title = input.taskTitle(p.activity);
        const base: Task = {
          ...fresh(), title, description: null, activity: p.activity, dueDate: p.date, done: false, doneAt: null,
          plantId: input.plant.id, cropId: crop.id, reminder: true, repeatDays: null, origin: 'planned', notificationId: null,
        };
        tasks.push({ ...base, notificationId: await remind(base, title) });
      }
    }
    commit((d) => ({ ...d, gardens: [...d.gardens, ...gardens], crops: [...d.crops, crop], tasks: [...d.tasks, ...tasks] }));
    return crop.id;
  }, [commit, fresh, remind]);

  const setCropStage = useCallback((id: string, stage: CropStage) => commit((d) => ({
    ...d, crops: replace(d.crops, id, (c) => stamp(c, { stage, harvestedOn: stage === 'finished' ? today() : c.harvestedOn })),
  })), [commit, stamp]);
  const deleteCrop = useCallback((id: string) => commit((d) => ({ ...d, crops: replace(d.crops, id, (c) => stamp(c, { deleted: true })) })), [commit, stamp]);

  const saveTask = useCallback(async (id: string | null, input: TaskInput, reminderBody: string) => {
    const existing = id ? dataRef.current.tasks.find((t) => t.id === id) : undefined;
    await cancelReminder(existing?.notificationId);
    const fields = {
      title: input.title.trim(), description: input.description?.trim() || null, activity: input.activity, dueDate: input.dueDate,
      plantId: input.plantId ?? null, cropId: input.cropId ?? existing?.cropId ?? null, reminder: input.reminder, repeatDays: input.repeatDays ?? null,
    };
    const base: Task = existing
      ? stamp(existing, fields)
      : { ...fresh(), ...fields, done: false, doneAt: null, origin: 'user', notificationId: null };
    const task = { ...base, notificationId: await remind(base, reminderBody) };
    commit((d) => ({ ...d, tasks: existing ? replace(d.tasks, task.id, () => task) : [...d.tasks, task] }));
  }, [commit, fresh, remind, stamp]);

  /** Marks a task done (or not). A repeating task spawns its next occurrence. */
  const setTaskDone = useCallback(async (id: string, done: boolean, reminderBody: string) => {
    const task = dataRef.current.tasks.find((t) => t.id === id);
    if (!task) return;
    if (done) await cancelReminder(task.notificationId);
    const updated = stamp(task, { done, doneAt: done ? Date.now() : null, notificationId: done ? null : task.notificationId });
    let next: Task | null = null;
    if (done && !task.done && task.repeatDays && task.repeatDays > 0) {
      const from = task.dueDate > today() ? task.dueDate : today();
      const base: Task = { ...task, ...fresh(), dueDate: addDays(from, task.repeatDays), done: false, doneAt: null, notificationId: null };
      next = { ...base, notificationId: await remind(base, reminderBody) };
    }
    commit((d) => ({ ...d, tasks: [...replace(d.tasks, id, () => updated), ...(next ? [next] : [])] }));
  }, [commit, fresh, remind, stamp]);

  const deleteTask = useCallback((id: string) => {
    cancelReminder(dataRef.current.tasks.find((t) => t.id === id)?.notificationId);
    commit((d) => ({ ...d, tasks: replace(d.tasks, id, (t) => stamp(t, { deleted: true, notificationId: null })) }));
  }, [commit, stamp]);

  const saveNote = useCallback((id: string | null, fields: Pick<Note, 'title' | 'body' | 'plantId' | 'cropId'>) => commit((d) => ({
    ...d,
    notes: id
      ? replace(d.notes, id, (n) => stamp(n, { title: fields.title.trim(), body: fields.body.trim() }))
      : [...d.notes, { ...fresh(), ...fields, title: fields.title.trim(), body: fields.body.trim() }],
  })), [commit, fresh, stamp]);
  const deleteNote = useCallback((id: string) => commit((d) => ({ ...d, notes: replace(d.notes, id, (n) => stamp(n, { deleted: true })) })), [commit, stamp]);

  const exportBackup = useCallback(() => JSON.stringify({
    format: BACKUP_FORMAT, schemaVersion: 1, exportedAt: Date.now(), settings, ...dataRef.current,
  }, null, 2), [settings]);

  const importBackup = useCallback((json: string) => {
    const parsed = JSON.parse(json);
    if (parsed?.format !== BACKUP_FORMAT) throw new Error('Not an AgriPedia backup');
    const { data: merged, applied } = mergeBackup(dataRef.current, sanitizeUserData(parsed));
    commit(() => merged);
    return applied;
  }, [commit]);

  const value = useMemo(() => ({
    loaded, data, history, favoriteIds, isFavorite, toggleFavorite, markViewed, clearHistory, saveGarden, deleteGarden, savePlot,
    deletePlot, addCrop, setCropStage, deleteCrop, saveTask, setTaskDone, deleteTask, saveNote, deleteNote, exportBackup, importBackup,
  }), [loaded, data, history, favoriteIds, isFavorite, toggleFavorite, markViewed, clearHistory, saveGarden, deleteGarden, savePlot,
    deletePlot, addCrop, setCropStage, deleteCrop, saveTask, setTaskDone, deleteTask, saveNote, deleteNote, exportBackup, importBackup]);

  return <UserDataContext.Provider value={value}>{children}</UserDataContext.Provider>;
};

export const useUserData = () => {
  const context = useContext(UserDataContext);
  if (!context) throw new Error('useUserData must be used within UserDataProvider');
  return context;
};
