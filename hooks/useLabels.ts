import { useCallback } from 'react';
import { MONTH_NAMES } from '../constants/months';
import { useLanguage } from '../contexts/LanguageContext';
import { Note, NoteCode } from '../lib/calendar';
import { monthRanges } from '../lib/months';
import { pick } from '../lib/text';
import { LocalizedText } from '../lib/types';

const NOTE_KEYS: Record<NoteCode, string> = {
  no_climate_data: 'note_no_climate', no_cycle_data: 'note_no_cycle', greenhouse_offset: 'note_greenhouse',
  controlled_environment: 'note_controlled', nursery_sheltered: 'note_nursery', frost_risk: 'note_frost',
  irrigation_required: 'note_irrigation', rainy_season_aligned: 'note_rainy', no_winter_chill: 'note_no_chill',
  winter_too_cold: 'note_winter_cold', summer_too_cool: 'note_summer_cool', too_hot: 'note_too_hot',
  southern_shift: 'note_south', tropical_perennial: 'note_tropical_perennial', no_window: 'note_no_window',
  method_not_suited: 'note_method', zone_not_listed: 'note_zone', difficult_for_beginner: 'note_difficult',
  explicit_data: 'note_explicit', indicative_only: 'note_indicative',
};

/** Translation keys whose names differ from the model values. */
const KEY_ALIASES: Record<string, string> = {
  sun_full_sun: 'sun_full', sun_partial_shade: 'sun_partial', farm_small_farm: 'farm_small', farm_medium_farm: 'farm_medium',
  farm_large_farm: 'farm_large', diff_easy: 'diff_easy',
};

/** Localized labels for model values, months, notes and dataset texts. */
export function useLabels() {
  const { t, language } = useLanguage();
  const label = useCallback((prefix: string, value: string) => {
    const key = `${prefix}_${value}`;
    return t(KEY_ALIASES[key] ?? key);
  }, [t]);
  const month = useCallback((m: number) => MONTH_NAMES[language][m - 1], [language]);
  const monthList = useCallback((months: Iterable<number>) => {
    const set = new Set(months);
    if (!set.size) return '—';
    return monthRanges(set).map(([s, e]) => (s === e ? month(s) : `${month(s)} – ${month(e)}`)).join(', ');
  }, [month]);
  const note = useCallback((n: Note) => {
    const v = n.value == null ? '' : Number.isInteger(n.value) ? String(n.value) : n.value.toFixed(1);
    return t(NOTE_KEYS[n.code], { a: n.code === 'too_hot' ? monthList(n.months ?? []) : n.months ? monthList(n.months) : v, b: v });
  }, [t, monthList]);
  const tr = useCallback((text: LocalizedText | undefined) => pick(text, language), [language]);
  /** "13 octobre 2026" (no dependency on Intl). */
  const date = useCallback((iso: string) => {
    const [y, m, d] = iso.split('-').map(Number);
    const name = MONTH_NAMES[language][m - 1] ?? '';
    return language === 'en' ? `${name} ${d}, ${y}` : `${d} ${language === 'fr' ? name.toLowerCase() : name} ${y}`;
  }, [language]);
  return { label, month, monthList, note, tr, date, language };
}
