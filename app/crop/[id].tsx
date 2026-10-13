import React, { useState } from 'react';
import { View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { ConfirmSheet } from '../../components/ConfirmSheet';
import { NoteEditor } from '../../components/NoteEditor';
import { PlantRow } from '../../components/PlantRow';
import { Screen } from '../../components/Screen';
import { HeaderIcon } from '../../components/ScreenHeader';
import { TaskEditor } from '../../components/TaskEditor';
import { TaskRow } from '../../components/TaskRow';
import { Body, Button, Card, Chip, ChipRow, EmptyState, InfoNote, InfoRow, SectionTitle } from '../../components/ui';
import { BORDER_RADIUS, SPACING } from '../../constants/theme';
import { useLanguage } from '../../contexts/LanguageContext';
import { useTheme } from '../../contexts/ThemeContext';
import { useUserData } from '../../contexts/UserDataContext';
import { useLabels } from '../../hooks/useLabels';
import { addDays, daysBetween, today } from '../../lib/dates';
import { catalog } from '../../lib/knowledge';
import { Plant } from '../../lib/types';
import { alive, Crop, CROP_STAGES, Note, Task } from '../../lib/userData';

/** First estimated harvest date of a crop, or null when the plant data does not allow it. */
function harvestEstimate(plant: Plant, crop: Crop): string | null {
  if (!plant.cycleDays || plant.lifecycle === 'perennial') return null;
  let inPlace = crop.startDate;
  if (crop.startActivity === 'nursery_sowing' && (plant.sowingMode === 'transplant' || plant.sowingMode === 'both')) {
    if (plant.nurseryWeeks?.min == null) return null;
    inPlace = addDays(inPlace, plant.nurseryWeeks.min * 7);
  }
  return addDays(inPlace, plant.cycleDays.min);
}

export default function CropScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { t, tn } = useLanguage();
  const { colors } = useTheme();
  const { label, tr, date } = useLabels();
  const { data, setCropStage, deleteCrop } = useUserData();
  const [taskEditor, setTaskEditor] = useState<{ task: Task | null } | null>(null);
  const [noteEditor, setNoteEditor] = useState<{ note: Note | null } | null>(null);
  const [confirm, setConfirm] = useState(false);
  const crop = alive(data.crops).find((c) => c.id === id);
  const plant = crop ? catalog.plantsById[crop.plantId] : undefined;

  if (!crop || !plant) {
    return <Screen title={t('crop_not_found')} back><EmptyState icon="help-circle-outline" title={t('crop_not_found')} /></Screen>;
  }
  const garden = data.gardens.find((g) => g.id === crop.gardenId);
  const plot = data.plots.find((p) => p.id === crop.plotId);
  const tasks = alive(data.tasks).filter((x) => x.cropId === crop.id).sort((a, b) => Number(a.done) - Number(b.done) || a.dueDate.localeCompare(b.dueDate));
  const notes = alive(data.notes).filter((n) => n.cropId === crop.id);
  const harvest = harvestEstimate(plant, crop);
  const elapsed = Math.max(0, daysBetween(crop.startDate, today()));
  const total = harvest ? daysBetween(crop.startDate, harvest) : 0;
  const progress = total > 0 ? Math.min(1, elapsed / total) : null;

  return (
    <Screen title={t('cultivation')} back right={(
      <>
        <HeaderIcon icon="create-outline" label={t('add_note')} onPress={() => setNoteEditor({ note: null })} />
        <HeaderIcon icon="trash-outline" label={t('delete')} onPress={() => setConfirm(true)} testID="delete-crop" />
      </>
    )}>
      <PlantRow plant={plant} subtitle={label('stage', crop.stage)} onPress={() => router.push({ pathname: '/plant/[id]', params: { id: plant.id } })} />
      <Card>
        <InfoRow label={t('garden')} value={garden?.name} />
        <InfoRow label={t('plot')} value={plot?.name ?? t('no_plot')} />
        <InfoRow label={t('cultivation_method')} value={tr(catalog.methodsById[crop.methodId]?.names) || crop.methodId} />
        <InfoRow label={t('start_activity')} value={`${label('act', crop.startActivity)} · ${date(crop.startDate)}`} />
        <InfoRow label={t('days_since_start')} value={tn('days', elapsed)} />
        {!!crop.quantity && <InfoRow label={t('quantity_optional')} value={crop.quantity} />}
        {!!crop.harvestedOn && <InfoRow label={t('stage_finished')} value={date(crop.harvestedOn)} />}
      </Card>

      {progress != null && crop.stage !== 'finished' && (
        <Card>
          <Body bold small>{t('progress_to_harvest')}</Body>
          <View style={{ height: 10, backgroundColor: colors.monthOff, borderRadius: BORDER_RADIUS.round, marginVertical: SPACING.sm, overflow: 'hidden' }}>
            <View style={{ width: `${Math.round(progress * 100)}%`, height: 10, backgroundColor: colors.primary }} />
          </View>
          <Body small>{`${Math.round(progress * 100)} % · ${label('act', 'harvest')} ≈ ${date(harvest!)} (${t('estimated')})`}</Body>
          <InfoNote text={t('progress_estimate')} />
        </Card>
      )}

      <SectionTitle title={t('stage')} />
      <ChipRow>
        {CROP_STAGES.map((s) => <Chip key={s} label={label('stage', s)} selected={crop.stage === s} onPress={() => setCropStage(crop.id, s)} testID={`stage-${s}`} />)}
      </ChipRow>

      <SectionTitle title={t('crop_tasks')} action={<Button variant="text" icon="add" label={t('new_task')} onPress={() => setTaskEditor({ task: null })} />} />
      {tasks.length ? tasks.map((x) => <TaskRow key={x.id} task={x} onPress={() => setTaskEditor({ task: x })} />) : <Body small secondary>{t('no_crop_tasks')}</Body>}

      <SectionTitle title={t('my_notes')} action={<Button variant="text" icon="add" label={t('add_note')} onPress={() => setNoteEditor({ note: null })} />} />
      {notes.length ? notes.map((n) => (
        <Card key={n.id} onPress={() => setNoteEditor({ note: n })}>
          {!!n.title && <Body bold>{n.title}</Body>}
          <Body small>{n.body}</Body>
        </Card>
      )) : <Body small secondary>{t('no_notes_crop')}</Body>}

      <TaskEditor visible={!!taskEditor} task={taskEditor?.task ?? null} defaults={{ plantId: plant.id, cropId: crop.id }} onClose={() => setTaskEditor(null)} />
      <NoteEditor visible={!!noteEditor} note={noteEditor?.note ?? null} plantId={plant.id} cropId={crop.id} onClose={() => setNoteEditor(null)} />
      <ConfirmSheet visible={confirm} title={t('delete_crop')} message={t('delete_crop_text')} onCancel={() => setConfirm(false)}
        onConfirm={() => { setConfirm(false); deleteCrop(crop.id); router.back(); }} />
    </Screen>
  );
}
