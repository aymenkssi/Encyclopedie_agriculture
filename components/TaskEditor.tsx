import React, { useState } from 'react';
import { ScrollView, Switch, View } from 'react-native';
import { SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { useUserData } from '../contexts/UserDataContext';
import { useLabels } from '../hooks/useLabels';
import { displayName } from '../lib/catalog';
import { IsoDate, today } from '../lib/dates';
import { catalog } from '../lib/knowledge';
import { Activity, ACTIVITIES } from '../lib/types';
import { Task } from '../lib/userData';
import { DateField } from './DateField';
import { Body, Button, Field, Select, Sheet, useRow } from './ui';

const REPEATS: (number | null)[] = [null, 1, 2, 3, 7, 14, 30];

/** Creates or edits a task of the personal calendar. */
type Props = {
  visible: boolean; task: Task | null; defaults?: { dueDate?: IsoDate; plantId?: string | null; cropId?: string | null }; onClose: () => void;
};

/** The form is mounted only while visible, so it always starts from the edited task. */
export const TaskEditor: React.FC<Props> = (props) => (props.visible ? <TaskForm key={props.task?.id ?? 'new'} {...props} /> : null);

const TaskForm: React.FC<Props> = ({ visible, task, defaults, onClose }) => {
  const { t, tn, language } = useLanguage();
  const { colors } = useTheme();
  const { label } = useLabels();
  const { saveTask, deleteTask } = useUserData();
  const row = useRow();
  const [title, setTitle] = useState(task?.title ?? '');
  const [description, setDescription] = useState(task?.description ?? '');
  const [activity, setActivity] = useState<Activity>(task?.activity ?? 'watering');
  const [dueDate, setDueDate] = useState<IsoDate>(task?.dueDate ?? defaults?.dueDate ?? today());
  const [plantId, setPlantId] = useState<string | null>(task?.plantId ?? defaults?.plantId ?? null);
  const [reminder, setReminder] = useState(task?.reminder ?? true);
  const [repeatDays, setRepeatDays] = useState<number | null>(task?.repeatDays ?? null);
  const [confirm, setConfirm] = useState(false);

  const save = async () => {
    const plant = plantId ? catalog.plantsById[plantId] : null;
    const fallback = plant ? t('task_title_auto', { a: label('act', activity), b: displayName(plant, language) }) : label('act', activity);
    await saveTask(task?.id ?? null, {
      title: title.trim() || fallback, description, activity, dueDate, plantId, cropId: task?.cropId ?? defaults?.cropId ?? null, reminder, repeatDays,
    }, t('notif_title'));
    onClose();
  };

  return (
    <Sheet visible={visible} onClose={onClose} title={t(task ? 'edit_task' : 'new_task')}>
      {confirm ? (
        <>
          <Body>{t('delete_task_text')}</Body>
          <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.md }]}>
            <Button variant="text" label={t('cancel')} onPress={() => setConfirm(false)} />
            <Button label={t('delete')} color={colors.error} onPress={() => { if (task) deleteTask(task.id); onClose(); }} testID="confirm" />
          </View>
        </>
      ) : (
        <ScrollView keyboardShouldPersistTaps="handled">
          <Field label={t('title')} value={title} onChangeText={setTitle} placeholder={label('act', activity)} testID="task-title" />
          <Select label={t('activity')} value={activity} options={ACTIVITIES} display={(a) => label('act', a)} onChange={setActivity} />
          <DateField label={t('due_date')} value={dueDate} onChange={setDueDate} testID="task-date" />
          <Select label={t('plant_optional')} value={plantId} options={[null, ...catalog.plants.map((p) => p.id)]}
            display={(id) => (id ? displayName(catalog.plantsById[id], language) : t('none'))} onChange={setPlantId} />
          <Field label={t('description')} value={description} onChangeText={setDescription} multiline />
          <Select label={t('repeat')} value={repeatDays} options={REPEATS}
            display={(n) => (n ? tn('every_n_days', n) : t('no_repeat'))} onChange={setRepeatDays} />
          <View style={[row, { alignItems: 'center', justifyContent: 'space-between', marginVertical: SPACING.sm }]}>
            <View style={{ flex: 1 }}><Body>{t('reminder')}</Body><Body small secondary>{t('notifications_hint')}</Body></View>
            <Switch value={reminder} onValueChange={setReminder} trackColor={{ true: colors.primary }} />
          </View>
          <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.sm }]}>
            {task && <Button variant="text" label={t('delete')} color={colors.error} onPress={() => setConfirm(true)} />}
            <Button variant="text" label={t('cancel')} onPress={onClose} />
            <Button label={t('save')} onPress={save} testID="task-save" />
          </View>
        </ScrollView>
      )}
    </Sheet>
  );
};
