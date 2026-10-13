import { catalog } from '../lib/catalogData';
import { editDistance, normalize, PlantSearch } from '../lib/search';

const search = new PlantSearch(catalog);
const top = (q: string) => search.search(q)[0]?.plant.id;

describe('search', () => {
  it('finds a plant by its names in every language, scientific name and synonym', () => {
    expect(top('tomate')).toBe('solanum-lycopersicum');
    expect(top('Tomato')).toBe('solanum-lycopersicum');
    expect(top('طماطم')).toBe('solanum-lycopersicum');
    expect(top('بندوره')).toBe('solanum-lycopersicum');
    expect(top('solanum lyco')).toBe('solanum-lycopersicum');
    expect(top('Lycopersicon esculentum')).toBe('solanum-lycopersicum');
  });

  it('tolerates typos without confusing similar crops', () => {
    expect(top('tomtae')).toBe('solanum-lycopersicum');
    expect(top('tommate')).toBe('solanum-lycopersicum');
    expect(search.search('tomate').map((h) => h.plant.id)).toEqual(['solanum-lycopersicum']);
    expect(search.search('xyzxyz')).toEqual([]);
  });

  it('finds every plant by each of its names', () => {
    catalog.plants.forEach((p) => {
      Object.values(p.names).flat().forEach((n) => expect([n, search.search(n!).some((h) => h.plant.id === p.id)]).toEqual([n, true]));
      expect(search.search(p.scientificName)[0].plant.id).toBe(p.id);
    });
  });

  it('combines filters', () => {
    const veg = search.search('', { categoryIds: ['vegetable'] });
    expect(veg.length).toBeGreaterThan(0);
    expect(search.search('', { categoryIds: ['vegetable'], difficulties: ['easy'] }).every((h) => h.plant.difficulty === 'easy')).toBe(true);
    const bre = catalog.regionsById['fr-bre'];
    expect(search.search('tomate', { region: bre, months: [5] })).toHaveLength(1);
    expect(search.search('tomate', { region: bre, months: [1] })).toHaveLength(0);
  });

  it('normalizes accents, ligatures and Arabic diacritics', () => {
    expect(normalize('Blé')).toBe('ble');
    expect(normalize('Cœur-de-bœuf')).toBe('coeur de boeuf');
    expect(normalize('طَمَاطِم')).toBe('طماطم');
    expect(editDistance('abc', 'acb')).toBe(1);
    expect(editDistance('', 'abc')).toBe(3);
  });
});
