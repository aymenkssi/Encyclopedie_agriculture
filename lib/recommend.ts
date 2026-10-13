import { Catalog, plantMethods } from './catalog';
import { activitiesIn, computeCalendar, EXTREME_MIN_MARGIN_C, Note, PlantCalendar } from './calendar';
import { Activity, METHODS, Plant, Region, START_ACTIVITIES } from './types';

export type ExperienceLevel = 'beginner' | 'intermediate' | 'expert';
export const EXPERIENCE_LEVELS: ExperienceLevel[] = ['beginner', 'intermediate', 'expert'];
export type FarmType = 'balcony' | 'garden' | 'small_farm' | 'medium_farm' | 'large_farm';
export const FARM_TYPES: FarmType[] = ['balcony', 'garden', 'small_farm', 'medium_farm', 'large_farm'];
export const defaultMethodFor = (farm: FarmType): string => (farm === 'balcony' ? METHODS.CONTAINER : METHODS.OPEN_FIELD);

export type SuitabilityLevel = 'recommended' | 'possible' | 'not_recommended' | 'unknown';
const LEVEL_ORDER: SuitabilityLevel[] = ['recommended', 'possible', 'unknown', 'not_recommended'];

export interface Recommendation {
  plant: Plant;
  level: SuitabilityLevel;
  /** 0..100, ranking only. */
  score: number;
  activities: Activity[];
  calendar: PlantCalendar;
  notes: Note[];
}

export interface RecommendationQuery {
  region: Region;
  months: Set<number>;
  methodId?: string;
  experience?: ExperienceLevel;
  farmType?: FarmType | null;
  categoryId?: string | null;
}

/** Rule-based suitability (docs/RECOMMENDATION_RULES.md). Never presented as a guarantee. */
export function evaluate(catalog: Catalog, plant: Plant, q: RecommendationQuery): Recommendation {
  const region = q.region;
  const methodId = q.methodId ?? METHODS.OPEN_FIELD;
  const experience = q.experience ?? 'beginner';
  const calendar = computeCalendar(catalog, plant, region, methodId);
  const notes: Note[] = [...calendar.notes];
  let blocking = false;
  if (!plantMethods(plant).includes(methodId)) {
    notes.push({ code: 'method_not_suited' });
    blocking = true;
  }
  const zones = plant.climateZoneIds ?? [];
  if (zones.length && !zones.includes(region.climateZoneId) && methodId !== METHODS.HYDROPONICS) notes.push({ code: 'zone_not_listed' });
  if (plant.lifecycle === 'perennial') blocking = perennialBlocking(plant, region, notes) || blocking;

  const activities = [...new Set([...q.months].flatMap((m) => activitiesIn(calendar, m)))];
  const starts = activities.filter((a) => START_ACTIVITIES.includes(a));
  const hasExplicit = (plant.calendar ?? []).some((e) => e.regionId === region.id || e.climateZoneId === region.climateZoneId);

  let level: SuitabilityLevel;
  if (!region.monthlyMeanTempC && !hasExplicit) level = 'unknown';
  else if (blocking || !calendar.available) level = 'not_recommended';
  // Outside the climate zones where the crop is usually grown: never more than "possible".
  else if (starts.length && !notes.some((n) => n.code === 'zone_not_listed')) level = 'recommended';
  else level = 'possible';

  if (experience === 'beginner' && plant.difficulty === 'hard') notes.push({ code: 'difficult_for_beginner' });

  let score = { recommended: 70, possible: 40, unknown: 15, not_recommended: 0 }[level];
  if (level !== 'not_recommended') {
    score += plant.difficulty === 'easy' ? (experience === 'beginner' ? 15 : 8)
      : plant.difficulty === 'medium' ? 6 : experience === 'expert' ? 6 : -10;
    if (calendar.windows.some((w) => START_ACTIVITIES.includes(w.activity) && [...w.preferred].some((m) => q.months.has(m)))) score += 10;
    if (notes.some((n) => n.code === 'zone_not_listed')) score -= 10;
    if (notes.some((n) => n.code === 'irrigation_required' && (n.months ?? []).some((m) => q.months.has(m)))) score -= 3;
  }
  return { plant, level, score: Math.max(0, Math.min(100, score)), activities, calendar, notes };
}

/** Ranked recommendations; not-recommended plants are excluded unless includeAll. */
export function recommend(catalog: Catalog, q: RecommendationQuery, includeAll = false): Recommendation[] {
  const query = { ...q, methodId: q.farmType === 'balcony' ? METHODS.CONTAINER : q.methodId };
  return catalog.plants
    .filter((p) => !query.categoryId || p.categoryId === query.categoryId)
    .map((p) => evaluate(catalog, p, query))
    .filter((r) => includeAll || r.level !== 'not_recommended')
    .sort((a, b) => LEVEL_ORDER.indexOf(a.level) - LEVEL_ORDER.indexOf(b.level) || b.score - a.score || a.plant.id.localeCompare(b.plant.id));
}

function perennialBlocking(plant: Plant, region: Region, notes: Note[]): boolean {
  let blocking = false;
  const means = region.monthlyMeanTempC;
  const mins = region.monthlyMinTempC;
  const hardiness = plant.temperature?.hardinessMin;
  if (hardiness != null) {
    // Typical absolute minima are several degrees below the coldest monthly mean minimum.
    const coldest = mins ? Math.min(...mins) - EXTREME_MIN_MARGIN_C : means ? Math.min(...means) - EXTREME_MIN_MARGIN_C - 5 : null;
    if (coldest != null && coldest < hardiness) {
      notes.push({ code: 'winter_too_cold', value: hardiness });
      blocking = true;
    }
  }
  const ph = plant.phenology;
  if (ph?.requiresWinterChill && means) {
    const limit = ph.chillColdestMonthMaxC ?? 7;
    if (Math.min(...means) > limit) {
      notes.push({ code: 'no_winter_chill', value: limit });
      blocking = true;
    }
  }
  const opt = plant.temperature?.optimalMin;
  if (opt != null && means && Math.max(...means) < opt) notes.push({ code: 'summer_too_cool', value: opt });
  return blocking;
}
