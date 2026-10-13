import React, { useMemo, useState } from 'react';
import { FlatList, ScrollView, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { PlantRow, StatusBadgeRow } from '../../components/PlantRow';
import { Screen } from '../../components/Screen';
import { Body, Button, Chip, ChipRow, EmptyState, Sheet, Title, useRow, HScroll } from '../../components/ui';
import { BORDER_RADIUS, FONT, SPACING } from '../../constants/theme';
import { useLanguage } from '../../contexts/LanguageContext';
import { useSettings } from '../../contexts/SettingsContext';
import { useTheme } from '../../contexts/ThemeContext';
import { useLabels } from '../../hooks/useLabels';
import { displayName } from '../../lib/catalog';
import { catalog, plantSearch } from '../../lib/knowledge';
import { activeFilterCount, CycleLength, PlantFilter } from '../../lib/search';
import { ALL_METHODS, Difficulty, WaterNeed } from '../../lib/types';

const DIFFICULTIES: Difficulty[] = ['easy', 'medium', 'hard'];
const WATER: WaterNeed[] = ['low', 'medium', 'high'];
const CYCLES: CycleLength[] = ['short', 'medium', 'long'];

const toggle = <T,>(list: T[] | undefined, v: T): T[] => (list?.includes(v) ? list.filter((x) => x !== v) : [...(list ?? []), v]);

export default function Encyclopedia() {
  const { colors } = useTheme();
  const { t, tn, align, language } = useLanguage();
  const { label, month, tr } = useLabels();
  const settings = useSettings();
  const params = useLocalSearchParams<{ category?: string; adapted?: string }>();
  const row = useRow();
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<PlantFilter>({});
  const [adapted, setAdapted] = useState(false);
  const [sheet, setSheet] = useState(false);

  // Shortcuts from the home screen (category chip, "see all" recommendations): applied when they change.
  const shortcut = `${params.category ?? ''}|${params.adapted ?? ''}`;
  const [lastShortcut, setLastShortcut] = useState('|');
  if (shortcut !== lastShortcut) {
    setLastShortcut(shortcut);
    if (params.category) setFilter((f) => ({ ...f, categoryIds: [params.category!] }));
    if (params.adapted === '1') setAdapted(true);
  }

  const region = settings.regionId ? catalog.regionsById[settings.regionId] : null;
  const currentMonth = new Date().getMonth() + 1;
  const effective: PlantFilter = useMemo(
    () => (adapted && region ? { ...filter, region, months: [currentMonth] } : filter),
    [filter, adapted, region, currentMonth],
  );
  const hits = useMemo(() => plantSearch.search(query, effective, language), [query, effective, language]);
  const count = activeFilterCount(effective);

  const section = (title: string, children: React.ReactNode) => (
    <View style={{ marginTop: SPACING.sm }}>
      <Title size={FONT.md}>{title}</Title>
      <ChipRow>{children}</ChipRow>
    </View>
  );

  return (
    <Screen title={t('tab_encyclopedia')} scroll={false}>
      <View style={{ paddingHorizontal: SPACING.md, paddingTop: SPACING.sm }}>
        <View style={[row, { alignItems: 'center', gap: SPACING.sm }]}>
          <View style={[row, { flex: 1, alignItems: 'center', gap: SPACING.sm, backgroundColor: colors.surfaceAlt, borderRadius: BORDER_RADIUS.round, paddingHorizontal: SPACING.md }]}>
            <Ionicons name="search" size={20} color={colors.textSecondary} />
            <TextInput value={query} onChangeText={setQuery} placeholder={t('search_hint')} placeholderTextColor={colors.textSecondary}
              style={{ flex: 1, color: colors.text, fontSize: FONT.md, paddingVertical: 10, textAlign: align }} testID="search-input"
              autoCorrect={false} returnKeyType="search" />
            {!!query && (
              <TouchableOpacity onPress={() => setQuery('')} accessibilityLabel={t('clear')} hitSlop={8}>
                <Ionicons name="close-circle" size={20} color={colors.textSecondary} />
              </TouchableOpacity>
            )}
          </View>
          <TouchableOpacity onPress={() => setSheet(true)} accessibilityLabel={t('filters')} testID="open-filters"
            style={{ width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center', backgroundColor: count ? colors.earthContainer : colors.surfaceAlt }}>
            <Ionicons name="options" size={22} color={count ? colors.onEarthContainer : colors.text} />
            {count > 0 && (
              <View style={{ position: 'absolute', top: 0, right: 0, minWidth: 18, height: 18, borderRadius: 9, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' }}>
                <Text style={{ color: colors.onPrimary, fontSize: 11, fontWeight: '700' }}>{count}</Text>
              </View>
            )}
          </TouchableOpacity>
        </View>
        <HScroll>
          <Chip label={t('all')} selected={!filter.categoryIds?.length} onPress={() => setFilter({ ...filter, categoryIds: [] })} />
          {catalog.reference.categories.map((c) => (
            <Chip key={c.id} label={tr(c.names)} selected={!!filter.categoryIds?.includes(c.id)} testID={`cat-${c.id}`}
              onPress={() => setFilter({ ...filter, categoryIds: toggle(filter.categoryIds, c.id) })} />
          ))}
        </HScroll>
        <View style={[row, { justifyContent: 'space-between', alignItems: 'center' }]}>
          <Body small secondary>{tn('results_count', hits.length)}</Body>
          {count > 0 && <Button variant="text" label={t('clear_filters')} onPress={() => { setFilter({}); setAdapted(false); }} />}
        </View>
        {adapted && region && <Body small secondary>{t('filter_adapted_detail', { a: tr(region.names), b: month(currentMonth) })}</Body>}
      </View>
      <FlatList data={hits} keyExtractor={(h) => h.plant.id} contentContainerStyle={{ padding: SPACING.md, paddingTop: SPACING.xs }}
        keyboardShouldPersistTaps="handled" testID="results"
        ListEmptyComponent={<EmptyState icon="search" title={t('no_results_title')} message={t('no_results_text')} />}
        renderItem={({ item }) => (
          <PlantRow plant={item.plant} testID={`plant-${item.plant.id}`}
            subtitle={item.matchedName && item.matchedName !== displayName(item.plant, language) ? `≈ ${item.matchedName}` : tr(catalog.categoriesById[item.plant.categoryId]?.names)}
            badges={<StatusBadgeRow plant={item.plant} difficulty={label('diff', item.plant.difficulty)} />}
            onPress={() => router.push({ pathname: '/plant/[id]', params: { id: item.plant.id } })} />
        )} />

      <Sheet visible={sheet} onClose={() => setSheet(false)} title={t('filters')}>
        <ScrollView style={{ maxHeight: 520 }}>
          <View style={{ marginTop: SPACING.xs }}>
            <Chip label={t('filter_adapted')} icon="location" selected={adapted} onPress={() => setAdapted(!adapted)} testID="filter-adapted" />
            {!region && adapted && <Body small color={colors.error}>{t('filter_adapted_no_region')}</Body>}
          </View>
          {section(t('difficulty'), DIFFICULTIES.map((d) => (
            <Chip key={d} label={label('diff', d)} selected={!!filter.difficulties?.includes(d)} onPress={() => setFilter({ ...filter, difficulties: toggle(filter.difficulties, d) })} />
          )))}
          {section(t('water_needs'), WATER.map((w) => (
            <Chip key={w} label={label('water', w)} selected={!!filter.waterNeeds?.includes(w)} onPress={() => setFilter({ ...filter, waterNeeds: toggle(filter.waterNeeds, w) })} />
          )))}
          {section(t('cultivation_method'), ALL_METHODS.map((m) => (
            <Chip key={m} label={tr(catalog.methodsById[m]?.names) || m} selected={!!filter.methodIds?.includes(m)} onPress={() => setFilter({ ...filter, methodIds: toggle(filter.methodIds, m) })} />
          )))}
          {section(t('cycle_length'), CYCLES.map((c) => (
            <Chip key={c} label={label('cycle', c)} selected={filter.cycle === c} onPress={() => setFilter({ ...filter, cycle: filter.cycle === c ? null : c })} />
          )))}
          {section(t('soil_types'), catalog.reference.soilTypes.map((s) => (
            <Chip key={s.id} label={tr(s.names)} selected={!!filter.soilIds?.includes(s.id)} onPress={() => setFilter({ ...filter, soilIds: toggle(filter.soilIds, s.id) })} />
          )))}
          {section(t('climate'), catalog.reference.climateZones.map((z) => (
            <Chip key={z.id} label={tr(z.names)} selected={!!filter.climateZoneIds?.includes(z.id)} onPress={() => setFilter({ ...filter, climateZoneIds: toggle(filter.climateZoneIds, z.id) })} />
          )))}
          {section(t('country'), catalog.geography.countries.map((c) => (
            <Chip key={c.id} label={tr(c.names)} selected={filter.countryId === c.id} onPress={() => setFilter({ ...filter, countryId: filter.countryId === c.id ? null : c.id })} />
          )))}
        </ScrollView>
        <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.sm }]}>
          <Button variant="text" label={t('clear_filters')} onPress={() => { setFilter({}); setAdapted(false); }} />
          <Button label={tn('results_count', hits.length)} onPress={() => setSheet(false)} testID="apply-filters" />
        </View>
      </Sheet>
    </Screen>
  );
}
