import React, { useEffect, useMemo, useState } from 'react';
import { ScrollView, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { AddCropSheet } from '../../components/AddCropSheet';
import { MonthAxis, MonthStrip } from '../../components/MonthStrip';
import { NoteEditor } from '../../components/NoteEditor';
import { PlantAvatar, StatusBadgeRow } from '../../components/PlantRow';
import { RegionPicker } from '../../components/RegionPicker';
import { Screen } from '../../components/Screen';
import { SourcesList } from '../../components/SourcesList';
import { HeaderIcon } from '../../components/ScreenHeader';
import {
  Body, Button, Card, Chip, ChipRow, DemoBanner, EmptyState, InfoNote, InfoRow, Pill, SectionTitle, StatusBadge, Title, useRow, HScroll } from '../../components/ui';
import { FONT, SPACING } from '../../constants/theme';
import { useLanguage } from '../../contexts/LanguageContext';
import { useSettings } from '../../contexts/SettingsContext';
import { useTheme } from '../../contexts/ThemeContext';
import { useUserData } from '../../contexts/UserDataContext';
import { useLabels } from '../../hooks/useLabels';
import { calendarStatus, CalendarOrigin, computeCalendar } from '../../lib/calendar';
import { displayName, plantMethods } from '../../lib/catalog';
import { catalog } from '../../lib/knowledge';
import { evaluate } from '../../lib/recommend';
import { pickNames } from '../../lib/text';
import { Plant, Range, START_ACTIVITIES } from '../../lib/types';
import { alive, Note } from '../../lib/userData';

const TABS = ['overview', 'conditions', 'calendar', 'guide', 'health', 'companions', 'sources'] as const;
type Tab = (typeof TABS)[number];
const TAB_KEYS: Record<Tab, string> = {
  overview: 'tab_overview', conditions: 'tab_conditions', calendar: 'tab_plant_calendar', guide: 'tab_guide',
  health: 'tab_health', companions: 'tab_companions', sources: 'tab_sources',
};
const ORIGIN_KEYS: Record<CalendarOrigin, string> = {
  explicit_region: 'origin_region', explicit_zone: 'origin_zone', computed: 'origin_computed', phenology: 'origin_phenology', controlled: 'origin_controlled',
};

const range = (r: Range | undefined | null, unit: string) =>
  (r ? (r.min === r.max ? `${r.min} ${unit}` : `${r.min}–${r.max} ${unit}`) : null);
const temp = (v: number | undefined) => (v == null ? null : `${v} °C`);

export default function PlantScreen() {
  const { id, tab: initialTab } = useLocalSearchParams<{ id: string; tab?: string }>();
  const plant = catalog.plantsById[id ?? ''];
  const { t, language } = useLanguage();
  const { isFavorite, toggleFavorite, markViewed } = useUserData();
  const { colors } = useTheme();
  const [tab, setTab] = useState<Tab>((TABS as readonly string[]).includes(initialTab ?? '') ? (initialTab as Tab) : 'overview');
  const [noteEditor, setNoteEditor] = useState<{ note: Note | null } | null>(null);
  const [addCrop, setAddCrop] = useState(false);

  useEffect(() => { if (plant) markViewed(plant.id); }, [plant, markViewed]);

  if (!plant) {
    return (
      <Screen title={t('plant_not_found')} back>
        <EmptyState icon="help-circle-outline" title={t('plant_not_found')} message={t('plant_not_found_text')} />
      </Screen>
    );
  }
  const fav = isFavorite(plant.id);
  return (
    <Screen title={displayName(plant, language)} back fab={undefined} scroll={false} right={(
      <>
        <HeaderIcon icon="create-outline" label={t('add_note')} onPress={() => setNoteEditor({ note: null })} testID="plant-add-note" />
        <HeaderIcon icon={fav ? 'heart' : 'heart-outline'} color={fav ? colors.clay : undefined} label={t(fav ? 'remove_favorite' : 'add_favorite')}
          onPress={() => toggleFavorite(plant.id)} testID="plant-favorite" />
      </>
    )}>
      <View>
        <HScroll padding={SPACING.sm}>
          {TABS.map((x) => <Chip key={x} label={t(TAB_KEYS[x])} selected={tab === x} onPress={() => setTab(x)} testID={`plant-tab-${x}`} />)}
        </HScroll>
      </View>
      <ScrollView contentContainerStyle={{ padding: SPACING.md, paddingBottom: SPACING.xl }} testID={`plant-${tab}`}>
        {tab === 'overview' && <Overview plant={plant} onAddCrop={() => setAddCrop(true)} onEditNote={(note) => setNoteEditor({ note })} />}
        {tab === 'conditions' && <Conditions plant={plant} />}
        {tab === 'calendar' && <PlantCalendarTab plant={plant} />}
        {tab === 'guide' && <Guide plant={plant} />}
        {tab === 'health' && <Health plant={plant} />}
        {tab === 'companions' && <Companions plant={plant} />}
        {tab === 'sources' && <SourcesList ids={plant.sourceIds ?? []} lastReviewed={plant.lastReviewed} />}
      </ScrollView>
      <NoteEditor visible={!!noteEditor} note={noteEditor?.note ?? null} plantId={plant.id} onClose={() => setNoteEditor(null)} />
      <AddCropSheet visible={addCrop} plant={plant} onClose={() => setAddCrop(false)} />
    </Screen>
  );
}

function Overview({ plant, onAddCrop, onEditNote }: { plant: Plant; onAddCrop: () => void; onEditNote: (n: Note) => void }) {
  const { t, language } = useLanguage();
  const { label, tr } = useLabels();
  const { data } = useUserData();
  const settings = useSettings();
  const row = useRow();
  const region = settings.regionId ? catalog.regionsById[settings.regionId] : null;
  const verdict = region ? evaluate(catalog, plant, { region, months: new Set([new Date().getMonth() + 1]), methodId: settings.preferredMethod, experience: settings.experience }) : null;
  const notes = alive(data.notes).filter((n) => n.plantId === plant.id);
  const otherNames = (['fr', 'ar', 'en'] as const).map((l) => [l, pickNames({ [l]: plant.names[l] }, l)] as const).filter(([, n]) => n.length);
  return (
    <>
      <View style={[row, { gap: SPACING.md, alignItems: 'center' }]}>
        <PlantAvatar plant={plant} size={72} />
        <View style={{ flex: 1, gap: 4 }}>
          <Title size={FONT.xl}>{displayName(plant, language)}</Title>
          <Body italic secondary>{`${plant.scientificName}${plant.scientificAuthority ? ` ${plant.scientificAuthority}` : ''}`}</Body>
          <Body small secondary>{`${plant.family} · ${tr(catalog.categoriesById[plant.categoryId]?.names)}`}</Body>
          <View style={[row, { gap: SPACING.xs, flexWrap: 'wrap' }]}><StatusBadgeRow plant={plant} difficulty={label('diff', plant.difficulty)} /></View>
        </View>
      </View>
      {plant.isDemo && <DemoBanner text={t('demo_sheet')} />}
      {!!tr(plant.description) && <View style={{ marginTop: SPACING.sm }}><Body>{tr(plant.description)}</Body></View>}
      {verdict && (
        <Card>
          <Body bold>{t('in_your_region', { a: tr(region!.names) })}</Body>
          <Body>{label('suit', verdict.level)}</Body>
          {verdict.activities.length > 0 && <Body small secondary>{verdict.activities.map((a) => label('act', a)).join(' · ')}</Body>}
        </Card>
      )}
      <View style={{ marginVertical: SPACING.sm }}>
        <Button icon="add-circle-outline" label={t('add_to_crops')} onPress={onAddCrop} testID="plant-add-crop" />
      </View>
      <SectionTitle title={t('common_names')} />
      <Card>
        {otherNames.map(([l, names]) => <InfoRow key={l} label={l.toUpperCase()} value={names.join(', ')} />)}
        {!!plant.synonyms?.length && <InfoRow label={t('synonyms')} value={plant.synonyms.join(', ')} />}
      </Card>
      <SectionTitle title={t('cultivation')} />
      <Card>
        <InfoRow label={t('difficulty')} value={label('diff', plant.difficulty)} />
        <InfoRow label={t('cycle_length')} value={plant.cycleDays ? t('cycle_days_value', { a: range(plant.cycleDays, '')!.trim() }) : null} />
        <InfoRow label={t('sowing_mode')} value={label('sow', plant.sowingMode)} />
        <InfoRow label={t('yield')} value={tr(plant.yieldInfo) || null} />
        {!!tr(plant.yieldFactors) && <InfoRow label="" value={tr(plant.yieldFactors)} />}
        <InfoRow label={t('storage')} value={tr(plant.storage) || null} />
      </Card>
      <Body small secondary>{label('life', plant.lifecycle)}</Body>
      {!!plant.varieties?.length && (
        <>
          <SectionTitle title={t('varieties')} />
          {plant.varieties.map((v) => (
            <Card key={v.id}>
              <View style={[row, { justifyContent: 'space-between', alignItems: 'center' }]}>
                <Body bold>{v.name}</Body>
                <StatusBadge status={v.status} />
              </View>
              {!!tr(v.description) && <Body small>{tr(v.description)}</Body>}
              {v.cycleDays && <Body small secondary>{t('cycle_days_value', { a: range(v.cycleDays, '')!.trim() })}</Body>}
            </Card>
          ))}
        </>
      )}
      <SectionTitle title={t('my_notes')} />
      {notes.length ? notes.map((n) => (
        <Card key={n.id} onPress={() => onEditNote(n)}>
          {!!n.title && <Body bold>{n.title}</Body>}
          <Body small>{n.body}</Body>
        </Card>
      )) : <Body small secondary>{t('no_notes_plant')}</Body>}
    </>
  );
}

function Conditions({ plant }: { plant: Plant }) {
  const { t } = useLanguage();
  const { label, tr } = useLabels();
  const tp = plant.temperature;
  const rot = plant.rotation;
  return (
    <>
      <SectionTitle title={t('climate_needs')} />
      <Card>
        <InfoRow label={t('temp_optimal')} value={tp?.optimalMin != null && tp.optimalMax != null ? `${tp.optimalMin}–${tp.optimalMax} °C` : null} />
        <InfoRow label={t('temp_min')} value={temp(tp?.min)} />
        <InfoRow label={t('temp_max')} value={temp(tp?.max)} />
        <InfoRow label={t('temp_germination')} value={temp(tp?.germinationMin)} />
        <InfoRow label={t('temp_hardiness')} value={temp(tp?.hardinessMin)} />
        <InfoRow label={t('frost_tolerance')} value={label('frost', plant.frostTolerance)} />
        <InfoRow label={t('sun_exposure')} value={label('sun', plant.sun)} />
        <InfoRow label={t('water_needs')} value={label('water', plant.water)} />
      </Card>
      <InfoNote text={t('temp_explanation')} />
      <SectionTitle title={t('soil')} />
      <Card>
        <InfoRow label={t('soil_types')} value={(plant.soilIds ?? []).map((s) => tr(catalog.soilTypesById[s]?.names)).join(', ') || null} />
        <InfoRow label={t('soil_ph')} value={range(plant.ph, '')?.trim() ?? null} />
      </Card>
      <SectionTitle title={t('cultivation')} />
      <Card>
        <InfoRow label={t('spacing_in_row')} value={range(plant.spacing?.inRowCm, 'cm')} />
        <InfoRow label={t('spacing_between_rows')} value={range(plant.spacing?.betweenRowsCm, 'cm')} />
        <InfoRow label={t('sowing_depth')} value={range(plant.sowingDepthCm, 'cm')} />
        <InfoRow label={t('nursery_duration')} value={plant.nurseryWeeks ? t('weeks_value', { a: range(plant.nurseryWeeks, '')!.trim() }) : null} />
        <InfoRow label={t('cultivation_method')} value={plantMethods(plant).map((m) => tr(catalog.methodsById[m]?.names) || m).join(', ')} />
        <InfoRow label={t('climate')} value={(plant.climateZoneIds ?? []).map((z) => tr(catalog.climateZonesById[z]?.names)).join(', ') || null} />
      </Card>
      {rot && (
        <>
          <SectionTitle title={t('rotation')} />
          <Card>
            <Body small bold>{t('rotation_group', { a: rot.group })}</Body>
            <InfoRow label={t('rotation_years')} value={rot.minYearsBeforeReturn != null ? String(rot.minYearsBeforeReturn) : null} />
            <InfoRow label={t('rotation_good')} value={(rot.goodPredecessorFamilies ?? []).join(', ') || null} />
            <InfoRow label={t('rotation_avoid')} value={(rot.avoidPredecessorFamilies ?? []).join(', ') || null} />
            {!!tr(rot.notes) && <Body small>{tr(rot.notes)}</Body>}
            <StatusBadge status={rot.status} />
          </Card>
        </>
      )}
    </>
  );
}

function PlantCalendarTab({ plant }: { plant: Plant }) {
  const { t } = useLanguage();
  const { label, monthList, note, tr } = useLabels();
  const { colors } = useTheme();
  const settings = useSettings();
  const row = useRow();
  const methods = plantMethods(plant);
  const [method, setMethod] = useState(methods.includes(settings.preferredMethod) ? settings.preferredMethod : methods[0]);
  const [variety, setVariety] = useState<string | null>(null);
  const [picker, setPicker] = useState(false);
  const region = settings.regionId ? catalog.regionsById[settings.regionId] : null;
  const cal = useMemo(() => (region ? computeCalendar(catalog, plant, region, method, variety) : null), [plant, region, method, variety]);
  const current = new Date().getMonth() + 1;

  if (!region) {
    return (
      <>
        <EmptyState icon="location-outline" title={t('no_region_title')} message={t('calendar_needs_region')}
          action={<Button label={t('choose_region')} onPress={() => setPicker(true)} />} />
        <RegionPicker visible={picker} currentRegionId={null} onClose={() => setPicker(false)} onPick={(r) => settings.update({ regionId: r.id })} />
      </>
    );
  }
  return (
    <>
      <Card onPress={() => setPicker(true)}>
        <Body bold>{t('calendar_for', { a: `${tr(region.names)} · ${tr(catalog.countriesById[region.countryId]?.names)}` })}</Body>
        <Body small secondary>{t('climate_data_status', { a: t(`status_${region.climateStatus ?? 'to_verify'}`) })}</Body>
      </Card>
      {methods.length > 1 && (
        <ChipRow>{methods.map((m) => <Chip key={m} label={tr(catalog.methodsById[m]?.names) || m} selected={m === method} onPress={() => setMethod(m)} />)}</ChipRow>
      )}
      {!!plant.varieties?.length && (
        <ChipRow>
          <Chip label={t('all')} selected={!variety} onPress={() => setVariety(null)} />
          {plant.varieties.map((v) => <Chip key={v.id} label={v.name} selected={variety === v.id} onPress={() => setVariety(v.id)} />)}
        </ChipRow>
      )}
      {cal && !cal.available ? (
        <EmptyState icon="calendar-outline" title={t('no_calendar_title')} message={t('no_calendar_text')} />
      ) : cal && (
        <Card>
          <MonthAxis />
          {cal.windows.map((w, i) => {
            const color = START_ACTIVITIES.includes(w.activity) ? colors.primary : w.activity === 'harvest' ? colors.clay : colors.earth;
            return (
              <View key={`${w.activity}-${i}`} style={{ marginTop: SPACING.sm, gap: 4 }}>
                <View style={[row, { justifyContent: 'space-between', alignItems: 'center' }]}>
                  <Body bold small>{label('act', w.activity)}</Body>
                  <StatusBadge status={w.status} />
                </View>
                <MonthStrip months={w.months} preferred={w.preferred} current={current} color={color} label={monthList(w.months)} />
                <Body small secondary>{`${monthList(w.months)} · ${t(ORIGIN_KEYS[w.origin])}`}</Body>
              </View>
            );
          })}
        </Card>
      )}
      {cal && (
        <>
          <SectionTitle title={t('calendar_notes')} />
          <View style={[row, { gap: SPACING.sm, alignItems: 'center' }]}>
            <Body small secondary>{t('validation_status')}</Body>
            <StatusBadge status={calendarStatus(cal)} />
          </View>
          {cal.notes.map((n, i) => <InfoNote key={i} text={note(n)} />)}
          <InfoNote text={t('no_guarantee')} />
        </>
      )}
      <RegionPicker visible={picker} currentRegionId={region.id} onClose={() => setPicker(false)} onPick={(r) => settings.update({ regionId: r.id })} />
    </>
  );
}

function Guide({ plant }: { plant: Plant }) {
  const { t } = useLanguage();
  const { tr } = useLabels();
  const { colors } = useTheme();
  const row = useRow();
  const guides = plant.guides ?? [];
  if (!guides.length) return <EmptyState icon="book-outline" title={t('no_guide_title')} message={t('no_guide_text')} />;
  return (
    <>
      {guides.map((g) => (
        <View key={g.id}>
          <View style={[row, { justifyContent: 'space-between', alignItems: 'center' }]}>
            <View style={{ flex: 1 }}><Title>{tr(g.title)}</Title></View>
            <StatusBadge status={g.status} />
          </View>
          {[...g.stages].sort((a, b) => a.order - b.order).map((s) => (
            <Card key={s.order}>
              <View style={[row, { gap: SPACING.sm, alignItems: 'flex-start' }]}>
                <Pill label={String(s.order)} bg={colors.primaryContainer} fg={colors.onPrimaryContainer} />
                <View style={{ flex: 1, gap: 4 }}>
                  <Body bold>{tr(s.title)}</Body>
                  <Body small>{tr(s.body)}</Body>
                  {!!tr(s.tips) && <InfoNote text={t('tip_prefix', { a: tr(s.tips) })} />}
                  {!!tr(s.mistakes) && <InfoNote tone="error" text={t('mistake_prefix', { a: tr(s.mistakes) })} />}
                  {!!s.methods?.length && <Body small secondary>{s.methods.map((m) => tr(catalog.methodsById[m]?.names) || m).join(', ')}</Body>}
                </View>
              </View>
            </Card>
          ))}
        </View>
      ))}
    </>
  );
}

function Treatments({ ids }: { ids?: string[] }) {
  const { t } = useLanguage();
  const { label, tr } = useLabels();
  const list = (ids ?? []).map((id) => catalog.treatmentsById[id]).filter(Boolean);
  if (!list.length) return null;
  return (
    <View style={{ marginTop: SPACING.xs }}>
      <Body bold small>{t('control_methods')}</Body>
      {list.map((x) => (
        <View key={x.id} style={{ marginTop: 4 }}>
          <Body small>{`• ${tr(x.names)} (${label('treat', x.type)})`}</Body>
          {!!tr(x.description) && <Body small secondary>{tr(x.description)}</Body>}
          {!!tr(x.caution) && <InfoNote tone="error" text={tr(x.caution)} />}
        </View>
      ))}
    </View>
  );
}

function Health({ plant }: { plant: Plant }) {
  const { t } = useLanguage();
  const { label, tr } = useLabels();
  const row = useRow();
  const diseases = (plant.diseaseIds ?? []).map((id) => catalog.diseasesById[id]).filter(Boolean);
  const pests = (plant.pestIds ?? []).map((id) => catalog.pestsById[id]).filter(Boolean);
  if (!diseases.length && !pests.length) return <EmptyState icon="medkit-outline" title={t('no_health_title')} message={t('no_health_text')} />;
  return (
    <>
      <InfoNote text={t('health_disclaimer')} />
      {diseases.length > 0 && <SectionTitle title={t('diseases')} />}
      {diseases.map((d) => (
        <Card key={d.id}>
          <View style={[row, { justifyContent: 'space-between', alignItems: 'center' }]}>
            <View style={{ flex: 1 }}><Body bold>{tr(d.names)}</Body></View>
            <StatusBadge status={d.status} />
          </View>
          <Body small secondary italic>{[label('health', d.type), d.causalAgent].filter(Boolean).join(' · ')}</Body>
          <InfoRow label={t('symptoms')} value={tr(d.symptoms) || null} />
          <InfoRow label={t('favorable_conditions')} value={tr(d.favorableConditions) || null} />
          <InfoRow label={t('prevention')} value={tr(d.prevention) || null} />
          <Treatments ids={d.treatmentIds} />
        </Card>
      ))}
      {pests.length > 0 && <SectionTitle title={t('pests')} />}
      {pests.map((p) => (
        <Card key={p.id}>
          <View style={[row, { justifyContent: 'space-between', alignItems: 'center' }]}>
            <View style={{ flex: 1 }}><Body bold>{tr(p.names)}</Body></View>
            <StatusBadge status={p.status} />
          </View>
          <Body small secondary italic>{[label('health', p.type), p.scientificName].filter(Boolean).join(' · ')}</Body>
          {!!tr(p.description) && <Body small>{tr(p.description)}</Body>}
          <InfoRow label={t('damage')} value={tr(p.damage) || null} />
          <InfoRow label={t('prevention')} value={tr(p.prevention) || null} />
          <Treatments ids={p.treatmentIds} />
        </Card>
      ))}
    </>
  );
}

function Companions({ plant }: { plant: Plant }) {
  const { t, language } = useLanguage();
  const { tr } = useLabels();
  const list = plant.companions ?? [];
  const group = (relation: 'good' | 'bad') => list.filter((c) => c.relation === relation);
  if (!list.length) return <EmptyState icon="people-outline" title={t('no_data')} />;
  return (
    <>
      {(['good', 'bad'] as const).map((rel) => group(rel).length > 0 && (
        <View key={rel}>
          <SectionTitle title={t(rel === 'good' ? 'good_companions' : 'bad_companions')} />
          {group(rel).map((c) => {
            const other = catalog.plantsById[c.otherPlantId];
            return (
              <Card key={c.otherPlantId} onPress={other ? () => router.push({ pathname: '/plant/[id]', params: { id: other.id } }) : undefined}>
                <Body bold>{other ? displayName(other, language) : c.otherPlantId}</Body>
                {!!tr(c.reason) && <Body small secondary>{tr(c.reason)}</Body>}
                <StatusBadge status={c.status} />
              </Card>
            );
          })}
        </View>
      ))}
    </>
  );
}
