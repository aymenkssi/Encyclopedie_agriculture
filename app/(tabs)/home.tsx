import React, { useMemo, useState } from 'react';
import { ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { router } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AdBanner } from '../../components/AdBanner';
import { CATEGORY_ICONS, PlantRow, SuitabilityPill } from '../../components/PlantRow';
import { RegionPicker } from '../../components/RegionPicker';
import { TaskEditor } from '../../components/TaskEditor';
import { TaskRow } from '../../components/TaskRow';
import { Body, Button, Card, Chip, ChipRow, DemoBanner, EmptyState, InfoNote, SectionTitle, Title, useRow, HScroll } from '../../components/ui';
import { BORDER_RADIUS, FONT, SPACING } from '../../constants/theme';
import { useLanguage } from '../../contexts/LanguageContext';
import { useSettings } from '../../contexts/SettingsContext';
import { useTheme } from '../../contexts/ThemeContext';
import { useUserData } from '../../contexts/UserDataContext';
import { useLabels } from '../../hooks/useLabels';
import { activitiesIn, computeCalendar } from '../../lib/calendar';
import { displayName, hemisphereOf, plantMethods } from '../../lib/catalog';
import { addDays, today } from '../../lib/dates';
import { catalog } from '../../lib/knowledge';
import { Season, seasonMonths, seasonsFor, wrapMonth } from '../../lib/months';
import { recommend } from '../../lib/recommend';
import { seasonalTips } from '../../lib/tips';
import { Task, alive } from '../../lib/userData';

type Period = { kind: 'month'; month: number } | { kind: 'season'; season: Season };

