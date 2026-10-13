import fs from 'fs';
import path from 'path';
import { translations } from '../constants/translations';
import { LANGUAGES } from '../lib/types';

const ROOT = path.join(__dirname, '..');
const sourceFiles = (dir: string): string[] => fs.readdirSync(path.join(ROOT, dir), { withFileTypes: true }).flatMap((e) =>
  e.isDirectory() ? sourceFiles(path.join(dir, e.name)) : /\.tsx?$/.test(e.name) ? [path.join(ROOT, dir, e.name)] : []);
const sources = ['app', 'components', 'hooks', 'contexts'].flatMap(sourceFiles).map((f) => fs.readFileSync(f, 'utf8')).join('\n');
// Plural forms may spell the number out (e.g. Arabic singular and dual), so {n} is optional there.
const placeholders = (s: string, key: string) =>
  [...new Set([...s.matchAll(/\{([a-z])\}/g)].map((m) => m[1]))].filter((p) => !(key.includes('__') && p === 'n')).sort().join(',');

describe('translations', () => {
  it('has the same keys and placeholders in every language', () => {
    const fr = translations.fr;
    for (const lang of LANGUAGES) {
      expect(Object.keys(translations[lang]).sort()).toEqual(Object.keys(fr).sort());
      for (const [key, text] of Object.entries(translations[lang])) {
        expect(`${lang}.${key}: ${placeholders(text, key)}`).toBe(`${lang}.${key}: ${placeholders(fr[key], key)}`);
        expect(text.trim()).not.toBe('');
      }
    }
  });

  it('defines every key used by the screens', () => {
    const used = [...sources.matchAll(/\bt\('([a-z0-9_]+)'/g)].map((m) => m[1]);
    const plural = [...sources.matchAll(/\btn\('([a-z0-9_]+)'/g)].map((m) => `${m[1]}__other`);
    const missing = [...new Set([...used, ...plural])].filter((k) => !(k in translations.fr));
    expect(missing).toEqual([]);
  });

  it('defines the keys built from model values', () => {
    const prefixes = { act: 13, diff: 3, water: 3, frost: 3, life: 3, sow: 4, status: 3, suit: 4, season: 6, exp: 3, stage: 6, health: 10, treat: 4, cycle: 3, theme: 3, tip: 5, origin: 5 };
    for (const [prefix, count] of Object.entries(prefixes)) {
      const keys = Object.keys(translations.fr).filter((k) => k.startsWith(`${prefix}_`) && !k.endsWith('_desc'));
      expect(`${prefix}: ${keys.length >= count}`).toBe(`${prefix}: true`);
    }
    for (const key of ['tip_frost', 'tip_dry', 'tip_wet', 'tip_heat', 'tip_rainy_start', 'farm_balcony', 'farm_garden', 'farm_small', 'farm_medium', 'farm_large']) {
      expect(translations.fr[key]).toBeDefined();
    }
  });
});
