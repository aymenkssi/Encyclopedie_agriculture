import React, { useMemo, useState } from 'react';
import { TouchableOpacity, View } from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Fab } from '../../components/Fab';
import { MonthAxis, MonthStrip } from '../../components/MonthStrip';
import { PlantRow } from '../../components/PlantRow';
import { Screen } from '../../components/Screen';
import { TaskEditor } from '../../components/TaskEditor';
import { TaskRow } from '../../components/TaskRow';
import { Body, Card, Chip, ChipRow, EmptyState, SectionTitle, Title, useRow } from '../../components/ui';
import { SPACING } from '../../constants/theme';
import { useLanguage } from '../../contexts/LanguageContext';
import { useSettings } from '../../contexts/SettingsContext';
import { useTheme } from '../../contexts/ThemeContext';
import { useUserData } from '../../contexts/UserDataContext';
import { useLabels } from '../../hooks/useLabels';
import { activitiesIn, computeCalendar, monthsFor } from '../../lib/calendar';
import { displayName, plantMethods } from '../../lib/catalog';
import { IsoDate, today } from '../../lib/dates';
import { catalog } from '../../lib/knowledge';
import { START_ACTIVITIES } from '../../lib/types';
import { alive, Task } from '../../lib/userData';

const pad = (n: number) => String(n).padStart(2, '0');

