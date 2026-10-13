import React from 'react';
import { Text, View } from 'react-native';
import { MONTH_INITIALS } from '../constants/months';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';

/** 12 cells; highlighted months are filled, preferred months are taller. Always January → December, left to right. */
export const MonthStrip: React.FC<{ months: Set<number>; preferred?: Set<number>; current?: number; color?: string; label?: string }> = (
  { months, preferred, current, color, label },
) => {
  const { colors } = useTheme();
  const fill = color ?? colors.primary;
  return (
    <View style={{ flexDirection: 'row', gap: 2 }} accessibilityLabel={label}>
      {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => {
        const on = months.has(m);
        const pref = preferred?.has(m);
        return (
          <View key={m} style={{
            flex: 1, height: pref ? 18 : 14, borderRadius: 3, alignItems: 'center', justifyContent: 'center',
            backgroundColor: on ? fill : colors.monthOff, opacity: on && preferred?.size && !pref ? 0.6 : 1,
          }}>
            {m === current && <View style={{ width: 5, height: 5, borderRadius: 3, backgroundColor: on ? colors.onPrimary : colors.textSecondary }} />}
          </View>
        );
      })}
    </View>
  );
};

export const MonthAxis: React.FC = () => {
  const { colors } = useTheme();
  const { language } = useLanguage();
  return (
    <View style={{ flexDirection: 'row', gap: 2 }}>
      {MONTH_INITIALS[language].map((m, i) => (
        <Text key={i} style={{ flex: 1, textAlign: 'center', fontSize: 10, color: colors.textSecondary }}>{m}</Text>
      ))}
    </View>
  );
};
