/** Calendar dates as "yyyy-mm-dd" strings (no time zone surprises), with day arithmetic. */
export type IsoDate = string;

const pad = (n: number) => String(n).padStart(2, '0');

export function toIso(d: Date): IsoDate {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function today(): IsoDate {
  return toIso(new Date());
}

export function parseIso(s: IsoDate): Date {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d, 12);
}

export function addDays(s: IsoDate, days: number): IsoDate {
  const d = parseIso(s);
  d.setDate(d.getDate() + Math.round(days));
  return toIso(d);
}

export function daysBetween(a: IsoDate, b: IsoDate): number {
  return Math.round((parseIso(b).getTime() - parseIso(a).getTime()) / 86_400_000);
}

export const monthOf = (s: IsoDate): number => Number(s.slice(5, 7));
export const isValidIso = (s: unknown): s is IsoDate => typeof s === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(s) && !isNaN(parseIso(s).getTime());
