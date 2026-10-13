import React, { ReactNode } from 'react';
import { Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { FONT, SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { displayName } from '../lib/catalog';
import { Plant } from '../lib/types';
import { Card, Pill, StatusBadge, useRow } from './ui';

export const CATEGORY_ICONS: Record<string, keyof typeof Ionicons.glyphMap> = {
  vegetable: 'leaf-outline', fruit: 'nutrition-outline', legume: 'ellipse-outline', cereal: 'barcode-outline',
  aromatic: 'flower-outline', industrial: 'construct-outline', other: 'apps-outline',
};

/** Illustration placeholder: category icon on a tinted circle (photos can be added per plant later). */
export const PlantAvatar: React.FC<{ plant: Plant; size?: number }> = ({ plant, size = 48 }) => {
  const { colors } = useTheme();
  return (
    <View style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: colors.primaryContainer, alignItems: 'center', justifyContent: 'center' }}>
      <Ionicons name={CATEGORY_ICONS[plant.categoryId] ?? 'leaf-outline'} size={size * 0.55} color={colors.onPrimaryContainer} />
    </View>
  );
};

export const PlantRow: React.FC<{ plant: Plant; subtitle?: string | null; badges?: ReactNode; onPress: () => void; testID?: string }> = (
  { plant, subtitle, badges, onPress, testID },
) => {
  const { colors } = useTheme();
  const { language, align, isRTL } = useLanguage();
  const row = useRow();
  return (
    <Card onPress={onPress} testID={testID}>
      <View style={[row, { alignItems: 'center', gap: SPACING.md }]}>
        <PlantAvatar plant={plant} />
        <View style={{ flex: 1, gap: 2 }}>
          <Text style={{ color: colors.text, fontSize: FONT.lg, fontWeight: '700', textAlign: align }} numberOfLines={1}>{displayName(plant, language)}</Text>
          <Text style={{ color: colors.textSecondary, fontSize: FONT.sm, fontStyle: 'italic', textAlign: align }} numberOfLines={1}>{plant.scientificName}</Text>
          {!!subtitle && <Text style={{ color: colors.text, fontSize: FONT.sm, textAlign: align }} numberOfLines={2}>{subtitle}</Text>}
          {badges && <View style={[row, { gap: SPACING.xs, marginTop: 4, flexWrap: 'wrap' }]}>{badges}</View>}
        </View>
        <Ionicons name={isRTL ? 'chevron-back' : 'chevron-forward'} size={20} color={colors.textSecondary} />
      </View>
    </Card>
  );
};

export const SuitabilityPill: React.FC<{ level: 'recommended' | 'possible' | 'not_recommended' | 'unknown'; label: string }> = ({ level, label }) => {
  const { colors } = useTheme();
  const [bg, fg] = level === 'recommended' ? [colors.primaryContainer, colors.onPrimaryContainer]
    : level === 'possible' ? [colors.earthContainer, colors.onEarthContainer]
      : level === 'not_recommended' ? [colors.errorContainer, colors.error] : [colors.chip, colors.text];
  return (
    <View style={{ backgroundColor: bg, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 3, alignSelf: 'flex-start' }}>
      <Text style={{ color: fg, fontSize: FONT.xs, fontWeight: '700' }}>{label}</Text>
    </View>
  );
};

/** Validation status, difficulty and demo flag of a plant sheet. */
export const StatusBadgeRow: React.FC<{ plant: Plant; difficulty: string }> = ({ plant, difficulty }) => {
  const { colors } = useTheme();
  const { t } = useLanguage();
  return (
    <>
      <StatusBadge status={plant.status} />
      <Pill label={difficulty} bg={colors.chip} fg={colors.text} />
      {plant.isDemo && <Pill label={t('demo_badge')} bg={colors.clayContainer} fg={colors.onClayContainer} />}
    </>
  );
};
