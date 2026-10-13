import React, { useState } from 'react';
import { View } from 'react-native';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { AddCropSheet } from '../../components/AddCropSheet';
import { Fab } from '../../components/Fab';
import { GardenEditor } from '../../components/GardenEditor';
import { PlantRow } from '../../components/PlantRow';
import { Screen } from '../../components/Screen';
import { Body, Button, Card, EmptyState, SectionTitle, useRow } from '../../components/ui';
import { SPACING } from '../../constants/theme';
import { useLanguage } from '../../contexts/LanguageContext';
import { useTheme } from '../../contexts/ThemeContext';
import { useUserData } from '../../contexts/UserDataContext';
import { useLabels } from '../../hooks/useLabels';
import { catalog } from '../../lib/knowledge';
import { alive } from '../../lib/userData';

const FARM_ICONS = { balcony: 'flower-outline', garden: 'leaf-outline', small_farm: 'home-outline', medium_farm: 'business-outline', large_farm: 'globe-outline' } as const;

export default function Crops() {
  const { colors } = useTheme();
  const { t, tn } = useLanguage();
  const { label, tr, date } = useLabels();
  const { data } = useUserData();
  const row = useRow();
  const [gardenEditor, setGardenEditor] = useState(false);
  const [addCrop, setAddCrop] = useState(false);
  const gardens = alive(data.gardens);
  const crops = alive(data.crops);
  const ongoing = crops.filter((c) => c.stage !== 'finished').sort((a, b) => b.startDate.localeCompare(a.startDate));

  return (
    <Screen title={t('tab_crops')} fab={<Fab label={t('add_crop')} onPress={() => setAddCrop(true)} testID="add-crop" />}>
      <SectionTitle title={t('my_gardens')} action={<Button variant="text" icon="add" label={t('new_garden')} onPress={() => setGardenEditor(true)} testID="new-garden" />} />
      {gardens.length === 0 ? (
        <EmptyState icon="leaf-outline" title={t('no_garden_title')} message={t('no_garden_text')}
          action={<Button label={t('new_garden')} onPress={() => setGardenEditor(true)} />} />
      ) : gardens.map((g) => {
        const n = crops.filter((c) => c.gardenId === g.id && c.stage !== 'finished').length;
        const region = g.regionId ? catalog.regionsById[g.regionId] : null;
        return (
          <Card key={g.id} onPress={() => router.push({ pathname: '/garden/[id]', params: { id: g.id } })} testID={`garden-${g.id}`}>
            <View style={[row, { alignItems: 'center', gap: SPACING.md }]}>
              <Ionicons name={FARM_ICONS[g.farmType]} size={28} color={colors.primary} />
              <View style={{ flex: 1 }}>
                <Body bold>{g.name}</Body>
                <Body small secondary>{[label('farm', g.farmType), g.areaM2 ? `${g.areaM2} m²` : null, region ? tr(region.names) : null].filter(Boolean).join(' · ')}</Body>
                <Body small>{tn('crops_count', n)}</Body>
              </View>
            </View>
          </Card>
        );
      })}

      <SectionTitle title={t('ongoing_crops')} />
      {ongoing.length === 0 ? <Body small secondary>{t('no_crops_in_garden')}</Body> : ongoing.map((c) => {
        const plant = catalog.plantsById[c.plantId];
        if (!plant) return null;
        return (
          <PlantRow key={c.id} plant={plant} testID={`crop-${c.id}`}
            subtitle={`${label('stage', c.stage)} · ${label('act', c.startActivity)} ${date(c.startDate)}`}
            onPress={() => router.push({ pathname: '/crop/[id]', params: { id: c.id } })} />
        );
      })}
      {crops.some((c) => c.stage === 'finished') && (
        <Button variant="text" icon="time-outline" label={t('history_crops')} onPress={() => router.push('/history')} />
      )}
      <GardenEditor visible={gardenEditor} garden={null} onClose={() => setGardenEditor(false)} />
      <AddCropSheet visible={addCrop} plant={null} onClose={() => setAddCrop(false)} />
    </Screen>
  );
}
