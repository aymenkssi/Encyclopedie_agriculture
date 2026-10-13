import { Catalog, displayName, plantMethods } from './catalog';
import { computeCalendar, startActivitiesIn } from './calendar';
import { Difficulty, Plant, Region, WaterNeed } from './types';

// Latin accents removed by hand: String.normalize is not guaranteed on every JS engine (Hermes).
const LATIN: Record<string, string> = {
  à: 'a', á: 'a', â: 'a', ã: 'a', ä: 'a', å: 'a', ç: 'c', è: 'e', é: 'e', ê: 'e', ë: 'e', ì: 'i', í: 'i', î: 'i', ï: 'i',
  ñ: 'n', ò: 'o', ó: 'o', ô: 'o', õ: 'o', ö: 'o', ù: 'u', ú: 'u', û: 'u', ü: 'u', ý: 'y', ÿ: 'y', œ: 'oe', æ: 'ae', ß: 'ss', '×': 'x',
};

/** Accent-, case- and script-variant-insensitive form (Latin and Arabic). */
export function normalize(input: string): string {
  let s = input.toLowerCase();
  s = s.replace(/[ؐ-ًؚ-ٰٟۖ-ۭـ]/g, ''); // Arabic diacritics, tatweel
  s = s.replace(/[أإآٱ]/g, 'ا').replace(/ة/g, 'ه').replace(/ى/g, 'ي').replace(/ؤ/g, 'و').replace(/ئ/g, 'ي');
  s = s.replace(/[^\u0000-\u007F]/g, (c) => LATIN[c] ?? c);
  try { s = s.normalize('NFD').replace(/[̀-ͯ]/g, ''); } catch { /* engine without normalize */ }
  return s.replace(/[^\p{L}\p{Nd}]+/gu, ' ').trim();
}

/** Optimal string alignment (restricted Damerau-Levenshtein) distance with an early-exit bound. */
export function editDistance(a: string, b: string, max = Number.MAX_SAFE_INTEGER): number {
  if (a === b) return 0;
  if (Math.abs(a.length - b.length) > max) return max + 1;
  if (!a.length) return b.length;
  if (!b.length) return a.length;
  let prev2 = new Array(b.length + 1).fill(0);
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  let cur = new Array(b.length + 1).fill(0);
  for (let i = 1; i <= a.length; i++) {
    cur[0] = i;
    let rowMin = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      let v = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost);
      if (i > 1 && j > 1 && a[i - 1] === b[j - 2] && a[i - 2] === b[j - 1]) v = Math.min(v, prev2[j - 2] + 1);
      cur[j] = v;
      rowMin = Math.min(rowMin, v);
    }
    if (rowMin > max) return max + 1;
    [prev2, prev, cur] = [prev, cur, prev2];
  }
  return prev[b.length];
}

export type CycleLength = 'short' | 'medium' | 'long';
const CYCLE_BOUNDS: Record<CycleLength, [number, number]> = { short: [0, 75], medium: [75, 150], long: [150, Infinity] };

/** Combinable filters: every non-empty criterion must match. */
export interface PlantFilter {
  categoryIds?: string[];
  difficulties?: Difficulty[];
  waterNeeds?: WaterNeed[];
  methodIds?: string[];
  soilIds?: string[];
  climateZoneIds?: string[];
  cycle?: CycleLength | null;
  /** With months: the calendar must allow a sowing / planting in one of them. */
  region?: Region | null;
  months?: number[];
  countryId?: string | null;
}

export const activeFilterCount = (f: PlantFilter): number =>
  [f.categoryIds?.length, f.difficulties?.length, f.waterNeeds?.length, f.methodIds?.length, f.soilIds?.length,
    f.climateZoneIds?.length, f.cycle, f.region && f.months?.length, f.countryId].filter(Boolean).length;

export interface SearchHit { plant: Plant; score: number; matchedName: string | null }

function allowedTypos(q: string): number { return q.length <= 7 ? 1 : 2; }

