import React, { useState } from 'react';
import { View } from 'react-native';
import { router } from 'expo-router';
import { SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useSettings } from '../contexts/SettingsContext';
import { useTheme } from '../contexts/ThemeContext';
import { useUserData } from '../contexts/UserDataContext';
import { useLabels } from '../hooks/useLabels';
import { catalog } from '../lib/knowledge';
import { FARM_TYPES, FarmType } from '../lib/recommend';
import { Garden } from '../lib/userData';
import { RegionPicker } from './RegionPicker';
import { Body, Button, Field, Select, Sheet, useRow } from './ui';

type Props = { visible: boolean; garden: Garden | null; onClose: () => void };

export const parseArea = (text: string): number | null | undefined => {
  const v = text.trim().replace(',', '.');
  if (!v) return null;
  const n = Number(v);
  return Number.isFinite(n) && n > 0 ? n : undefined;
};

export const GardenEditor: React.FC<Props> = (props) => (props.visible ? <GardenForm key={props.garden?.id ?? 'new'} {...props} /> : null);

const GardenForm: React.FC<Props> = ({ visible, garden, onClose }) => {
  const { t } = useLanguage();
  const { colors } = useTheme();
  const { label, tr } = useLabels();
  const settings = useSettings();
  const { saveGarden, deleteGarden } = useUserData();
  const row = useRow();
  const [name, setName] = useState(garden?.name ?? '');
  const [farmType, setFarmType] = useState<FarmType>(garden?.farmType ?? settings.farmType);
  const [area, setArea] = useState(garden?.areaM2 != null ? String(garden.areaM2) : '');
  const [regionId, setRegionId] = useState<string | null>(garden?.regionId ?? settings.regionId);
  const [picker, setPicker] = useState(false);
  const [confirm, setConfirm] = useState(false);
  const areaValue = parseArea(area);
  const region = regionId ? catalog.regionsById[regionId] : null;

  const save = () => {
    if (areaValue === undefined) return;
    saveGarden(garden?.id ?? null, { name: name.trim() || t('default_garden_name'), farmType, areaM2: areaValue, regionId });
    onClose();
  };

  return (
    <Sheet visible={visible} onClose={onClose} title={t(garden ? 'edit_garden' : 'new_garden')}>
      {confirm ? (
        <>
          <Body>{t('delete_garden_text')}</Body>
          <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.md }]}>
            <Button variant="text" label={t('cancel')} onPress={() => setConfirm(false)} />
            <Button label={t('delete')} color={colors.error} testID="confirm" onPress={() => {
              if (garden) deleteGarden(garden.id);
              onClose();
              if (router.canGoBack()) router.back();
            }} />
          </View>
        </>
      ) : (
        <>
          <Field label={t('name')} value={name} onChangeText={setName} placeholder={t('default_garden_name')} testID="garden-name" />
          <Select label={t('garden_type')} value={farmType} options={FARM_TYPES} display={(f) => label('farm', f)} onChange={setFarmType} />
          <Field label={t('area_m2')} value={area} onChangeText={setArea} keyboardType="decimal-pad" error={areaValue === undefined} />
          {areaValue === undefined && <Body small color={colors.error}>{t('invalid_number')}</Body>}
          <Button variant="outline" icon="location-outline" onPress={() => setPicker(true)}
            label={region ? `${tr(region.names)} · ${tr(catalog.countriesById[region.countryId]?.names)}` : t('choose_region')} />
          {farmType === 'large_farm' && <Body small secondary>{t('large_farm_future')}</Body>}
          <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.sm }]}>
            {garden && <Button variant="text" label={t('delete')} color={colors.error} onPress={() => setConfirm(true)} />}
            <Button variant="text" label={t('cancel')} onPress={onClose} />
            <Button label={t('save')} onPress={save} disabled={areaValue === undefined} testID="garden-save" />
          </View>
          <RegionPicker visible={picker} currentRegionId={regionId} onClose={() => setPicker(false)} onPick={(r) => setRegionId(r.id)} />
        </>
      )}
    </Sheet>
  );
};
