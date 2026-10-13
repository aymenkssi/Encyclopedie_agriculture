import { Catalog, hemisphereOf } from './catalog';
import { ALL_MONTHS, shiftMonths, spanMonths, wrapMonth } from './months';
import { Activity, Hemisphere, METHODS, Plant, Region, SeasonalCalendarEntry, START_ACTIVITIES, ValidationStatus, WaterNeed } from './types';

/** Where a calendar window comes from. */
export type CalendarOrigin = 'explicit_region' | 'explicit_zone' | 'computed' | 'phenology' | 'controlled';

/**
 * Machine-readable explanations attached to calendars and recommendations. The UI turns
 * them into localized sentences, so every language gets exactly the same reasoning.
 */
export type NoteCode =
  | 'no_climate_data' | 'no_cycle_data' | 'greenhouse_offset' | 'controlled_environment' | 'nursery_sheltered'
  | 'frost_risk' | 'irrigation_required' | 'rainy_season_aligned' | 'no_winter_chill' | 'winter_too_cold'
  | 'summer_too_cool' | 'too_hot' | 'southern_shift' | 'tropical_perennial' | 'no_window' | 'method_not_suited'
  | 'zone_not_listed' | 'difficult_for_beginner' | 'explicit_data' | 'indicative_only';

export interface Note { code: NoteCode; months?: number[]; value?: number }

export interface ActivityWindow {
  activity: Activity;
  months: Set<number>;
  /** Subset of months that is preferable (e.g. start of the rainy season). */
  preferred: Set<number>;
  origin: CalendarOrigin;
  status: ValidationStatus;
}

export interface PlantCalendar {
  plantId: string;
  regionId: string;
  methodId: string;
  available: boolean;
  windows: ActivityWindow[];
  notes: Note[];
}

export const GREENHOUSE_OFFSET_C = 4;
const FROST_MIN_THRESHOLD_C = 2;
const FROST_MEAN_THRESHOLD_C = 7;
export const EXTREME_MIN_MARGIN_C = 7;
const DEFAULT_CYCLE_MONTHS = 3;
const STATUS_RANK: Record<ValidationStatus, number> = { verified: 0, indicative: 1, to_verify: 2 };

export const rainfedThresholdMm = (water: WaterNeed): number => ({ low: 20, medium: 50, high: 80 })[water];

export const monthsFor = (cal: PlantCalendar, activity: Activity): Set<number> =>
  new Set(cal.windows.filter((w) => w.activity === activity).flatMap((w) => [...w.months]));
export const activitiesIn = (cal: PlantCalendar, month: number): Activity[] =>
  [...new Set(cal.windows.filter((w) => w.months.has(month)).map((w) => w.activity))];
export const startActivitiesIn = (cal: PlantCalendar, month: number): Activity[] =>
  activitiesIn(cal, month).filter((a) => START_ACTIVITIES.includes(a));

/** Months with frost risk: explicit data, else monthly minima, else monthly means. */
export function frostMonths(region: Region, methodId: string = METHODS.OPEN_FIELD): Set<number> {
  const offset = methodId === METHODS.GREENHOUSE ? GREENHOUSE_OFFSET_C : 0;
  if (methodId !== METHODS.GREENHOUSE && region.frostMonths) return new Set(region.frostMonths);
  if (region.monthlyMinTempC) return new Set(ALL_MONTHS.filter((m) => region.monthlyMinTempC![m - 1] + offset < FROST_MIN_THRESHOLD_C));
  if (region.monthlyMeanTempC) return new Set(ALL_MONTHS.filter((m) => region.monthlyMeanTempC![m - 1] + offset < FROST_MEAN_THRESHOLD_C));
  return new Set();
}