export default function Home() {
  const { colors } = useTheme();
  const { t, isRTL } = useLanguage();
  const { label, month, monthList, tr, language } = useLabels();
  const settings = useSettings();
  const { data, history, favoriteIds } = useUserData();
  const row = useRow();
  const [picker, setPicker] = useState(false);
  const [period, setPeriod] = useState<Period>({ kind: 'month', month: new Date().getMonth() + 1 });
  const [editing, setEditing] = useState<Task | null>(null);

  const region = settings.regionId ? catalog.regionsById[settings.regionId] : null;
  const hemisphere = region ? hemisphereOf(catalog, region) : 'north';
  const months = useMemo(
    () => (period.kind === 'month' ? new Set([period.month]) : seasonMonths(period.season, region, hemisphere)),
    [period, region, hemisphere],
  );
  const periodLabel = period.kind === 'month' ? month(period.month) : `${label('season', period.season)} (${monthList(months)})`;
  const refMonth = period.kind === 'month' ? period.month : [...months][0] ?? 1;

  const recos = useMemo(() => (region ? recommend(catalog, {
    region, months, methodId: settings.preferredMethod, experience: settings.experience, farmType: settings.farmType,
  }).slice(0, 8) : []), [region, months, settings.preferredMethod, settings.experience, settings.farmType]);

  const tasks = useMemo(() => {
    const limit = addDays(today(), 7);
    return alive(data.tasks).filter((x) => !x.done && x.dueDate <= limit).sort((a, b) => a.dueDate.localeCompare(b.dueDate)).slice(0, 6);
  }, [data.tasks]);

  const crops = alive(data.crops).filter((c) => c.stage !== 'finished');
  const followed = useMemo(() => {
    if (!region) return [];
    const ids = [...new Set([...crops.map((c) => c.plantId), ...favoriteIds])];
    return ids.flatMap((id) => {
      const plant = catalog.plantsById[id];
      if (!plant) return [];
      const method = crops.find((c) => c.plantId === id)?.methodId ?? plantMethods(plant)[0];
      const acts = activitiesIn(computeCalendar(catalog, plant, region, method), refMonth);
      return acts.length ? [{ plant, acts }] : [];
    });
  }, [region, crops, favoriteIds, refMonth]);

  const tips = region ? seasonalTips(region, months) : [];
  const favorites = favoriteIds.map((id) => catalog.plantsById[id]).filter(Boolean);
  const recent = history.slice(0, 10).map((id) => catalog.plantsById[id]).filter(Boolean);
  const openPlant = (id: string) => router.push({ pathname: '/plant/[id]', params: { id } });
  const shiftMonth = (by: number) => setPeriod({ kind: 'month', month: wrapMonth(refMonth + by) });

  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: colors.background }}>
      <ScrollView contentContainerStyle={{ padding: SPACING.md, paddingBottom: SPACING.xl }} testID="home">
        <View style={[row, { alignItems: 'center', gap: SPACING.sm, marginBottom: SPACING.sm }]}>
          <Ionicons name="leaf" size={32} color={colors.primary} />
          <View style={{ flex: 1 }}>
            <Title size={FONT.xxl}>{t('app_name')}</Title>
            <Body small secondary>{t('home_subtitle')}</Body>
          </View>
        </View>

        <TouchableOpacity onPress={() => router.push('/(tabs)/encyclopedia')} testID="home-search"
          style={[row, { alignItems: 'center', gap: SPACING.sm, backgroundColor: colors.surfaceAlt, borderRadius: BORDER_RADIUS.round, paddingHorizontal: SPACING.md, paddingVertical: 12, marginBottom: SPACING.sm }]}>
          <Ionicons name="search" size={20} color={colors.textSecondary} />
          <Text style={{ color: colors.textSecondary, fontSize: FONT.md, flex: 1, textAlign: isRTL ? 'right' : 'left' }}>{t('search_hint')}</Text>
        </TouchableOpacity>

        {catalog.manifest.demo && <DemoBanner text={tr(catalog.manifest.disclaimer)} onPress={() => router.push('/sources')} />}

        <Card onPress={() => setPicker(true)} testID="home-region">
          <View style={[row, { alignItems: 'center', gap: SPACING.md }]}>
            <Ionicons name="location" size={28} color={colors.earth} />
            <View style={{ flex: 1 }}>
              {region ? (
                <>
                  <Body bold>{`${tr(region.names)} · ${tr(catalog.countriesById[region.countryId]?.names)}`}</Body>
                  <Body small secondary>{tr(catalog.climateZonesById[region.climateZoneId]?.names)}</Body>
                </>
              ) : (
                <>
                  <Body bold>{t('no_region_title')}</Body>
                  <Body small secondary>{t('no_region_text')}</Body>
                </>
              )}
            </View>
            <Text style={{ color: colors.primary, fontWeight: '700' }}>{t(region ? 'change' : 'choose_region')}</Text>
          </View>
        </Card>

        <View style={[row, { alignItems: 'center', justifyContent: 'space-between', marginTop: SPACING.sm }]}>
          <TouchableOpacity onPress={() => shiftMonth(-1)} accessibilityLabel={t('previous')} hitSlop={8} testID="month-prev">
            <Ionicons name={isRTL ? 'chevron-forward' : 'chevron-back'} size={26} color={colors.primary} />
          </TouchableOpacity>
          <Title>{periodLabel}</Title>
          <TouchableOpacity onPress={() => shiftMonth(1)} accessibilityLabel={t('next')} hitSlop={8} testID="month-next">
            <Ionicons name={isRTL ? 'chevron-back' : 'chevron-forward'} size={26} color={colors.primary} />
          </TouchableOpacity>
        </View>
        <HScroll>
          <Chip label={t('this_month')} selected={period.kind === 'month' && period.month === new Date().getMonth() + 1}
            onPress={() => setPeriod({ kind: 'month', month: new Date().getMonth() + 1 })} />
          {seasonsFor(region).map((s) => (
            <Chip key={s} label={label('season', s)} selected={period.kind === 'season' && period.season === s}
              onPress={() => setPeriod({ kind: 'season', season: s })} testID={`season-${s}`} />
          ))}
        </HScroll>

        <SectionTitle title={t('recommended_for', { a: periodLabel })} action={region ? (
          <Button variant="text" label={t('see_all')} onPress={() => router.push({ pathname: '/(tabs)/encyclopedia', params: { adapted: '1' } })} />
        ) : undefined} />
        {!region ? (
          <EmptyState icon="location-outline" title={t('no_region_title')} message={t('no_region_text')}
            action={<Button label={t('choose_region')} onPress={() => setPicker(true)} />} />
        ) : recos.length === 0 ? (
          <EmptyState icon="calendar-outline" title={t('no_reco_title')} message={t('no_reco_text')} />
        ) : (
          <>
            <Body small secondary>{t('recommended_hint')}</Body>
            {recos.map((r) => (
              <PlantRow key={r.plant.id} plant={r.plant} onPress={() => openPlant(r.plant.id)} testID={`reco-${r.plant.id}`}
                subtitle={r.activities.map((a) => label('act', a)).join(' · ') || null}
                badges={<SuitabilityPill level={r.level} label={label('suit', r.level)} />} />
            ))}
          </>
        )}

        <SectionTitle title={t('tasks_now')} action={<Button variant="text" label={t('open_calendar')} onPress={() => router.push('/(tabs)/calendar')} />} />
        {tasks.length ? tasks.map((task) => <TaskRow key={task.id} task={task} onPress={() => setEditing(task)} />)
          : <Body small secondary>{t('no_tasks_now')}</Body>}

        {followed.length > 0 && (
          <>
            <SectionTitle title={t('month_calendar_for_you', { a: month(refMonth) })} />
            {followed.map(({ plant, acts }) => (
              <PlantRow key={plant.id} plant={plant} subtitle={acts.map((a) => label('act', a)).join(' · ')} onPress={() => openPlant(plant.id)} />
            ))}
          </>
        )}

        {tips.length > 0 && (
          <>
            <SectionTitle title={t('seasonal_tips')} />
            <Card>
              {tips.map((tip) => <InfoNote key={tip.kind} text={t(`tip_${tip.kind}`, { a: monthList(tip.months) })} />)}
            </Card>
          </>
        )}

        <SectionTitle title={t('categories')} />
        <ChipRow>
          {catalog.reference.categories.map((c) => (
            <Chip key={c.id} label={tr(c.names)} icon={CATEGORY_ICONS[c.id] ?? 'leaf-outline'}
              onPress={() => router.push({ pathname: '/(tabs)/encyclopedia', params: { category: c.id } })} />
          ))}
        </ChipRow>

        {favorites.length > 0 && (
          <>
            <SectionTitle title={t('favorites')} />
            <ChipRow>{favorites.map((p) => <Chip key={p.id} label={displayName(p, language)} icon="heart" onPress={() => openPlant(p.id)} />)}</ChipRow>
          </>
        )}
        {recent.length > 0 && (
          <>
            <SectionTitle title={t('recently_viewed')} action={<Button variant="text" label={t('see_all')} onPress={() => router.push('/history')} />} />
            <ChipRow>{recent.map((p) => <Chip key={p.id} label={displayName(p, language)} icon="time-outline" onPress={() => openPlant(p.id)} />)}</ChipRow>
          </>
        )}

        <Card onPress={() => router.push('/donate')} style={{ marginTop: SPACING.lg }} testID="home-support">
          <View style={[row, { alignItems: 'center', gap: SPACING.md }]}>
            <Ionicons name="heart" size={28} color={colors.clay} />
            <View style={{ flex: 1 }}>
              <Body bold>{t('support_title')}</Body>
              <Body small secondary>{t('support_home_hint')}</Body>
            </View>
          </View>
        </Card>
        <InfoNote text={t('no_guarantee')} />
      </ScrollView>
      <AdBanner />
      <RegionPicker visible={picker} currentRegionId={settings.regionId} onClose={() => setPicker(false)}
        onPick={(r) => settings.update({ regionId: r.id })} />
      <TaskEditor visible={!!editing} task={editing} onClose={() => setEditing(null)} />
    </SafeAreaView>
  );
}
