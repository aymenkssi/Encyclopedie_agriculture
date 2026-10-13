import { computeCalendar, frostMonths, monthsFor } from '../lib/calendar';
import { catalog } from '../lib/catalogData';
import { evaluate, recommend } from '../lib/recommend';
import { monthRanges, seasonMonths, currentSeason, spanMonths, wrapMonth } from '../lib/months';
import { plantMethods } from '../lib/catalog';

const tomato = catalog.plantsById['solanum-lycopersicum'];
const region = (id: string) => catalog.regionsById[id];
const sorted = (s: Set<number>) => [...s].sort((a, b) => a - b);

describe('calendar engine', () => {
  it('uses explicit climate-zone entries first', () => {
    const cal = computeCalendar(catalog, tomato, region('fr-bre'), 'open_field');
    const w = cal.windows.find((x) => x.activity === 'transplanting')!;
    expect(w.origin).toBe('explicit_zone');
    expect(sorted(w.months)).toEqual([5, 6]);
    expect(cal.notes.some((n) => n.code === 'explicit_data')).toBe(true);
  });

  it('never transplants a frost-sensitive crop during frost months (Quebec)', () => {
    const cal = computeCalendar(catalog, tomato, region('ca-qc'));
    const transplant = monthsFor(cal, 'transplanting');
    const frost = frostMonths(region('ca-qc'));
    expect(transplant.size).toBeGreaterThan(0);
    expect([...transplant].every((m) => !frost.has(m) && m >= 6 && m <= 8)).toBe(true);
  });

  it('avoids the hottest months in Kaolack', () => {
    const cal = computeCalendar(catalog, tomato, region('sn-kao'));
    expect(monthsFor(cal, 'transplanting').has(5)).toBe(false);
    expect(cal.notes.some((n) => n.code === 'too_hot')).toBe(true);
  });

  it('is all year in hydroponics', () => {
    const cal = computeCalendar(catalog, tomato, region('ca-qc'), 'hydroponics');
    expect(monthsFor(cal, 'direct_sowing').size).toBe(12);
  });

  it('extends the season under greenhouse', () => {
    const plain = { ...tomato, calendar: [] };
    const open = monthsFor(computeCalendar(catalog, plain, region('fr-als'), 'open_field'), 'transplanting');
    const gh = monthsFor(computeCalendar(catalog, plain, region('fr-als'), 'greenhouse'), 'transplanting');
    expect([...open].every((m) => gh.has(m))).toBe(true);
    expect(gh.size).toBeGreaterThan(open.size);
  });

  it('is unavailable without climate data', () => {
    const r = { ...region('fr-idf'), monthlyMeanTempC: null, monthlyMinTempC: null };
    const cal = computeCalendar(catalog, { ...tomato, calendar: [] }, r);
    expect(cal.available).toBe(false);
    expect(cal.notes.some((n) => n.code === 'no_climate_data')).toBe(true);
  });

  it('prefers the start of the rainy season', () => {
    const crop = { ...tomato, id: 'test-crop', calendar: [], sowingMode: 'direct' as const, water: 'medium' as const,
      temperature: { min: 18, max: 35 }, cycleDays: { min: 90, max: 100 }, frostTolerance: 'none' as const };
    const cal = computeCalendar(catalog, crop, region('sn-kao'));
    const w = cal.windows.find((x) => x.activity === 'direct_sowing')!;
    expect(w.preferred.size).toBeGreaterThan(0);
    expect([...w.preferred].every((m) => m === 6 || m === 7)).toBe(true);
  });

  it('never sows an annual all year in a temperate climate', () => {
    catalog.plants.filter((p) => p.lifecycle !== 'perennial').forEach((p) => {
      const cal = computeCalendar(catalog, p, region('fr-idf'), plantMethods(p)[0]);
      const starts = new Set([...monthsFor(cal, 'direct_sowing'), ...monthsFor(cal, 'transplanting'), ...monthsFor(cal, 'planting')]);
      expect([p.id, starts.size < 12]).toEqual([p.id, true]);
    });
  });

  it('never harvests an annual during the Quebec winter', () => {
    catalog.plants.filter((p) => p.lifecycle !== 'perennial').forEach((p) => {
      const harvest = monthsFor(computeCalendar(catalog, p, region('ca-qc'), plantMethods(p)[0]), 'harvest');
      expect([p.id, [12, 1, 2].some((m) => harvest.has(m))]).toEqual([p.id, false]);
    });
  });
});

describe('recommendations', () => {
  it('suggests tomato in May in Brittany, only "possible" in January', () => {
    expect(evaluate(catalog, tomato, { region: region('fr-bre'), months: new Set([5]) }).level).toBe('recommended');
    expect(evaluate(catalog, tomato, { region: region('fr-bre'), months: new Set([1]) }).level).toBe('possible');
  });

  it('refuses an unsupported growing method', () => {
    const r = evaluate(catalog, { ...tomato, methods: ['open_field'] }, { region: region('fr-bre'), months: new Set([5]), methodId: 'container' });
    expect(r.level).toBe('not_recommended');
  });

  it('keeps crops outside their climate zones at most "possible"', () => {
    const r = evaluate(catalog, catalog.plantsById['olea-europaea'], { region: region('fr-idf'), months: new Set([3]) });
    expect(r.level).not.toBe('recommended');
  });

  it('uses containers for balconies and ranks by level', () => {
    const list = recommend(catalog, { region: region('fr-paca'), months: new Set([4]), farmType: 'balcony' });
    expect(list.every((r) => plantMethods(r.plant).includes('container'))).toBe(true);
    const all = recommend(catalog, { region: region('ma-rsk'), months: new Set([3]) }, true);
    const order = ['recommended', 'possible', 'unknown', 'not_recommended'];
    const ranks = all.map((r) => order.indexOf(r.level));
    expect(ranks).toEqual([...ranks].sort((a, b) => a - b));
  });
});

describe('months and seasons', () => {
  it('wraps and groups months', () => {
    expect([wrapMonth(13), wrapMonth(0), wrapMonth(-1)]).toEqual([1, 12, 11]);
    expect(spanMonths(11, 3)).toEqual([11, 12, 1]);
    expect(monthRanges(new Set([11, 12, 1, 4]))).toEqual([[4, 4], [11, 1]]);
    expect(monthRanges(new Set([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]))).toEqual([[1, 12]]);
  });

  it('shifts seasons in the south and uses rainy seasons in the tropics', () => {
    expect(sorted(seasonMonths('summer', null, 'south'))).toEqual([1, 2, 12]);
    expect(sorted(seasonMonths('rainy', region('sn-dkr'), 'north'))).toEqual([7, 8, 9, 10]);
    expect(currentSeason(2, region('sn-dkr'), 'north')).toBe('dry');
  });
});