/** How well normalized query `q` matches normalized term `t` (0 = no match). */
export function termScore(q: string, t: string): number {
  if (t === q) return 100;
  if (t.startsWith(q)) return 90 - (t.length - q.length) * 0.1;
  const words = t.split(' ');
  if (words.some((w) => w.startsWith(q))) return 80;
  if (q.length >= 3 && t.includes(q)) return 70;
  const fuzzy = (qt: string, w: string) => {
    if (qt.length < 3) return false;
    const target = w.length > qt.length ? w.slice(0, qt.length) : w;
    return editDistance(qt, target, allowedTypos(qt)) <= allowedTypos(qt);
  };
  const tokens = q.split(' ').filter(Boolean);
  if (tokens.length > 1 && tokens.every((qt) => words.some((w) => w.startsWith(qt) || fuzzy(qt, w)))) return 65;
  if (q.length >= 3) {
    const allowed = allowedTypos(q);
    let best = Infinity;
    for (const w of [...words, t]) {
      best = Math.min(best, editDistance(q, w, allowed));
      if (w.length > q.length) best = Math.min(best, editDistance(q, w.slice(0, q.length), allowed));
    }
    if (best <= allowed) return 50 - best * 10;
  }
  return 0;
}

/**
 * In-memory search tolerant to typos, accents and Arabic spelling variants, over common names
 * in every language, scientific names, synonyms, family, category and variety names.
 */
export class PlantSearch {
  private index: { plant: Plant; terms: [string, string][] }[];

  constructor(private catalog: Catalog) {
    this.index = catalog.plants.map((p) => {
      const originals = [
        ...Object.values(p.names).flat().filter((n): n is string => !!n),
        p.scientificName, ...(p.synonyms ?? []), p.family,
        ...Object.values(catalog.categoriesById[p.categoryId]?.names ?? {}).filter((n): n is string => !!n),
        ...(p.varieties ?? []).map((v) => v.name),
      ];
      const seen = new Set<string>();
      const terms = originals.map((o) => [normalize(o), o] as [string, string]).filter(([n]) => n && !seen.has(n) && !!seen.add(n));
      return { plant: p, terms };
    });
  }

  search(query: string, filter: PlantFilter = {}, lang = 'fr'): SearchHit[] {
    const q = normalize(query);
    const candidates = this.index.filter((e) => this.matches(e.plant, filter));
    if (!q) {
      return candidates.map((e) => ({ plant: e.plant, score: 0, matchedName: null }))
        .sort((a, b) => displayName(a.plant, lang).localeCompare(displayName(b.plant, lang), lang));
    }
    const hits: SearchHit[] = [];
    for (const e of candidates) {
      let best = 0;
      let name: string | null = null;
      for (const [norm, original] of e.terms) {
        const s = termScore(q, norm);
        if (s > best) { best = s; name = original; }
      }
      if (best > 0) hits.push({ plant: e.plant, score: best, matchedName: name });
    }
    return hits.sort((a, b) => b.score - a.score || displayName(a.plant, lang).localeCompare(displayName(b.plant, lang), lang));
  }

  matches(p: Plant, f: PlantFilter): boolean {
    const has = <T,>(list: T[] | undefined, v: T) => !list?.length || list.includes(v);
    const any = <T,>(list: T[] | undefined, values: T[]) => !list?.length || values.some((v) => list.includes(v));
    if (!has(f.categoryIds, p.categoryId) || !has(f.difficulties, p.difficulty) || !has(f.waterNeeds, p.water)) return false;
    if (!any(f.methodIds, plantMethods(p)) || !any(f.soilIds, p.soilIds ?? []) || !any(f.climateZoneIds, p.climateZoneIds ?? [])) return false;
    if (f.cycle) {
      const days = p.cycleDays?.min;
      if (days == null) return false;
      const [low, high] = CYCLE_BOUNDS[f.cycle];
      if (days <= low || days > high) return false;
    }
    if (f.region && f.months?.length) {
      const method = f.methodIds?.length === 1 ? f.methodIds[0] : plantMethods(p)[0];
      const cal = computeCalendar(this.catalog, p, f.region, method);
      if (!f.months.some((m) => startActivitiesIn(cal, m).length)) return false;
    }
    if (f.countryId) {
      const regions = this.catalog.countriesById[f.countryId]?.regions ?? [];
      if (!regions.length) return false;
      const zones = new Set(regions.map((r) => r.climateZoneId));
      const ids = new Set(regions.map((r) => r.id));
      const ok = (p.calendar ?? []).some((e) => e.regionId && ids.has(e.regionId)) ||
        !(p.climateZoneIds ?? []).length || (p.climateZoneIds ?? []).some((z) => zones.has(z));
      if (!ok) return false;
    }
    return true;
  }
}
