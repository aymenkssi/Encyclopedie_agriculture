import { catalog } from '../lib/catalogData';
import { rotationConflicts } from '../lib/rotation';
import { seasonalTips } from '../lib/tips';

const tomato = catalog.plantsById['solanum-lycopersicum'];

describe('rotation advice', () => {
  it('warns when the same family was grown recently on the plot', () => {
    const recent = rotationConflicts(catalog, tomato, [{ plantId: 'solanum-tuberosum', startDate: '2026-03-01' }], '2026-10-13');
    expect(recent.map((p) => p.id)).toEqual(['solanum-tuberosum']);
  });

  it('ignores crops older than the rotation delay', () => {
    expect(rotationConflicts(catalog, tomato, [{ plantId: 'solanum-tuberosum', startDate: '2015-03-01' }], '2026-10-13')).toEqual([]);
  });
});

describe('seasonal tips', () => {
  it('only uses the selected months', () => {
    const region = catalog.geography.countries.flatMap((c) => c.regions).find((r) => r.monthlyPrecipMm)!;
    const all = seasonalTips(region, new Set([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]));
    for (const tip of all) expect(tip.months.length).toBeGreaterThan(0);
    for (const tip of seasonalTips(region, new Set([7]))) expect(tip.months.every((m) => m === 7 || m === 8)).toBe(true);
  });
});