const dedupeNotes = (notes: Note[]): Note[] => {
  const seen = new Set<string>();
  return notes.filter((n) => {
    const key = `${n.code}|${(n.months ?? []).join(',')}|${n.value ?? ''}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
};

/**
 * Indicative cultivation calendar of a plant in a region.
 * Priority: explicit regional entries > explicit climate-zone entries > computed windows.
 * The computation checks, month by month, mean temperatures, frost, rainfall and the rainy
 * season against the plant's needs over the whole cycle: never calendar seasons alone.
 */
export function computeCalendar(
  catalog: Catalog, plant: Plant, region: Region, methodId: string = METHODS.OPEN_FIELD, varietyId?: string | null,
): PlantCalendar {
  const hemisphere = hemisphereOf(catalog, region);
  const notes: Note[] = [];
  let base: ActivityWindow[];
  if (methodId === METHODS.HYDROPONICS) base = controlled(plant, notes);
  else if (plant.lifecycle === 'perennial' && plant.phenology) base = perennial(plant, region, hemisphere, notes);
  else base = annual(plant, region, methodId, notes);

  const explicit = explicitWindows(plant, region, hemisphere, methodId, varietyId ?? null);
  let windows = base;
  if (explicit.length) {
    notes.push({ code: 'explicit_data' });
    const replaced = new Set(explicit.map((w) => w.activity));
    windows = [...base.filter((w) => !replaced.has(w.activity)), ...explicit];
  }
  const available = windows.length > 0;
  if (!available && !notes.some((n) => n.code === 'no_climate_data')) notes.push({ code: 'no_window' });
  notes.push({ code: 'indicative_only' });
  const order = (a: Activity) => ['soil_preparation', 'nursery_sowing', 'direct_sowing', 'transplanting', 'planting', 'watering',
    'fertilizing', 'pruning', 'flowering', 'health_monitoring', 'treatment', 'harvest', 'rotation_planning'].indexOf(a);
  windows.sort((a, b) => order(a.activity) - order(b.activity));
  return { plantId: plant.id, regionId: region.id, methodId, available, windows, notes: dedupeNotes(notes) };
}

function explicitWindows(plant: Plant, region: Region, hemisphere: Hemisphere, methodId: string, varietyId: string | null): ActivityWindow[] {
  const entries = plant.calendar ?? [];
  const matches = (e: SeasonalCalendarEntry) =>
    (!e.methodId || e.methodId === methodId) && (!e.varietyId || e.varietyId === varietyId);
  let chosen = entries.filter((e) => e.regionId === region.id && matches(e));
  let origin: CalendarOrigin = 'explicit_region';
  if (!chosen.length) {
    chosen = entries.filter((e) => !e.regionId && e.climateZoneId === region.climateZoneId &&
      (!e.hemisphere || e.hemisphere === hemisphere) && matches(e));
    origin = 'explicit_zone';
  }
  const byActivity = new Map<Activity, SeasonalCalendarEntry[]>();
  chosen.forEach((e) => byActivity.set(e.activity, [...(byActivity.get(e.activity) ?? []), e]));
  return [...byActivity.entries()].map(([activity, list]) => {
    const specific = list.filter((e) => e.varietyId || e.methodId);
    const used = specific.length ? specific : list;
    const status = used.map((e) => e.status ?? 'to_verify').sort((a, b) => STATUS_RANK[b] - STATUS_RANK[a])[0];
    return { activity, months: new Set(used.flatMap((e) => e.months)), preferred: new Set<number>(), origin, status };
  });
}

function controlled(plant: Plant, notes: Note[]): ActivityWindow[] {
  notes.push({ code: 'controlled_environment' });
  const all = new Set(ALL_MONTHS);
  const w = (activity: Activity): ActivityWindow => ({ activity, months: all, preferred: new Set(), origin: 'controlled', status: 'indicative' });
  return [w(plant.sowingMode === 'vegetative' ? 'planting' : 'direct_sowing'), w('harvest'), w('health_monitoring')];
}

function annual(plant: Plant, region: Region, methodId: string, notes: Note[]): ActivityWindow[] {
  if (!region.monthlyMeanTempC) {
    notes.push({ code: 'no_climate_data' });
    return [];
  }
  const offset = methodId === METHODS.GREENHOUSE ? GREENHOUSE_OFFSET_C : 0;
  const temps = region.monthlyMeanTempC.map((t) => t + offset);
  if (offset > 0) notes.push({ code: 'greenhouse_offset', value: GREENHOUSE_OFFSET_C });

  const frost = frostMonths(region, methodId);
  if (frost.size && plant.frostTolerance !== 'hardy') notes.push({ code: 'frost_risk', months: [...frost] });
  const tr = plant.temperature ?? {};
  if (tr.max != null) {
    const hot = ALL_MONTHS.filter((m) => temps[m - 1] > tr.max!);
    if (hot.length) notes.push({ code: 'too_hot', months: hot, value: tr.max });
  }

  const cycle = plant.cycleDays;
  if (!cycle) notes.push({ code: 'no_cycle_data' });
  const cycleMonths = cycle ? Math.min(12, Math.max(1, Math.ceil((cycle.min + cycle.max) / 2 / 30))) : DEFAULT_CYCLE_MONTHS;

  const isGrowable = (m: number): boolean => {
    const t = temps[m - 1];
    if (tr.min != null && t < tr.min) return false;
    if (tr.max != null && t > tr.max) return false;
    if (frost.has(m)) {
      if (plant.frostTolerance === 'none') return false;
      if (plant.frostTolerance === 'light' && region.monthlyMinTempC && region.monthlyMinTempC[m - 1] + offset < -3) return false;
    }
    return true;
  };
  const growable = new Set(ALL_MONTHS.filter(isGrowable));
  // Warmth needed on average over the cycle and at harvest: halfway between the minimum and
  // the optimal temperature (a crop that merely survives does not mature).
  const warmth = tr.min != null && tr.optimalMin != null ? (tr.min + tr.optimalMin) / 2 : tr.min ?? tr.optimalMin;
  const minHarvest = cycle ? Math.floor(cycle.min / 30) : cycleMonths;
  const maxHarvest = Math.max(minHarvest, cycle ? Math.floor(cycle.max / 30) : cycleMonths);
  const warmEnough = (m: number) => warmth == null || temps[m - 1] >= warmth;
  const range = (a: number, b: number) => Array.from({ length: b - a + 1 }, (_, i) => a + i);

  const feasible = new Set(ALL_MONTHS.filter((s) => {
    const span = spanMonths(s, cycleMonths);
    const avg = span.reduce((sum, m) => sum + temps[m - 1], 0) / span.length;
    return span.every((m) => growable.has(m)) && (warmth == null || avg >= warmth) &&
      range(minHarvest, maxHarvest).some((k) => warmEnough(wrapMonth(s + k)));
  }));
  const germinationOk = new Set(ALL_MONTHS.filter((m) => tr.germinationMin == null || temps[m - 1] >= tr.germinationMin));

  const rainy = new Set(region.rainySeasonMonths ?? []);
  const rainfed = methodId === METHODS.OPEN_FIELD || methodId === METHODS.CONTAINER;
  const preferred = (starts: Set<number>): Set<number> => {
    if (!rainy.size || !rainfed) return new Set();
    // Starting in the first months of the rainy season lets the crop use the rains.
    const early = [...rainy].filter((m) => !rainy.has(wrapMonth(m - 1)) || !rainy.has(wrapMonth(m - 2)));
    return new Set(early.filter((m) => starts.has(m)));
  };

  const windows: ActivityWindow[] = [];
  const add = (activity: Activity, months: Set<number>, pref: Set<number> = new Set()) =>
    windows.push({ activity, months, preferred: pref, origin: 'computed', status: 'indicative' });
  const starts = new Set<number>();
  const direct = new Set([...feasible].filter((m) => germinationOk.has(m)));

  if ((plant.sowingMode === 'direct' || plant.sowingMode === 'both') && direct.size) {
    add('direct_sowing', direct, preferred(direct));
    direct.forEach((m) => starts.add(m));
  }
  if ((plant.sowingMode === 'transplant' || plant.sowingMode === 'both') && feasible.size) {
    add('transplanting', feasible, preferred(feasible));
    const weeks = plant.nurseryWeeks ? (plant.nurseryWeeks.min + plant.nurseryWeeks.max) / 2 : 6;
    add('nursery_sowing', shiftMonths(feasible, -Math.max(1, Math.ceil(weeks / 4.345))));
    notes.push({ code: 'nursery_sheltered' });
    feasible.forEach((m) => starts.add(m));
  }
  if (plant.sowingMode === 'vegetative' && direct.size) {
    add('planting', direct, preferred(direct));
    direct.forEach((m) => starts.add(m));
  }
  if (!starts.size) return [];

  const harvest = new Set<number>();
  const growing = new Set<number>();
  for (const s of starts) {
    for (let k = minHarvest; k <= maxHarvest; k++) if (warmEnough(wrapMonth(s + k))) harvest.add(wrapMonth(s + k));
    for (let k = 0; k < cycleMonths; k++) growing.add(wrapMonth(s + k));
  }
  const minus = (a: Set<number>, b: Set<number>) => new Set([...a].filter((m) => !b.has(m)));
  add('harvest', harvest);
  add('soil_preparation', minus(shiftMonths(starts, -1), starts));
  add('health_monitoring', growing);
  add('fertilizing', growing);
  add('rotation_planning', minus(shiftMonths(harvest, 1), harvest));

  if (region.monthlyPrecipMm) {
    const threshold = rainfedThresholdMm(plant.water);
    const dry = new Set([...growing].filter((m) => methodId !== METHODS.OPEN_FIELD || region.monthlyPrecipMm![m - 1] < threshold));
    if (dry.size) {
      add('watering', dry);
      if (methodId === METHODS.OPEN_FIELD) notes.push({ code: 'irrigation_required', months: [...dry] });
    }
  } else {
    add('watering', growing);
  }
  if (windows.some((w) => w.preferred.size)) notes.push({ code: 'rainy_season_aligned', months: [...rainy] });
  return windows;
}

function perennial(plant: Plant, region: Region, hemisphere: Hemisphere, notes: Note[]): ActivityWindow[] {
  const ph = plant.phenology!;
  const shift = hemisphere === 'south' ? 6 : 0;
  if (shift) notes.push({ code: 'southern_shift' });
  const means = region.monthlyMeanTempC;
  const tropical = !!region.rainySeasonMonths?.length || (!!means && Math.min(...means) >= 18);
  if (tropical) notes.push({ code: 'tropical_perennial' });
  const status: ValidationStatus = tropical ? 'to_verify' : 'indicative';
  const list: ActivityWindow[] = [];
  const add = (activity: Activity, months: number[] | undefined) => {
    if (months?.length) list.push({ activity, months: shiftMonths(months, shift), preferred: new Set(), origin: 'phenology', status });
  };
  add('planting', ph.plantingMonthsNorth);
  add('pruning', ph.pruningMonthsNorth);
  add('flowering', ph.floweringMonthsNorth);
  add('harvest', ph.harvestMonthsNorth);
  const planting = ph.plantingMonthsNorth ?? [];
  add('soil_preparation', [...shiftMonths(planting, -1)].filter((m) => !planting.includes(m)));
  const active = [...(ph.floweringMonthsNorth ?? []), ...(ph.harvestMonthsNorth ?? [])];
  if (active.length) {
    const f = ph.floweringMonthsNorth?.length ? Math.min(...ph.floweringMonthsNorth) : Math.min(...active);
    const h = ph.harvestMonthsNorth?.length ? Math.max(...ph.harvestMonthsNorth) : Math.max(...active);
    const span = spanMonths(f, wrapMonth(h - f + 1));
    add('health_monitoring', span);
    if (region.monthlyPrecipMm) {
      const threshold = rainfedThresholdMm(plant.water);
      const dry = [...shiftMonths(span, shift)].filter((m) => region.monthlyPrecipMm![m - 1] < threshold);
      if (dry.length) {
        list.push({ activity: 'watering', months: new Set(dry), preferred: new Set(), origin: 'phenology', status });
        notes.push({ code: 'irrigation_required', months: dry });
      }
    }
  }
  return list;
}

/** Weakest status among the windows of a calendar. */
export function calendarStatus(cal: PlantCalendar): ValidationStatus {
  return cal.windows.map((w) => w.status).sort((a, b) => STATUS_RANK[b] - STATUS_RANK[a])[0] ?? 'to_verify';
}
