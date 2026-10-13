import React from 'react';
import { View } from 'react-native';
import { SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { Body, Button, Sheet, useRow } from './ui';

/** Confirmation dialog that works the same on phones and on the web. */
export const ConfirmSheet: React.FC<{
  visible: boolean; title: string; message?: string; confirmLabel?: string; destructive?: boolean; onCancel: () => void; onConfirm: () => void;
}> = ({ visible, title, message, confirmLabel, destructive = true, onCancel, onConfirm }) => {
  const { t } = useLanguage();
  const { colors } = useTheme();
  const row = useRow();
  return (
    <Sheet visible={visible} onClose={onCancel} title={title}>
      {!!message && <Body>{message}</Body>}
      <View style={[row, { justifyContent: 'flex-end', gap: SPACING.sm, marginTop: SPACING.md }]}>
        <Button variant="text" label={t('cancel')} onPress={onCancel} />
        <Button label={confirmLabel ?? t('delete')} color={destructive ? colors.error : undefined} onPress={onConfirm} testID="confirm" />
      </View>
    </Sheet>
  );
};
