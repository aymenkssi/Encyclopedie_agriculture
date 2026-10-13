/**
 * Domain model of the AgriPedia knowledge base (same JSON format as data/seed).
 * Identifiers are stable string slugs (e.g. `solanum-lycopersicum`) so that user data
 * (favorites, crops, notes) survives content updates.
 */

export type Language = 'fr' | 'ar' | 'en';
export const LANGUAGES: Language[] = ['fr', 'ar', 'en'];

/** Language code -> text, e.g. {"fr": "Tomate", "ar": "طماطم"}. */
export type LocalizedText = Partial<Record<string, string>>;
/** Language code -> names (first is the preferred one). */
export type LocalizedNames = Partial<Record<string, string[]>>;

export type ValidationStatus = 'verified' | 'indicative' | 'to_verify';
export type Hemisphere = 'north' | 'south';
export type Lifecycle = 'annual' | 'biennial' | 'perennial';
export type Difficulty = 'easy' | 'medium' | 'hard';
export type FrostTolerance = 'none' | 'light' | 'hardy';
export type SunExposure = 'full_sun' | 'partial_shade' | 'shade';
export type WaterNeed = 'low' | 'medium' | 'high';
export type SowingMode = 'direct' | 'transplant' | 'both' | 'vegetative';
export type Activity =
  | 'soil_preparation'
  | 'nursery_sowing'
  | 'direct_sowing'
  | 'transplanting'
  | 'planting'
  | 'watering'
  | 'fertilizing'
  | 'pruning'
  | 'flowering'
  | 'health_monitoring'
  | 'treatment'
  | 'harvest'
  | 'rotation_planning';

export const ACTIVITIES: Activity[] = [
  'soil_preparation', 'nursery_sowing', 'direct_sowing', 'transplanting', 'planting', 'watering', 'fertilizing',
  'pruning', 'flowering', 'health_monitoring', 'treatment', 'harvest', 'rotation_planning',
];
export const START_ACTIVITIES: Activity[] = ['nursery_sowing', 'direct_sowing', 'transplanting', 'planting'];

export const METHODS = { OPEN_FIELD: 'open_field', GREENHOUSE: 'greenhouse', CONTAINER: 'container', HYDROPONICS: 'hydroponics' } as const;
export const ALL_METHODS = Object.values(METHODS) as string[];

export type HealthIssueType =
  | 'fungal' | 'oomycete' | 'bacterial' | 'viral' | 'physiological' | 'insect' | 'mite' | 'mollusc' | 'nematode' | 'other';
export type TreatmentType = 'cultural' | 'mechanical' | 'biological' | 'chemical';

export interface Range { min: number; max: number }

export interface DataSource {
  id: string;
  title: string;
  publisher?: string;
  url?: string;
  license?: string;
  kind: string;
  accessedOn?: string | null;
  notes?: LocalizedText;
}

export interface NamedItem { id: string; names: LocalizedText; description?: LocalizedText }
export interface PlantCategory extends NamedItem { order?: number }
export interface ClimateZone extends NamedItem { koppen?: string[] }
export type SoilType = NamedItem;
export type CultivationMethod = NamedItem;

export interface Region {
  id: string;
  countryId: string;
  names: LocalizedText;
  latitude: number;
  longitude: number;
  altitudeM?: number;
  climateZoneId: string;
  koppen?: string;
  /** 12 monthly mean temperatures (°C), January first. */
  monthlyMeanTempC?: number[] | null;
  /** 12 monthly means of daily minimum temperatures (°C). */
  monthlyMinTempC?: number[] | null;
  /** 12 monthly precipitation totals (mm). */
  monthlyPrecipMm?: number[] | null;
  /** Months (1..12) with frost risk; overrides the estimate from minima. */
  frostMonths?: number[] | null;
  /** Rainy season months (tropical / semi-arid regions). */
  rainySeasonMonths?: number[] | null;
  dominantSoilIds?: string[];
  climateStatus?: ValidationStatus;
  sourceIds?: string[];
  notes?: LocalizedText;
}

export interface Country { id: string; names: LocalizedText; hemisphere: Hemisphere; regions: Region[] }

export interface TemperatureRange {
  min?: number;
  optimalMin?: number;
  optimalMax?: number;
  max?: number;
  germinationMin?: number;
  hardinessMin?: number;
}

