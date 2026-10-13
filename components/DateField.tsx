import React, { useState } from 'react';
import { Platform, Text, TouchableOpacity, View } from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { Ionicons } from '@expo/vector-icons';
import { BORDER_RADIUS, FONT, SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { useLabels } from '../hooks/useLabels';
import { IsoDate, isValidIso, parseIso, toIso } from '../lib/dates';
import { Field, useRow } from './ui';

/** Date input: the system date picker on phones, a YYYY-MM-DD text field on the web. */
export const DateField: React.FC<{ label: string; value: IsoDate; onChange: (d: IsoDate) => void; testID?: string }> = (
  { label, value, onChange, testID },
) => {
  const { colors } = useTheme();
  const { align } = useLanguage();
  const { date } = useLabels();
  const row = useRow();
  const [open, setOpen] = useState(false);
  const [text, setText] = useState(value);

  if (Platform.OS === 'web') {
    return (
      <Field label={label} value={text} placeholder="2026-03-15" error={!isValidIso(text)} testID={testID}
        onChangeText={(v) => { setText(v); if (isValidIso(v)) onChange(v); }} />
    );
  }
  return (
    <View style={{ marginVertical: SPACING.xs }}>
      <Text style={{ color: colors.textSecondary, fontSize: FONT.sm, marginBottom: 4, textAlign: align }}>{label}</Text>
      <TouchableOpacity onPress={() => setOpen(true)} testID={testID}
        style={[row, { borderWidth: 1, borderColor: colors.divider, borderRadius: BORDER_RADIUS.md, paddingHorizontal: SPACING.md, paddingVertical: 12, alignItems: 'center', gap: SPACING.sm, backgroundColor: colors.surface }]}>
        <Ionicons name="calendar-outline" size={18} color={colors.primary} />
        <Text style={{ flex: 1, color: colors.text, fontSize: FONT.md, textAlign: align }}>{date(value)}</Text>
      </TouchableOpacity>
      {open && (
        <DateTimePicker value={parseIso(value)} mode="date" display="default"
          onChange={(event, picked) => {
            setOpen(Platform.OS === 'ios');
            if (event.type === 'set' && picked) onChange(toIso(picked));
          }} />
      )}
    </View>
  );
};
