import { addDays, IsoDate } from './dates';
import { Activity, Plant } from './types';

/** A task proposed from the user's own crop dates (never a universal reminder). */
export interface PlannedTask { activity: Activity; date: IsoDate; estimated: true }

export const HEALTH_CHECK_INTERVAL_DAYS = 14;
export const MAX_HEALTH_CHECKS = 8;

/**
 * Milestones of a crop from the date the user actually sowed or planted it, using the
 * plant's own cycle data. When data is missing, no date is invented.
 */
export function planTasks(plant: Plant, startActivity: Activity, startDate: IsoDate, todayDate: IsoDate): PlannedTask[] {
  const tasks: PlannedTask[] = [];
  let inPlace = startDate;
  if (startActivity === 'nursery_sowing' && (plant.sowingMode === 'transplant' || plant.sowingMode === 'both')) {
    const weeks = plant.nurseryWeeks?.min;
    if (weeks == null) return tasks;
    inPlace = addDays(startDate, weeks * 7);
    tasks.push({ activity: 'transplanting', date: inPlace, estimated: true });
  }
  const cycle = plant.cycleDays;
  if (cycle && plant.lifecycle !== 'perennial') {
    const harvest = addDays(inPlace, cycle.min);
    tasks.push({ activity: 'harvest', date: harvest, estimated: true });
    let d = addDays(inPlace, HEALTH_CHECK_INTERVAL_DAYS);
    for (let n = 0; d < harvest && n < MAX_HEALTH_CHECKS; n++) {
      tasks.push({ activity: 'health_monitoring', date: d, estimated: true });
      d = addDays(d, HEALTH_CHECK_INTERVAL_DAYS);
    }
    tasks.push({ activity: 'rotation_planning', date: addDays(inPlace, cycle.max), estimated: true });
  }
  return tasks.filter((t) => t.date >= todayDate).sort((a, b) => a.date.localeCompare(b.date));
}
