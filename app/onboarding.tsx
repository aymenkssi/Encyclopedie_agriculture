import React, { useState } from 'react';
import { ScrollView, View } from 'react-native';
import { router } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { RegionPicker } from '../components/RegionPicker';
import { Body, Button, Chip, ChipRow, DemoBanner, Title, useRow } from '../components/ui';
import { FONT, SPACING } from '../constants/theme';
import { Language, useLanguage } from '../contexts/LanguageContext';
import { useSettings } from '../contexts/SettingsContext';
import { useTheme } from '../contexts/ThemeContext';
import { useLabels } from '../hooks/useLabels';
import { catalog } from '../lib/knowledge';
import { defaultMethodFor, EXPERIENCE_LEVELS, ExperienceLevel, FARM_TYPES, FarmType } from '../lib/recommend';

const LANGUAGE_NAMES: [Language, string][] = [['fr', 'Français'], ['ar', 'العربية'], ['en', 'English']];

/** First launch: language, region (optional), experience and type of growing space. No account. */
export default function Onboarding() {
  const { colors } = useTheme();
  const { t, language, setLanguage } = useLanguage();
  const { label, tr } = useLabels();
  const settings = useSettings();
  const row = useRow();
  const [step, setStep] = useState(0);
  const [regionId, setRegionId] = useState<string | null>(settings.regionId);
  const [experience, setExperience] = useState<ExperienceLevel>(settings.experience);
  const [farm, setFarm] = useState<FarmType>(settings.farmType);
  const [picker, setPicker] = useState(false);
  const steps = 4;

  const finish = () => {
    settings.update({ onboardingDone: true, regionId, experience, farmType: farm, preferredMethod: defaultMethodFor(farm) });
    router.replace('/(tabs)/home');
  };
  const region = regionId ? catalog.regionsById[regionId] : null;

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.background }}>
      <View style={{ height: 4, backgroundColor: colors.monthOff, margin: SPACING.md, borderRadius: 2 }}>
        <View style={{ height: 4, width: `${((step + 1) / steps) * 100}%`, backgroundColor: colors.primary, borderRadius: 2 }} />
      </View>
      <ScrollView contentContainerStyle={{ padding: SPACING.lg, gap: SPACING.md }}>
        {step === 0 && (
          <>
            <Ionicons name="leaf" size={56} color={colors.primary} />
            <Title size={FONT.xxl}>{t('onb_welcome_title')}</Title>
            <Body>{t('onb_welcome_text')}</Body>
            <Title>{t('language')}</Title>
            <ChipRow>
              {LANGUAGE_NAMES.map(([code, name]) => (
                <Chip key={code} label={name} selected={language === code} onPress={() => setLanguage(code)} testID={`lang-${code}`} />
              ))}
            </ChipRow>
            <DemoBanner text={tr(catalog.manifest.disclaimer)} />
          </>
        )}
        {step === 1 && (
          <>
            <Ionicons name="location" size={48} color={colors.primary} />
            <Title size={FONT.xxl}>{t('onb_region_title')}</Title>
            <Body>{t('onb_region_text')}</Body>
            <Button variant="outline" icon="map" testID="choose-region"
              label={region ? `${tr(catalog.countriesById[region.countryId]?.names)} · ${tr(region.names)}` : t('choose_region')}
              onPress={() => setPicker(true)} />
            <Body small secondary>{t('onb_region_skip')}</Body>
          </>
        )}
        {step === 2 && (
          <>
            <Title size={FONT.xxl}>{t('onb_experience_title')}</Title>
            {EXPERIENCE_LEVELS.map((e) => <Chip key={e} label={label('exp', e)} selected={experience === e} onPress={() => setExperience(e)} />)}
          </>
        )}
        {step === 3 && (
          <>
            <Title size={FONT.xxl}>{t('onb_farm_title')}</Title>
            {FARM_TYPES.map((f) => <Chip key={f} label={label('farm', f)} selected={farm === f} onPress={() => setFarm(f)} />)}
          </>
        )}
      </ScrollView>
      <View style={[row, { padding: SPACING.md, gap: SPACING.sm, alignItems: 'center' }]}>
        {step > 0 && <Button variant="text" label={t('previous')} onPress={() => setStep(step - 1)} />}
        <View style={{ flex: 1 }} />
        {step < steps - 1 && <Button variant="text" label={t('skip')} onPress={finish} testID="skip" />}
        <Button label={step < steps - 1 ? t('next') : t('start')} onPress={step < steps - 1 ? () => setStep(step + 1) : finish} testID="next" />
      </View>
      <RegionPicker visible={picker} currentRegionId={regionId} onClose={() => setPicker(false)} onPick={(r) => setRegionId(r.id)} />
    </SafeAreaView>
  );
}
