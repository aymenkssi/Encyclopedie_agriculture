import { Hemisphere, Region } from './types';

/** Month helpers (months are 1..12 and wrap around the year). */
export const ALL_MONTHS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

export const wrapMonth = (m: number): number => ((((m - 1) % 12) + 12) % 12) + 1;
export const shiftMonths = (months: Iterable<number>, by: number): Set<number> =>
  new Set([...months].map((m) => wrapMonth(m + by)));
/** `count` consecutive months starting at `start`. */
export const spanMonths = (start: number, count: number): number[] =>
  Array.from({ length: count }, (_, i) => wrapMonth(start + i));

/** Contiguous, wrap-aware ranges, e.g. {11,12,1,4} -> [[4,4],[11,1]]. */
export function monthRanges(months: Set<number>): [number, number][] {
  if (months.size === 0) return [];
  if (months.size === 12) return [[1, 12]];
  const starts = [...months].filter((m) => !months.has(wrapMonth(m - 1))).sort((a, b) => a - b);
  return starts.map((s) => {
    let e = s;
    while (months.has(wrapMonth(e + 1))) e = wrapMonth(e + 1);
    return [s, e];
  });
}

export type Season = 'spring' | 'summer' | 'autumn' | 'winter' | 'rainy' | 'dry';

export function seasonsFor(region?: Region | null): Season[] {
  return region?.rainySeasonMonths?.length
    ? ['rainy', 'dry', 'spring', 'summer', 'autumn', 'winter']
    : ['spring', 'summer', 'autumn', 'winter'];
}

/** Months of a season; astronomical seasons are shifted by 6 months in the south. */
export function seasonMonths(season: Season, region: Region | null | undefined, hemisphere: Hemisphere): Set<number> {
  const rainy = region?.rainySeasonMonths ?? [];
  if (season === 'rainy') return new Set(rainy);
  if (season === 'dry') return rainy.length ? new Set(ALL_MONTHS.filter((m) => !rainy.includes(m))) : new Set();
  const north: Record<string, number[]> = { spring: [3, 4, 5], summer: [6, 7, 8], autumn: [9, 10, 11], winter: [12, 1, 2] };
  const set = new Set(north[season]);
  return hemisphere === 'south' ? shiftMonths(set, 6) : set;
}

export function currentSeason(month: number, region: Region | null | undefined, hemisphere: Hemisphere): Season {
  const rainy = region?.rainySeasonMonths;
  if (rainy?.length) return rainy.includes(month) ? 'rainy' : 'dry';
  return (['spring', 'summer', 'autumn', 'winter'] as Season[]).find((s) => seasonMonths(s, region, hemisphere).has(month))!;
}
