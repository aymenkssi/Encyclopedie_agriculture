import { Catalog } from './catalog';
import { ALL_METHODS, LANGUAGES, LocalizedText, ValidationStatus } from './types';

export interface Issue { severity: 'error' | 'warning'; where: string; message: string }

/**
 * Integrity checks of the dataset: referential integrity, translations, plausible values and
 * honesty rules (demo data must be flagged, "verified" requires a source).
 */
export function validateCatalog(c: Catalog): Issue[] {
  const issues: Issue[] = [];
  const err = (where: string, message: string) => issues.push({ severity: 'error', where, message });
  const warn = (where: string, message: string) => issues.push({ severity: 'warning', where, message });
  const requireLangs = (where: string, t: LocalizedText | undefined, field: string) =>
    LANGUAGES.filter((l) => !t?.[l]?.trim()).forEach((l) => warn(where, `${field} missing '${l}' translation`));
  const dupes = (kind: string, ids: string[]) =>
    ids.filter((id, i) => ids.indexOf(id) !== i).forEach((id) => err(kind, `duplicate id '${id}'`));

  dupes('plant', c.plants.map((p) => p.id));
  dupes('region', c.geography.countries.flatMap((co) => co.regions.map((r) => r.id)));
  dupes('country', c.geography.countries.map((co) => co.id));
  dupes('source', c.reference.sources.map((s) => s.id));
  dupes('disease', c.health.diseases.map((d) => d.id));
  dupes('pest', c.health.pests.map((p) => p.id));
  dupes('treatment', c.health.treatments.map((t) => t.id));

  const checkSources = (where: string, ids: string[] = [], status: ValidationStatus = 'to_verify') => {
    ids.filter((id) => !c.sourcesById[id]).forEach((id) => err(where, `unknown source '${id}'`));
    if (status === 'verified' && !ids.length) err(where, "status 'verified' requires at least one source");
  };
  ALL_METHODS.filter((m) => !c.methodsById[m]).forEach((m) => err('reference', `missing method '${m}'`));

  for (const country of c.geography.countries) {
    requireLangs(`country ${country.id}`, country.names, 'names');
    for (const r of country.regions) {
      const w = `region ${r.id}`;
      if (r.countryId !== country.id) err(w, `countryId '${r.countryId}' != parent '${country.id}'`);
      if (!c.climateZonesById[r.climateZoneId]) err(w, `unknown climate zone '${r.climateZoneId}'`);
      requireLangs(w, r.names, 'names');
      (r.dominantSoilIds ?? []).filter((s) => !c.soilTypesById[s]).forEach((s) => err(w, `unknown soil '${s}'`));
      checkSources(w, r.sourceIds, r.climateStatus);
      if (Math.abs(r.latitude) > 90 || Math.abs(r.longitude) > 180) err(w, 'invalid coordinates');
      for (const key of ['monthlyMeanTempC', 'monthlyMinTempC', 'monthlyPrecipMm'] as const) {
        const v = r[key];
        if (v && v.length !== 12) err(w, `${key} must have 12 values`);
      }
      const t = r.monthlyMeanTempC;
      if (t) {
        r.monthlyMinTempC?.forEach((mn, i) => { if (mn > t[i]) err(w, `min > mean in month ${i + 1}`); });
        if (t.some((x) => x < -40 || x > 45)) err(w, 'implausible mean temperature');
      } else warn(w, 'no climate normals: calendars cannot be computed');
      if (r.monthlyPrecipMm?.some((p) => p < 0 || p > 1500)) err(w, 'implausible precipitation');
      [...(r.frostMonths ?? []), ...(r.rainySeasonMonths ?? [])].filter((m) => m < 1 || m > 12).forEach((m) => err(w, `invalid month ${m}`));
    }
  }

  for (const p of c.plants) {
    const w = `plant ${p.id}`;
    if (!/^[a-z0-9-]+$/.test(p.id)) err(w, 'id must be a lowercase slug');
    if (!c.categoriesById[p.categoryId]) err(w, `unknown category '${p.categoryId}'`);
    LANGUAGES.filter((l) => !p.names[l]?.length).forEach((l) => err(w, `missing common name in '${l}'`));
    requireLangs(w, p.description, 'description');
    (p.soilIds ?? []).filter((s) => !c.soilTypesById[s]).forEach((s) => err(w, `unknown soil '${s}'`));
    (p.methods ?? []).filter((m) => !c.methodsById[m]).forEach((m) => err(w, `unknown method '${m}'`));
    (p.climateZoneIds ?? []).filter((z) => !c.climateZonesById[z]).forEach((z) => err(w, `unknown climate zone '${z}'`));
    (p.diseaseIds ?? []).filter((d) => !c.diseasesById[d]).forEach((d) => err(w, `unknown disease '${d}'`));
    (p.pestIds ?? []).filter((d) => !c.pestsById[d]).forEach((d) => err(w, `unknown pest '${d}'`));
    (p.companions ?? []).filter((co) => !c.plantsById[co.otherPlantId]).forEach((co) => warn(w, `companion '${co.otherPlantId}' not in catalog`));
    (p.companions ?? []).filter((co) => co.otherPlantId === p.id).forEach(() => err(w, 'plant cannot be its own companion'));
    checkSources(w, p.sourceIds, p.status);
    if (c.manifest.demo && !p.isDemo) err(w, 'demo dataset: plant must be flagged isDemo=true');
    if (p.isDemo && p.status === 'verified') err(w, 'demo content cannot be marked verified');
    const t = p.temperature ?? {};
    const ordered = [t.min, t.optimalMin, t.optimalMax, t.max].filter((x): x is number => x != null);
    if (ordered.some((x, i) => i > 0 && x < ordered[i - 1])) err(w, 'temperatures must satisfy min <= optimalMin <= optimalMax <= max');
    if (p.ph && (p.ph.min < 3 || p.ph.max > 10)) err(w, 'implausible pH range');
    if (p.lifecycle === 'perennial' && !p.phenology && !p.calendar?.length) warn(w, 'perennial without phenology or calendar');
    if (p.lifecycle !== 'perennial' && !p.cycleDays) warn(w, 'annual without cycleDays');
    for (const e of p.calendar ?? []) {
      if (e.months.some((m) => m < 1 || m > 12)) err(w, 'calendar months must be within 1..12');
      if (e.regionId && !c.regionsById[e.regionId]) err(w, `calendar: unknown region '${e.regionId}'`);
      if (e.climateZoneId && !c.climateZonesById[e.climateZoneId]) err(w, `calendar: unknown zone '${e.climateZoneId}'`);
      if (!e.regionId && !e.climateZoneId) err(w, 'calendar entry must be scoped to a region or a climate zone');
      if (e.varietyId && !(p.varieties ?? []).some((v) => v.id === e.varietyId)) err(w, `calendar: unknown variety '${e.varietyId}'`);
      checkSources(`${w} calendar`, e.sourceIds, e.status);
    }
    for (const g of p.guides ?? []) {
      const orders = g.stages.map((s) => s.order);
      if (orders.some((o, i) => i > 0 && o < orders[i - 1])) err(w, `guide ${g.id}: stages not ordered`);
      g.stages.forEach((s) => { requireLangs(`${w} guide ${g.id} stage ${s.order}`, s.title, 'title'); requireLangs(`${w} guide ${g.id} stage ${s.order}`, s.body, 'body'); });
    }
  }
  for (const d of c.health.diseases) {
    (d.treatmentIds ?? []).filter((t) => !c.treatmentsById[t]).forEach((t) => err(`disease ${d.id}`, `unknown treatment '${t}'`));
    checkSources(`disease ${d.id}`, d.sourceIds, d.status);
    requireLangs(`disease ${d.id}`, d.names, 'names');
  }
  for (const pe of c.health.pests) {
    (pe.treatmentIds ?? []).filter((t) => !c.treatmentsById[t]).forEach((t) => err(`pest ${pe.id}`, `unknown treatment '${t}'`));
    checkSources(`pest ${pe.id}`, pe.sourceIds, pe.status);
    requireLangs(`pest ${pe.id}`, pe.names, 'names');
  }
  for (const t of c.health.treatments) {
    checkSources(`treatment ${t.id}`, t.sourceIds, t.status);
    if (t.type === 'chemical' && !Object.keys(t.caution ?? {}).length) err(`treatment ${t.id}`, 'chemical treatment requires a caution text');
  }
  return issues;
}
