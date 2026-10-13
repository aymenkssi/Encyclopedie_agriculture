import React from 'react';
import { TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';

/** Floating "add" button, on the trailing side of the reading direction. */
export const Fab: React.FC<{ label: string; onPress: () => void; icon?: keyof typeof Ionicons.glyphMap; testID?: string }> = (
  { label, onPress, icon = 'add', testID },
) => {
  const { colors } = useTheme();
  const { isRTL } = useLanguage();
  return (
    <TouchableOpacity onPress={onPress} accessibilityRole="button" accessibilityLabel={label} testID={testID}
      style={{
        position: 'absolute', bottom: 16, [isRTL ? 'left' : 'right']: 16, width: 56, height: 56, borderRadius: 16,
        backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', elevation: 4,
        shadowColor: '#000', shadowOpacity: 0.2, shadowRadius: 6, shadowOffset: { width: 0, height: 2 },
      }}>
      <Ionicons name={icon} size={28} color={colors.onPrimary} />
    </TouchableOpacity>
  );
};
