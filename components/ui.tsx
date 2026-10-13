import React, { ReactNode, useState } from 'react';
import {
  FlatList, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, TextInputProps, TouchableOpacity, View, ViewStyle,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BORDER_RADIUS, FONT, Palette, SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme, useThemedStyles } from '../contexts/ThemeContext';
import { ValidationStatus } from '../lib/types';

/** Row that follows the reading direction (right-to-left in Arabic). */
export function useRow(): ViewStyle {
  const { isRTL } = useLanguage();
  return { flexDirection: isRTL ? 'row-reverse' : 'row' };
}

export const SectionTitle: React.FC<{ title: string; action?: ReactNode }> = ({ title, action }) => {
  const styles = useThemedStyles(makeStyles);
  const { align } = useLanguage();
  const row = useRow();
  return (
    <View style={[row, styles.sectionHeader]}>
      <Text style={[styles.sectionTitle, { textAlign: align }]}>{title}</Text>
      {action}
    </View>
  );
};

export const Card: React.FC<{ children: ReactNode; onPress?: () => void; style?: ViewStyle; testID?: string }> = ({ children, onPress, style, testID }) => {
  const styles = useThemedStyles(makeStyles);
  if (!onPress) return <View style={[styles.card, style]} testID={testID}>{children}</View>;
  return <TouchableOpacity style={[styles.card, style]} onPress={onPress} activeOpacity={0.7} testID={testID}>{children}</TouchableOpacity>;
};

export const Body: React.FC<{ children: ReactNode; secondary?: boolean; small?: boolean; bold?: boolean; color?: string; italic?: boolean; numberOfLines?: number }> = (
  { children, secondary, small, bold, color, italic, numberOfLines },
) => {
  const { colors } = useTheme();
  const { align } = useLanguage();
  return (
    <Text numberOfLines={numberOfLines} style={{
      color: color ?? (secondary ? colors.textSecondary : colors.text), fontSize: small ? FONT.sm : FONT.md, lineHeight: small ? 18 : 22,
      fontWeight: bold ? '700' : '400', fontStyle: italic ? 'italic' : 'normal', textAlign: align,
    }}>{children}</Text>
  );
};

export const Title: React.FC<{ children: ReactNode; size?: number }> = ({ children, size = FONT.lg }) => {
  const { colors } = useTheme();
  const { align } = useLanguage();
  return <Text style={{ color: colors.text, fontSize: size, fontWeight: '700', textAlign: align }}>{children}</Text>;
};

export const Chip: React.FC<{ label: string; selected?: boolean; onPress?: () => void; icon?: keyof typeof Ionicons.glyphMap; testID?: string }> = (
  { label, selected, onPress, icon, testID },
) => {
  const { colors } = useTheme();
  const styles = useThemedStyles(makeStyles);
  return (
    <TouchableOpacity onPress={onPress} disabled={!onPress} testID={testID} accessibilityRole="button" accessibilityState={{ selected }}
      style={[styles.chip, selected && { backgroundColor: colors.earthContainer, borderColor: colors.earthContainer }]}>
      {icon && <Ionicons name={icon} size={16} color={selected ? colors.onEarthContainer : colors.primary} style={{ marginHorizontal: 4 }} />}
      <Text style={{ color: selected ? colors.onEarthContainer : colors.text, fontSize: FONT.sm, fontWeight: '600' }}>{label}</Text>
    </TouchableOpacity>
  );
};

export const ChipRow: React.FC<{ children: ReactNode }> = ({ children }) => {
  const row = useRow();
  return <View style={[row, { flexWrap: 'wrap', gap: SPACING.sm, marginVertical: SPACING.xs }]}>{children}</View>;
};

export const Pill: React.FC<{ label: string; bg: string; fg: string }> = ({ label, bg, fg }) => (
  <View style={{ backgroundColor: bg, borderRadius: BORDER_RADIUS.round, paddingHorizontal: 10, paddingVertical: 3, alignSelf: 'flex-start' }}>
    <Text style={{ color: fg, fontSize: FONT.xs, fontWeight: '700' }}>{label}</Text>
  </View>
);

export const StatusBadge: React.FC<{ status?: ValidationStatus }> = ({ status = 'to_verify' }) => {
  const { colors } = useTheme();
  const { t } = useLanguage();
  const [bg, fg] = status === 'verified' ? [colors.primaryContainer, colors.onPrimaryContainer]
    : status === 'indicative' ? [colors.earthContainer, colors.onEarthContainer] : [colors.clayContainer, colors.onClayContainer];
  return <Pill label={t(`status_${status}`)} bg={bg} fg={fg} />;
};

