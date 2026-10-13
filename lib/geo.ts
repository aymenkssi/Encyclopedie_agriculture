import { Catalog } from './catalog';
import { Region } from './types';

export function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const rad = (d: number) => (d * Math.PI) / 180;
  const a = Math.sin(rad(lat2 - lat1) / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(rad(lon2 - lon1) / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.sqrt(a));
}

/** Nearest known region to a coordinate (only used after explicit user consent), or null beyond maxKm. */
export function nearestRegion(catalog: Catalog, lat: number, lon: number, maxKm = 400): { region: Region; km: number } | null {
  let best: { region: Region; km: number } | null = null;
  for (const region of Object.values(catalog.regionsById)) {
    const km = haversineKm(lat, lon, region.latitude, region.longitude);
    if (!best || km < best.km) best = { region, km };
  }
  return best && best.km <= maxKm ? best : null;
}
