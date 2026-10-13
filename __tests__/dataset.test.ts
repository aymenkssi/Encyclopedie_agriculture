import fs from 'fs';
import path from 'path';
import { buildCatalog } from '../lib/catalog';
import { catalog } from '../lib/catalogData';
import { validateCatalog } from '../lib/validator';
import { Plant } from '../lib/types';

const SEED = path.join(__dirname, '..', 'data', 'seed');
const errors = (issues: ReturnType<typeof validateCatalog>) => issues.filter((i) => i.severity === 'error');

describe('seed dataset', () => {
  it('lists every plant file in the manifest, and lib/catalogData.ts is up to date', () => {
    const files = fs.readdirSync(path.join(SEED, 'plants')).filter((f) => f.endsWith('.json')).sort();
    expect([...catalog.manifest.plantFiles].sort()).toEqual(files);
    const generated = fs.readFileSync(path.join(__dirname, '..', 'lib', 'catalogData.ts'), 'utf8');
    files.forEach((f) => expect(generated).toContain(`plants/${f}`)); // run `yarn catalog` if this fails
  });

  it('names each plant file after the plant id', () => {
    catalog.plants.forEach((p) => expect(fs.existsSync(path.join(SEED, 'plants', `${p.id}.json`))).toBe(true));
  });

  it('has no validation error', () => {
    expect(errors(validateCatalog(catalog)).map((i) => `${i.where}: ${i.message}`)).toEqual([]);
  });

  it('is flagged as demonstration data', () => {
    expect(catalog.manifest.demo).toBe(true);
    expect(catalog.plants.every((p) => p.isDemo)).toBe(true);
  });

  it('has complete climate normals for every region', () => {
    Object.values(catalog.regionsById).forEach((r) => {
      expect(r.monthlyMeanTempC).toHaveLength(12);
      expect(r.monthlyPrecipMm).toHaveLength(12);
    });
  });
});

describe('validator', () => {
  const tomato = catalog.plantsById['solanum-lycopersicum'];
  const withPlant = (p: Plant) =>
    errors(validateCatalog(buildCatalog(catalog.manifest, catalog.reference, catalog.geography, catalog.health,
      catalog.plants.map((x) => (x.id === p.id ? p : x)))));

  it('detects unknown references', () => {
    const e = withPlant({ ...tomato, categoryId: 'nope', diseaseIds: ['ghost'] });
    expect(e.some((i) => i.message.includes('unknown category'))).toBe(true);
    expect(e.some((i) => i.message.includes('unknown disease'))).toBe(true);
  });

  it('refuses "verified" without source and on demo content', () => {
    const e = withPlant({ ...tomato, status: 'verified', sourceIds: [] });
    expect(e.some((i) => i.message.includes('requires at least one source'))).toBe(true);
    expect(e.some((i) => i.message.includes('demo content cannot be marked verified'))).toBe(true);
  });

  it('detects inconsistent temperatures', () => {
    const e = withPlant({ ...tomato, temperature: { ...tomato.temperature, min: 30 } });
    expect(e.some((i) => i.message.includes('temperatures'))).toBe(true);
  });
});