export const InfoNote: React.FC<{ text: string; tone?: 'info' | 'error' }> = ({ text, tone = 'info' }) => {
  const { colors } = useTheme();
  const row = useRow();
  return (
    <View style={[row, { gap: SPACING.sm, marginVertical: 3, alignItems: 'flex-start' }]}>
      <Ionicons name={tone === 'error' ? 'warning' : 'information-circle'} size={18} color={tone === 'error' ? colors.error : colors.earth} />
      <View style={{ flex: 1 }}><Body small secondary={tone !== 'error'} color={tone === 'error' ? colors.error : undefined}>{text}</Body></View>
    </View>
  );
};

export const InfoRow: React.FC<{ label: string; value?: string | null }> = ({ label, value }) => {
  const { t } = useLanguage();
  const row = useRow();
  return (
    <View style={[row, { paddingVertical: 6, gap: SPACING.sm }]}>
      <View style={{ flex: 0.45 }}><Body secondary small>{label}</Body></View>
      <View style={{ flex: 0.55 }}><Body small italic={!value}>{value || t('no_data')}</Body></View>
    </View>
  );
};

export const DemoBanner: React.FC<{ text: string; onPress?: () => void }> = ({ text, onPress }) => {
  const { colors } = useTheme();
  const row = useRow();
  return (
    <TouchableOpacity disabled={!onPress} onPress={onPress}
      style={[row, { backgroundColor: colors.clayContainer, borderRadius: BORDER_RADIUS.md, padding: SPACING.md, gap: SPACING.sm, marginVertical: SPACING.xs }]}>
      <Ionicons name="flask" size={20} color={colors.onClayContainer} />
      <View style={{ flex: 1 }}><Body small color={colors.onClayContainer}>{text}</Body></View>
    </TouchableOpacity>
  );
};

export const EmptyState: React.FC<{ icon?: keyof typeof Ionicons.glyphMap; title: string; message?: string; action?: ReactNode }> = (
  { icon = 'leaf-outline', title, message, action },
) => {
  const { colors } = useTheme();
  return (
    <View style={{ alignItems: 'center', padding: SPACING.xl, gap: SPACING.sm }}>
      <Ionicons name={icon} size={48} color={colors.primary} />
      <Text style={{ color: colors.text, fontSize: FONT.lg, fontWeight: '700', textAlign: 'center' }}>{title}</Text>
      {!!message && <Text style={{ color: colors.textSecondary, fontSize: FONT.md, textAlign: 'center' }}>{message}</Text>}
      {action}
    </View>
  );
};

export const Button: React.FC<{ label: string; onPress: () => void; icon?: keyof typeof Ionicons.glyphMap; variant?: 'filled' | 'outline' | 'text'; color?: string; disabled?: boolean; testID?: string }> = (
  { label, onPress, icon, variant = 'filled', color, disabled, testID },
) => {
  const { colors } = useTheme();
  const bg = variant === 'filled' ? color ?? colors.primary : 'transparent';
  const fg = variant === 'filled' ? colors.onPrimary : color ?? colors.primary;
  return (
    <TouchableOpacity onPress={onPress} disabled={disabled} testID={testID} accessibilityRole="button"
      style={{
        flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: SPACING.sm, opacity: disabled ? 0.5 : 1,
        backgroundColor: bg, borderRadius: BORDER_RADIUS.round, paddingVertical: variant === 'text' ? 6 : 12, paddingHorizontal: SPACING.md,
        borderWidth: variant === 'outline' ? 1 : 0, borderColor: colors.divider,
      }}>
      {icon && <Ionicons name={icon} size={18} color={fg} />}
      <Text style={{ color: fg, fontWeight: '700', fontSize: FONT.md }}>{label}</Text>
    </TouchableOpacity>
  );
};

export const Field: React.FC<TextInputProps & { label: string; error?: boolean }> = ({ label, error, style, ...props }) => {
  const { colors } = useTheme();
  const { align } = useLanguage();
  return (
    <View style={{ marginVertical: SPACING.xs }}>
      <Text style={{ color: colors.textSecondary, fontSize: FONT.sm, marginBottom: 4, textAlign: align }}>{label}</Text>
      <TextInput placeholderTextColor={colors.textSecondary} {...props}
        style={[{
          borderWidth: 1, borderColor: error ? colors.error : colors.divider, borderRadius: BORDER_RADIUS.md, color: colors.text,
          paddingHorizontal: SPACING.md, paddingVertical: 10, fontSize: FONT.md, textAlign: align, backgroundColor: colors.surface,
        }, style]} />
    </View>
  );
};

