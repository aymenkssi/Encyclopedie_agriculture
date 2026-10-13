import React, { useMemo, useState } from 'react';
import { ScrollView, Switch, View } from 'react-native';
import { router } from 'expo-router';
import { SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useSettings } from '../contexts/SettingsContext';
import { useTheme } from '../contexts/ThemeContext';
import { useUserData } from '../contexts/UserDataContext';
import { useLabels } from '../hooks/useLabels';
import { displayName, plantMethods } from '../lib/catalog';
import { IsoDate, today } from '../lib/dates';
import { catalog } from '../lib/knowledge';
import { rotationConflicts } from '../lib/rotation';
import { Activity, Plant, START_ACTIVITIES } from '../lib/types';
import { alive, CROP_STAGES, CropStage } from '../lib/userData';
import { DateField } from './DateField';
import { Body, Button, Field, InfoNote, Select, Sheet, useRow } from './ui';

/** Adds a crop of `plant` (or of a plant chosen in the form) to a garden, with optional planned tasks. */
type Props = { visible: boolean; plant: Plant | null; gardenId?: string | null; onClose: () => void };

export const AddCropSheet: React.FC<Props> = (props) => (props.visible ? <CropForm {...props} /> : null);

const defaultStart = (p: Plant): Activity =>
  p.sowingMode === 'vegetative' ? 'planting' : p.sowingMode === 'transplant' ? 'nursery_sowing' : 'direct_sowing';

const CropForm: React.FC<Props> = ({ visible, plant: initialPlant, gardenId: initialGarden, onClose }) => {
  const { t, language } = useLanguage();
  const { colors } = useTheme();
  const { label, tr } = useLabels();
  const settings = useSettings();
  const { data, addCrop } = useUserData();
  const row = useRow();
  const gardens = alive(data.gardens);
  const first = initialPlant ?? catalog.plants[0];
  const [plantId, setPlantIdState] = useState<string>(first.id);
  const [gardenId, setGardenId] = useState<string | null>(initialGarden ?? gardens[0]?.id ?? null);
  const [plotId, setPlotId] = useState<string | null>(null);
  const methodFor = (p: Plant) => (plantMethods(p).includes(settings.preferredMethod) ? settings.preferredMethod : plantMethods(p)[0]);
  const [methodId, setMethodId] = useState(methodFor(first));
  const [start, setStart] = useState<Activity>(defaultStart(first));
  const [date, setDate] = useState<IsoDate>(today());
  const [stage, setStage] = useState<CropStage>('sown');
  const [quantity, setQuantity] = useState('');
  const [plan, setPlan] = useState(true);
  const [saving, setSaving] = useState(false);
  const plant = catalog.plantsById[plantId];
  const setPlantId = (id: string) => {
    const p = catalog.plantsById[id];
    setPlantIdState(id);
    setMethodId(methodFor(p));
    setStart(defaultStart(p));
  };

  const plots = alive(data.plots).filter((p) => p.gardenId === gardenId);
  const conflicts = useMemo(() => (plotId && plant
    ? rotationConflicts(catalog, plant, alive(data.crops).filter((c) => c.plotId === plotId), today()) : []),
  [plotId, plant, data.crops]);

  const save = async () => {
    if (!plant || saving) return;
    setSaving(true);
    try {
      const id = await addCrop({
        gardenId, plotId, plant, methodId, startActivity: start, startDate: date, stage, quantity: quantity || null, planTasks: plan,
        taskTitle: (a) => t('task_title_auto', { a: label('act', a), b: displayName(plant, language) }),
        defaultGardenName: t('default_garden_name'), defaultRegionId: settings.regionId, defaultFarmType: settings.farmType,
      });
      onClose();
      router.push({ pathname: '/crop/[id]', params: { id } });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Sheet visible={visible} onClose={onClose} title={t('add_crop')}>
      <ScrollView keyboardShouldPersistTaps="handled">
        {!initialPlant && (
          <Select label={t('plant')} value={plantId} options={catalog.plants.map((p) => p.id)}
            display={(id) => displayName(catalog.plantsById[id], language)} onChange={setPlantId} testID="crop-plant" />
        )}
        {gardens.length ? (
          <Select label={t('garden')} value={gardenId} options={gardens.map((g) => g.id)}
            display={(id) => gardens.find((g) => g.id === id)?.name ?? ''} onChange={(id) => { setGardenId(id); setPlotId(null); }} />
        ) : <InfoNote text={t('default_garden_will_be_created')} />}
        {plots.length > 0 && (
          <Select label={t('plot')} value={plotId} options={[null, ...plots.map((p) => p.id)]}
            display={(id) => (id ? plots.find((p) => p.id === id)?.name ?? '' : t('no_plot'))} onChange={setPlotId} />
        )}
        {conflicts.length > 0 && (
          <InfoNote tone="error" text={t('rotation_warning', { a: conflicts.map((p) => displayName(p, language)).join(', ') })} />
        )}
        <Select label={t('cultivation_method')} value={methodId} options={plant ? plantMethods(plant) : []}
          display={(m) => tr(catalog.methodsById[m]?.names) || m} onChange={setMethodId} />
        <Select label={t('start_activity')} value={start} options={START_ACTIVITIES} display={(a) => label('act', a)} onChange={setStart} />
        <DateField label={t('start_date')} value={date} onChange={setDate} testID="crop-date" />
        <Select label={t('stage')} value={stage} options={CROP_STAGES} display={(s) => label('stage', s)} onChange={setStage} />
        <Field label={t('quantity_optional')} value={quantity} onChangeText={setQuantity} />
        <View style={[row, { alignItems: 'center', justifyContent: 'space-between', marginVertical: SPACING.sm, gap: SPACING.sm }]}>
          <View style={{ flex: 1 }}><Body>{t('plan_tasks')}</Body><Body small secondary>{t('plan_tasks_hint')}</Body></View>
          <Switch value={plan} onValueChange={setPlan} trackColor={{ true: colors.primary }} testID="crop-plan" />
        </View>
        <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm }]}>
          <Button variant="text" label={t('cancel')} onPress={onClose} />
          <Button label={t('add')} onPress={save} disabled={saving} testID="crop-save" />
        </View>
      </ScrollView>
    </Sheet>
  );
};
