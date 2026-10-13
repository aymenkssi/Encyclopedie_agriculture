import React, { useState } from 'react';
import { View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { AddCropSheet } from '../../components/AddCropSheet';
import { Fab } from '../../components/Fab';
import { GardenEditor, parseArea } from '../../components/GardenEditor';
import { PlantRow } from '../../components/PlantRow';
import { Screen } from '../../components/Screen';
import { HeaderIcon } from '../../components/ScreenHeader';
import { Body, Button, Card, EmptyState, Field, InfoNote, Select, SectionTitle, Sheet, useRow } from '../../components/ui';
import { SPACING } from '../../constants/theme';
import { useLanguage } from '../../contexts/LanguageContext';
import { useTheme } from '../../contexts/ThemeContext';
import { useUserData } from '../../contexts/UserDataContext';
import { useLabels } from '../../hooks/useLabels';
import { displayName } from '../../lib/catalog';
import { catalog } from '../../lib/knowledge';
import { ALL_METHODS } from '../../lib/types';
import { alive } from '../../lib/userData';

export default function GardenScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { t, tn, language } = useLanguage();
  const { colors } = useTheme();
  const { label, tr, date } = useLabels();
  const { data, savePlot, deletePlot } = useUserData();
  const row = useRow();
  const [editor, setEditor] = useState(false);
  const [addCrop, setAddCrop] = useState(false);
  const [plotSheet, setPlotSheet] = useState(false);
  const [plotName, setPlotName] = useState('');
  const [plotArea, setPlotArea] = useState('');
  const [plotSoil, setPlotSoil] = useState<string | null>(null);
  const [plotMethod, setPlotMethod] = useState('open_field');
  const garden = alive(data.gardens).find((g) => g.id === id);

  if (!garden) {
    return <Screen title={t('garden_not_found')} back><EmptyState icon="help-circle-outline" title={t('garden_not_found')} /></Screen>;
  }
  const region = garden.regionId ? catalog.regionsById[garden.regionId] : null;
  const plots = alive(data.plots).filter((p) => p.gardenId === garden.id);
  const crops = alive(data.crops).filter((c) => c.gardenId === garden.id);
  const used = plots.reduce((s, p) => s + (p.areaM2 ?? 0), 0);
  const areaValue = parseArea(plotArea);

  const openPlotSheet = () => { setPlotName(''); setPlotArea(''); setPlotSoil(null); setPlotMethod('open_field'); setPlotSheet(true); };
  const addPlot = () => {
    if (areaValue === undefined) return;
    savePlot({ gardenId: garden.id, name: plotName.trim() || `${t('plot')} ${plots.length + 1}`, areaM2: areaValue, soilId: plotSoil, methodId: plotMethod });
    setPlotSheet(false);
  };

  return (
    <Screen title={garden.name} back right={<HeaderIcon icon="create-outline" label={t('edit')} onPress={() => setEditor(true)} testID="edit-garden" />}
      fab={<Fab label={t('add_crop')} onPress={() => setAddCrop(true)} testID="garden-add-crop" />}>
      <Card>
        <Body bold>{label('farm', garden.farmType)}</Body>
        {garden.areaM2 != null && <Body small>{`${t('area_m2')} : ${garden.areaM2}`}</Body>}
        <Body small secondary>{t('garden_region', { a: region ? `${tr(region.names)} · ${tr(catalog.countriesById[region.countryId]?.names)}` : t('no_region') })}</Body>
        {garden.farmType === 'large_farm' && <InfoNote text={t('large_farm_future')} />}
      </Card>

      <SectionTitle title={t('plots')} action={<Button variant="text" icon="add" label={t('new_plot')} onPress={openPlotSheet} testID="new-plot" />} />
      {garden.areaM2 != null && used > 0 && (
        <Body small color={used > garden.areaM2 ? colors.error : colors.textSecondary}>
          {used > garden.areaM2 ? t('plots_area_exceeded') : t('plots_area_used', { a: `${used} m²`, b: `${garden.areaM2} m²` })}
        </Body>
      )}
      {plots.length === 0 ? <Body small secondary>{t('no_plots')}</Body> : plots.map((p) => {
        const current = crops.filter((c) => c.plotId === p.id && c.stage !== 'finished');
        return (
          <Card key={p.id}>
            <View style={[row, { alignItems: 'center', gap: SPACING.sm }]}>
              <View style={{ flex: 1 }}>
                <Body bold>{p.name}</Body>
                <Body small secondary>{[p.areaM2 ? `${p.areaM2} m²` : null, p.soilId ? tr(catalog.soilTypesById[p.soilId]?.names) : null,
                  tr(catalog.methodsById[p.methodId]?.names)].filter(Boolean).join(' · ')}</Body>
                <Body small>{current.length ? current.map((c) => (catalog.plantsById[c.plantId] ? displayName(catalog.plantsById[c.plantId], language) : '')).join(', ') : t('plot_free')}</Body>
              </View>
              <HeaderIcon icon="trash-outline" label={t('delete')} onPress={() => deletePlot(p.id)} color={colors.textSecondary} />
            </View>
          </Card>
        );
      })}

      <SectionTitle title={t('crops_in_garden')} />
      <Body small secondary>{tn('crops_count', crops.filter((c) => c.stage !== 'finished').length)}</Body>
      {crops.length === 0 ? <Body small secondary>{t('no_crops_in_garden')}</Body> : crops
        .sort((a, b) => Number(a.stage === 'finished') - Number(b.stage === 'finished') || b.startDate.localeCompare(a.startDate))
        .map((c) => {
          const plant = catalog.plantsById[c.plantId];
          if (!plant) return null;
          const plot = plots.find((p) => p.id === c.plotId);
          return (
            <PlantRow key={c.id} plant={plant} onPress={() => router.push({ pathname: '/crop/[id]', params: { id: c.id } })}
              subtitle={[label('stage', c.stage), date(c.startDate), plot?.name].filter(Boolean).join(' · ')} />
          );
        })}

      <GardenEditor visible={editor} garden={garden} onClose={() => setEditor(false)} />
      <AddCropSheet visible={addCrop} plant={null} gardenId={garden.id} onClose={() => setAddCrop(false)} />
      <Sheet visible={plotSheet} onClose={() => setPlotSheet(false)} title={t('new_plot')}>
        <Field label={t('name')} value={plotName} onChangeText={setPlotName} placeholder={`${t('plot')} ${plots.length + 1}`} testID="plot-name" />
        <Field label={t('area_m2')} value={plotArea} onChangeText={setPlotArea} keyboardType="decimal-pad" error={areaValue === undefined} />
        <Select label={t('soil')} value={plotSoil} options={[null, ...catalog.reference.soilTypes.map((s) => s.id)]}
          display={(s) => (s ? tr(catalog.soilTypesById[s]?.names) : t('unknown'))} onChange={setPlotSoil} />
        <Select label={t('cultivation_method')} value={plotMethod} options={ALL_METHODS} display={(m) => tr(catalog.methodsById[m]?.names) || m} onChange={setPlotMethod} />
        <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.sm }]}>
          <Button variant="text" label={t('cancel')} onPress={() => setPlotSheet(false)} />
          <Button label={t('add')} onPress={addPlot} disabled={areaValue === undefined} testID="plot-save" />
        </View>
      </Sheet>
    </Screen>
  );
}