/** Tap-to-choose field opening a list (Android-friendly alternative to a dropdown). */
export function Select<T>({ label, value, options, display, onChange, testID }: {
  label: string; value: T | null; options: T[]; display: (v: T) => string; onChange: (v: T) => void; testID?: string;
}) {
  const { colors } = useTheme();
  const { align, t } = useLanguage();
  const row = useRow();
  const [open, setOpen] = useState(false);
  return (
    <View style={{ marginVertical: SPACING.xs }}>
      <Text style={{ color: colors.textSecondary, fontSize: FONT.sm, marginBottom: 4, textAlign: align }}>{label}</Text>
      <TouchableOpacity onPress={() => setOpen(true)} testID={testID}
        style={[row, { borderWidth: 1, borderColor: colors.divider, borderRadius: BORDER_RADIUS.md, paddingHorizontal: SPACING.md, paddingVertical: 12, alignItems: 'center', backgroundColor: colors.surface }]}>
        <Text style={{ flex: 1, color: colors.text, fontSize: FONT.md, textAlign: align }} numberOfLines={1}>{value != null ? display(value) : '—'}</Text>
        <Ionicons name="chevron-down" size={18} color={colors.textSecondary} />
      </TouchableOpacity>
      <Sheet visible={open} onClose={() => setOpen(false)} title={label}>
        <FlatList data={options} keyExtractor={(_, i) => String(i)} style={{ maxHeight: 420 }}
          renderItem={({ item }) => (
            <TouchableOpacity onPress={() => { onChange(item); setOpen(false); }} style={[row, { paddingVertical: 14, gap: SPACING.sm, alignItems: 'center' }]}>
              <Ionicons name={item === value ? 'radio-button-on' : 'radio-button-off'} size={20} color={colors.primary} />
              <Text style={{ color: colors.text, fontSize: FONT.md, flex: 1, textAlign: align }}>{display(item)}</Text>
            </TouchableOpacity>
          )} />
        <Button variant="text" label={t('close')} onPress={() => setOpen(false)} />
      </Sheet>
    </View>
  );
}

/** Bottom sheet / dialog container. */
export const Sheet: React.FC<{ visible: boolean; onClose: () => void; title?: string; children: ReactNode }> = ({ visible, onClose, title, children }) => {
  const styles = useThemedStyles(makeStyles);
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable style={styles.sheet} onPress={() => undefined}>
          {!!title && <View style={{ marginBottom: SPACING.sm }}><Title>{title}</Title></View>}
          {children}
        </Pressable>
      </Pressable>
    </Modal>
  );
};

export const Divider: React.FC = () => {
  const { colors } = useTheme();
  return <View style={{ height: 1, backgroundColor: colors.border, marginVertical: SPACING.sm }} />;
};

const makeStyles = (c: Palette) => StyleSheet.create({
  sectionHeader: { alignItems: 'center', justifyContent: 'space-between', marginTop: SPACING.md, marginBottom: SPACING.sm },
  sectionTitle: { flex: 1, color: c.text, fontSize: FONT.lg, fontWeight: '700' },
  card: { backgroundColor: c.surface, borderRadius: BORDER_RADIUS.lg, padding: SPACING.md, borderWidth: 1, borderColor: c.border, marginVertical: SPACING.xs },
  chip: { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: c.divider, borderRadius: BORDER_RADIUS.round, paddingHorizontal: 14, paddingVertical: 8, backgroundColor: c.surface },
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: c.background, borderTopLeftRadius: BORDER_RADIUS.xl, borderTopRightRadius: BORDER_RADIUS.xl, padding: SPACING.lg, maxHeight: '90%' },
});

/** Tappable settings / navigation row with an icon, a title and an optional description. */
export const LinkRow: React.FC<{ icon: keyof typeof Ionicons.glyphMap; title: string; subtitle?: string; onPress: () => void; color?: string; testID?: string; right?: ReactNode }> = (
  { icon, title, subtitle, onPress, color, testID, right },
) => {
  const { colors } = useTheme();
  const { isRTL } = useLanguage();
  const row = useRow();
  return (
    <TouchableOpacity onPress={onPress} testID={testID} accessibilityRole="button" style={[row, { alignItems: 'center', gap: SPACING.md, paddingVertical: 12 }]}>
      <Ionicons name={icon} size={22} color={color ?? colors.primary} />
      <View style={{ flex: 1 }}>
        <Body>{title}</Body>
        {!!subtitle && <Body small secondary>{subtitle}</Body>}
      </View>
      {right ?? <Ionicons name={isRTL ? 'chevron-back' : 'chevron-forward'} size={18} color={colors.textSecondary} />}
    </TouchableOpacity>
  );
};

/** Horizontal chip strip; in right-to-left languages it starts from the right edge. */
export const HScroll: React.FC<{ children: ReactNode; padding?: number }> = ({ children, padding = 0 }) => {
  const { isRTL } = useLanguage();
  const ref = React.useRef<ScrollView>(null);
  return (
    <ScrollView ref={ref} horizontal showsHorizontalScrollIndicator={false}
      onContentSizeChange={() => { if (isRTL) ref.current?.scrollToEnd({ animated: false }); }}
      contentContainerStyle={{ flexDirection: isRTL ? 'row-reverse' : 'row', gap: SPACING.sm, paddingVertical: SPACING.xs, paddingHorizontal: padding }}>
      {children}
    </ScrollView>
  );
};
