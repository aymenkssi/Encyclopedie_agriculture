import { frostMonths } from './calendar';
import { wrapMonth } from './months';
import { Region } from './types';

export type TipKind = 'frost' | 'dry' | 'wet' | 'heat' | 'rainy_start';
export interface Tip { kind: TipKind; months: number[] }

const DRY_MM = 20;
const WET_MM = 150;
const HOT_MEAN_C = 28;

/** Seasonal advice derived from the region's climate normals for the selected months. */
export function seasonalTips(region: Region, months: Set<number>): Tip[] {
  const tips: Tip[] = [];
  const sel = [...months].sort((a, b) => a - b);
  const frost = sel.filter((m) => frostMonths(region).has(m));
  if (frost.length) tips.push({ kind: 'frost', months: frost });
  const p = region.monthlyPrecipMm;
  if (p) {
    const dry = sel.filter((m) => p[m - 1] < DRY_MM);
    if (dry.length) tips.push({ kind: 'dry', months: dry });
    const wet = sel.filter((m) => p[m - 1] > WET_MM);
    if (wet.length) tips.push({ kind: 'wet', months: wet });
  }
  const t = region.monthlyMeanTempC;
  if (t) {
    const hot = sel.filter((m) => t[m - 1] >= HOT_MEAN_C);
    if (hot.length) tips.push({ kind: 'heat', months: hot });
  }
  const rainy = region.rainySeasonMonths ?? [];
  if (rainy.length) {
    const starts = rainy.filter((m) => !rainy.includes(wrapMonth(m - 1)));
    const upcoming = new Set([...sel, ...sel.map((m) => wrapMonth(m + 1))]);
    const s = starts.filter((m) => upcoming.has(m)).sort((a, b) => a - b);
    if (s.length) tips.push({ kind: 'rainy_start', months: s });
  }
  return tips;
}