export interface PlantVariety { id: string; name: string; description?: LocalizedText; cycleDays?: Range; status?: ValidationStatus }

export interface GrowingStage {
  order: number;
  title: LocalizedText;
  body: LocalizedText;
  tips?: LocalizedText;
  mistakes?: LocalizedText;
  methods?: string[];
}

export interface GrowingGuide { id: string; title: LocalizedText; stages: GrowingStage[]; status?: ValidationStatus }

export interface SeasonalCalendarEntry {
  activity: Activity;
  months: number[];
  regionId?: string;
  climateZoneId?: string;
  hemisphere?: Hemisphere;
  varietyId?: string;
  methodId?: string;
  status?: ValidationStatus;
  assumptions?: LocalizedText;
  sourceIds?: string[];
}

export interface PlantCompanion { otherPlantId: string; relation: 'good' | 'bad'; reason?: LocalizedText; status?: ValidationStatus }

export interface CropRotation {
  group: string;
  minYearsBeforeReturn?: number;
  goodPredecessorFamilies?: string[];
  avoidPredecessorFamilies?: string[];
  notes?: LocalizedText;
  status?: ValidationStatus;
}

export interface PerennialPhenology {
  plantingMonthsNorth?: number[];
  pruningMonthsNorth?: number[];
  floweringMonthsNorth?: number[];
  harvestMonthsNorth?: number[];
  requiresWinterChill?: boolean;
  chillColdestMonthMaxC?: number;
}

export interface Plant {
  id: string;
  scientificName: string;
  scientificAuthority?: string;
  synonyms?: string[];
  family: string;
  categoryId: string;
  names: LocalizedNames;
  description?: LocalizedText;
  lifecycle: Lifecycle;
  difficulty: Difficulty;
  temperature?: TemperatureRange;
  frostTolerance: FrostTolerance;
  sun: SunExposure;
  soilIds?: string[];
  ph?: Range;
  water: WaterNeed;
  cycleDays?: Range;
  spacing?: { inRowCm?: Range; betweenRowsCm?: Range };
  sowingDepthCm?: Range;
  sowingMode: SowingMode;
  nurseryWeeks?: Range;
  methods?: string[];
  climateZoneIds?: string[];
  yieldInfo?: LocalizedText;
  yieldFactors?: LocalizedText;
  storage?: LocalizedText;
  varieties?: PlantVariety[];
  guides?: GrowingGuide[];
  calendar?: SeasonalCalendarEntry[];
  phenology?: PerennialPhenology;
  companions?: PlantCompanion[];
  rotation?: CropRotation;
  diseaseIds?: string[];
  pestIds?: string[];
  image?: string;
  imageCredit?: string;
  status?: ValidationStatus;
  sourceIds?: string[];
  lastReviewed?: string;
  isDemo?: boolean;
}

export interface PlantDisease {
  id: string;
  names: LocalizedText;
  causalAgent?: string;
  type: HealthIssueType;
  symptoms?: LocalizedText;
  favorableConditions?: LocalizedText;
  prevention?: LocalizedText;
  treatmentIds?: string[];
  status?: ValidationStatus;
  sourceIds?: string[];
}

export interface Pest {
  id: string;
  names: LocalizedText;
  scientificName?: string;
  type: HealthIssueType;
  description?: LocalizedText;
  damage?: LocalizedText;
  prevention?: LocalizedText;
  treatmentIds?: string[];
  status?: ValidationStatus;
  sourceIds?: string[];
}

export interface Treatment {
  id: string;
  names: LocalizedText;
  type: TreatmentType;
  description?: LocalizedText;
  caution?: LocalizedText;
  status?: ValidationStatus;
  sourceIds?: string[];
}

export interface ReferenceData {
  categories: PlantCategory[];
  climateZones: ClimateZone[];
  soilTypes: SoilType[];
  methods: CultivationMethod[];
  sources: DataSource[];
}

export interface HealthData { diseases: PlantDisease[]; pests: Pest[]; treatments: Treatment[] }
export interface GeographyData { countries: Country[] }

export interface SeedManifest {
  version: number;
  generatedOn: string;
  demo: boolean;
  plantFiles: string[];
  disclaimer?: LocalizedText;
}