export default function CalendarScreen() {
  const { colors } = useTheme();
  const { t, isRTL, language } = useLanguage();
  const { label, month, tr } = useLabels();
  const settings = useSettings();
  const { data, favoriteIds } = useUserData();
  const row = useRow();
  const now = new Date();
  const [ym, setYm] = useState({ y: now.getFullYear(), m: now.getMonth() + 1 });
  const [mode, setMode] = useState<'tasks' | 'crops'>('tasks');
  const [editor, setEditor] = useState<{ task: Task | null } | null>(null);
  const prefix = `${ym.y}-${pad(ym.m)}`;
  const todayIso = today();
  const isCurrent = todayIso.startsWith(prefix);

  const shift = (by: number) => setYm(({ y, m }) => {
    const idx = y * 12 + (m - 1) + by;
    return { y: Math.floor(idx / 12), m: (idx % 12) + 1 };
  });

  const tasks = alive(data.tasks);
  const monthTasks = useMemo(() => tasks.filter((x) => x.dueDate.startsWith(prefix))
    .sort((a, b) => Number(a.done) - Number(b.done) || a.dueDate.localeCompare(b.dueDate)), [tasks, prefix]);
  const overdue = isCurrent ? tasks.filter((x) => !x.done && x.dueDate < `${prefix}-01`).sort((a, b) => a.dueDate.localeCompare(b.dueDate)) : [];

  const region = settings.regionId ? catalog.regionsById[settings.regionId] : null;
  const crops = alive(data.crops).filter((c) => c.stage !== 'finished');
  const followed = useMemo(() => {
    if (!region) return [];
    return [...new Set([...crops.map((c) => c.plantId), ...favoriteIds])].flatMap((id) => {
      const plant = catalog.plantsById[id];
      if (!plant) return [];
      const cal = computeCalendar(catalog, plant, region, crops.find((c) => c.plantId === id)?.methodId ?? plantMethods(plant)[0]);
      const starts = new Set(START_ACTIVITIES.flatMap((a) => [...monthsFor(cal, a)]));
      return [{ plant, cal, starts, harvest: monthsFor(cal, 'harvest'), acts: activitiesIn(cal, ym.m) }];
    });
  }, [region, crops, favoriteIds, ym.m]);

  const defaultDue: IsoDate = isCurrent ? todayIso : `${prefix}-01`;

  return (
    <Screen title={t('tab_calendar')} fab={mode === 'tasks' ? <Fab label={t('new_task')} onPress={() => setEditor({ task: null })} testID="add-task" /> : undefined}>
      <View style={[row, { alignItems: 'center', justifyContent: 'space-between' }]}>
        <TouchableOpacity onPress={() => shift(-1)} accessibilityLabel={t('previous')} hitSlop={8} testID="cal-prev">
          <Ionicons name={isRTL ? 'chevron-forward' : 'chevron-back'} size={26} color={colors.primary} />
        </TouchableOpacity>
        <Title>{`${month(ym.m)} ${ym.y}`}</Title>
        <TouchableOpacity onPress={() => shift(1)} accessibilityLabel={t('next')} hitSlop={8} testID="cal-next">
          <Ionicons name={isRTL ? 'chevron-back' : 'chevron-forward'} size={26} color={colors.primary} />
        </TouchableOpacity>
      </View>
      <ChipRow>
        <Chip label={t('my_tasks')} icon="checkbox-outline" selected={mode === 'tasks'} onPress={() => setMode('tasks')} testID="cal-tasks" />
        <Chip label={t('crop_calendar')} icon="leaf-outline" selected={mode === 'crops'} onPress={() => setMode('crops')} testID="cal-crops" />
        {!isCurrent && <Chip label={t('this_month')} onPress={() => setYm({ y: now.getFullYear(), m: now.getMonth() + 1 })} />}
      </ChipRow>

      {mode === 'tasks' ? (
        <>
          {overdue.length > 0 && (
            <>
              <SectionTitle title={t('overdue_tasks')} />
              {overdue.map((x) => <TaskRow key={x.id} task={x} onPress={() => setEditor({ task: x })} />)}
            </>
          )}
          <SectionTitle title={t('tasks_of_month')} />
          {monthTasks.length ? monthTasks.map((x) => <TaskRow key={x.id} task={x} onPress={() => setEditor({ task: x })} />)
            : <EmptyState icon="calendar-outline" title={t('no_tasks_title')} message={t('no_tasks_text')} />}
        </>
      ) : !region ? (
        <EmptyState icon="location-outline" title={t('no_region_title')} message={t('calendar_needs_region')} />
      ) : !followed.length ? (
        <EmptyState icon="leaf-outline" title={t('no_followed_plants_title')} message={t('no_followed_plants_text')} />
      ) : (
        <>
          <Body small secondary>{t('crop_calendar_hint', { a: tr(region.names) })}</Body>
          <Card>
            <MonthAxis />
            {followed.map(({ plant, starts, harvest }) => (
              <TouchableOpacity key={plant.id} onPress={() => router.push({ pathname: '/plant/[id]', params: { id: plant.id, tab: 'calendar' } })}
                style={{ marginTop: SPACING.sm, gap: 3 }}>
                <Body bold small>{displayName(plant, language)}</Body>
                <MonthStrip months={starts} current={ym.m} label={label('act', 'direct_sowing')} />
                {harvest.size > 0 && <MonthStrip months={harvest} current={ym.m} color={colors.clay} label={label('act', 'harvest')} />}
              </TouchableOpacity>
            ))}
            <View style={[row, { gap: SPACING.md, marginTop: SPACING.sm, flexWrap: 'wrap' }]}>
              <View style={[row, { gap: 4, alignItems: 'center' }]}><View style={{ width: 14, height: 10, borderRadius: 2, backgroundColor: colors.primary }} /><Body small>{t('legend_start')}</Body></View>
              <View style={[row, { gap: 4, alignItems: 'center' }]}><View style={{ width: 14, height: 10, borderRadius: 2, backgroundColor: colors.clay }} /><Body small>{label('act', 'harvest')}</Body></View>
            </View>
          </Card>
          <SectionTitle title={t('month_calendar_for_you', { a: month(ym.m) })} />
          {followed.filter((f) => f.acts.length).map(({ plant, acts }) => (
            <PlantRow key={plant.id} plant={plant} subtitle={acts.map((a) => label('act', a)).join(' · ')}
              onPress={() => router.push({ pathname: '/plant/[id]', params: { id: plant.id, tab: 'calendar' } })} />
          ))}
          {!followed.some((f) => f.acts.length) && <Body small secondary>{t('nothing_this_month')}</Body>}
        </>
      )}
      <TaskEditor visible={!!editor} task={editor?.task ?? null} defaults={{ dueDate: defaultDue }} onClose={() => setEditor(null)} />
    </Screen>
  );
}
