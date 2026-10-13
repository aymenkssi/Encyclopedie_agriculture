import { catalog } from '../lib/catalogData';
import { addDays, daysBetween } from '../lib/dates';
import { nearestRegion } from '../lib/geo';
import { mergeRecords } from '../lib/sync';
import { MAX_HEALTH_CHECKS, planTasks } from '../lib/tasks';

const tomato = catalog.plantsById['solanum-lycopersicum'];

describe('task planner', () => {
  it('derives milestones from the user date and the plant data', () => {
    const tasks = planTasks(tomato, 'nursery_sowing', '2026-03-01', '2026-03-01');
    const transplant = tasks.find((t) => t.activity === 'transplanting')!;
    expect(transplant.date).toBe(addDays('2026-03-01', 35));
    expect(daysBetween(transplant.date, tasks.find((t) => t.activity === 'harvest')!.date)).toBe(60);
    const checks = tasks.filter((t) => t.activity === 'health_monitoring').length;
    expect(checks).toBeGreaterThan(0);
    expect(checks).toBeLessThanOrEqual(MAX_HEALTH_CHECKS);
  });

  it('invents no date without data', () => {
    expect(planTasks({ ...tomato, nurseryWeeks: undefined }, 'nursery_sowing', '2026-03-01', '2026-03-01')).toEqual([]);
  });
});

describe('sync merge', () => {
  it('keeps the last writer and never duplicates', () => {
    const r = mergeRecords(
      [{ id: 'a', updatedAt: 10, deleted: false, deviceId: 'd1' }, { id: 'b', updatedAt: 5, deleted: false, deviceId: 'd1' }, { id: 'c', updatedAt: 1, deleted: false, deviceId: 'd1' }],
      [{ id: 'a', updatedAt: 5, deleted: false, deviceId: 'd2' }, { id: 'b', updatedAt: 7, deleted: true, deviceId: 'd2' }, { id: 'd', updatedAt: 3, deleted: false, deviceId: 'd2' }],
    );
    expect(r.pushRemote.map((x) => x.id).sort()).toEqual(['a', 'c']);
    expect(r.applyLocally.map((x) => x.id).sort()).toEqual(['b', 'd']);
  });

  it('lets a deletion win on an exact tie', () => {
    const r = mergeRecords([{ id: 'x', updatedAt: 9, deleted: false, deviceId: 'd1' }], [{ id: 'x', updatedAt: 9, deleted: true, deviceId: 'd2' }]);
    expect(r.applyLocally.map((x) => x.id)).toEqual(['x']);
  });
});

describe('nearest region', () => {
  it('finds Brittany near Rennes and nothing in Antarctica', () => {
    const near = nearestRegion(catalog, 48.2, -1.5)!;
    expect(near.region.id).toBe('fr-bre');
    expect(near.km).toBeLessThan(50);
    expect(nearestRegion(catalog, -80, 0)).toBeNull();
  });
});
