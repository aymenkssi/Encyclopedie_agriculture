import React, { ReactNode } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { FONT, Palette, SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme, useThemedStyles } from '../contexts/ThemeContext';

interface Props {
  title: string;
  /** Shows a back button (screens opened on top of the tabs). */
  back?: boolean;
  right?: ReactNode;
}

export const ScreenHeader: React.FC<Props> = ({ title, back, right }) => {
  const { colors } = useTheme();
  const styles = useThemedStyles(makeStyles);
  const { t, isRTL } = useLanguage();
  const goBack = () => (router.canGoBack() ? router.back() : router.replace('/(tabs)/home'));
  return (
    <View style={[styles.header, { flexDirection: isRTL ? 'row-reverse' : 'row' }]}>
      <View style={styles.side}>
        {back && (
          <TouchableOpacity onPress={goBack} style={styles.side} accessibilityRole="button" accessibilityLabel={t('back')} testID="back">
            <Ionicons name={isRTL ? 'arrow-forward' : 'arrow-back'} size={24} color={colors.text} />
          </TouchableOpacity>
        )}
      </View>
      <Text style={styles.title} numberOfLines={1}>{title}</Text>
      <View style={[styles.right, { flexDirection: isRTL ? 'row-reverse' : 'row' }]}>{right}</View>
    </View>
  );
};

/** Icon button for the header's right side. */
export const HeaderIcon: React.FC<{ icon: keyof typeof Ionicons.glyphMap; label: string; onPress: () => void; color?: string; testID?: string }> = (
  { icon, label, onPress, color, testID },
) => {
  const { colors } = useTheme();
  return (
    <TouchableOpacity onPress={onPress} accessibilityRole="button" accessibilityLabel={label} testID={testID}
      style={{ width: 40, height: 44, alignItems: 'center', justifyContent: 'center' }}>
      <Ionicons name={icon} size={22} color={color ?? colors.text} />
    </TouchableOpacity>
  );
};

const makeStyles = (c: Palette) => StyleSheet.create({
  header: {
    alignItems: 'center', paddingHorizontal: SPACING.sm, paddingVertical: SPACING.xs, backgroundColor: c.surface,
    borderBottomWidth: 1, borderBottomColor: c.border, minHeight: 52,
  },
  side: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  right: { minWidth: 44, alignItems: 'center', justifyContent: 'flex-end' },
  title: { flex: 1, textAlign: 'center', fontWeight: 'bold', fontSize: FONT.xl - 1, color: c.text },
});
