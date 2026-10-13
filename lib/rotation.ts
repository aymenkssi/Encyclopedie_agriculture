import { Catalog } from './catalog';
import { IsoDate } from './dates';
import { Plant } from './types';

/**
 * Plants grown recently on the same plot that conflict with `plant` (same family, or a
 * predecessor family to avoid), within the rotation delay of the plant (at least one year).
 */
export function rotationConflicts(
  catalog: Catalog, plant: Plant, plotCrops: { plantId: string; startDate: IsoDate }[], todayDate: IsoDate,
): Plant[] {
  const years = Math.max(1, plant.rotation?.minYearsBeforeReturn ?? 0);
  const limit = `${Number(todayDate.slice(0, 4)) - years}${todayDate.slice(4)}`;
  const avoid = plant.rotation?.avoidPredecessorFamilies ?? [];
  const found = plotCrops
    .filter((c) => c.startDate >= limit)
    .map((c) => catalog.plantsById[c.plantId])
    .filter((p): p is Plant => !!p && (p.family === plant.family || avoid.includes(p.family)));
  return [...new Map(found.map((p) => [p.id, p])).values()];
}
