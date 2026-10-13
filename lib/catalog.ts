import { pickNames } from './text';
import {
  ClimateZone, Country, CultivationMethod, DataSource, GeographyData, HealthData, Hemisphere, Pest, Plant,
  PlantCategory, PlantDisease, ReferenceData, Region, SeedManifest, SoilType, Treatment,
} from './types';

/** The whole knowledge base, in memory (read-only). */
export interface Catalog {
  manifest: SeedManifest;
  reference: ReferenceData;
  geography: GeographyData;
  health: HealthData;
  plants: Plant[];
  plantsById: Record<string, Plant>;
  regionsById: Record<string, Region>;
  countriesById: Record<string, Country>;
  diseasesById: Record<string, PlantDisease>;
  pestsById: Record<string, Pest>;
  treatmentsById: Record<string, Treatment>;
  sourcesById: Record<string, DataSource>;
  categoriesById: Record<string, PlantCategory>;
  climateZonesById: Record<string, ClimateZone>;
  soilTypesById: Record<string, SoilType>;
  methodsById: Record<string, CultivationMethod>;
}

const byId = <T extends { id: string }>(list: T[]): Record<string, T> =>
  Object.fromEntries(list.map((item) => [item.id, item]));

export function buildCatalog(
  manifest: SeedManifest, reference: ReferenceData, geography: GeographyData, health: HealthData, plants: Plant[],
): Catalog {
  return {
    manifest, reference, geography, health, plants,
    plantsById: byId(plants),
    regionsById: byId(geography.countries.flatMap((c) => c.regions)),
    countriesById: byId(geography.countries),
    diseasesById: byId(health.diseases),
    pestsById: byId(health.pests),
    treatmentsById: byId(health.treatments),
    sourcesById: byId(reference.sources),
    categoriesById: byId(reference.categories),
    climateZonesById: byId(reference.climateZones),
    soilTypesById: byId(reference.soilTypes),
    methodsById: byId(reference.methods),
  };
}

export function hemisphereOf(catalog: Catalog, region: Region): Hemisphere {
  return catalog.countriesById[region.countryId]?.hemisphere ?? 'north';
}

export function displayName(plant: Plant, lang: string): string {
  return pickNames(plant.names, lang)[0] ?? plant.scientificName;
}

export const plantMethods = (plant: Plant): string[] => (plant.methods?.length ? plant.methods : ['open_field']);
