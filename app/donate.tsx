import React from 'react';
import { Linking, Share, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Screen } from '../components/Screen';
import { Body, Card, Divider, LinkRow, SectionTitle, useRow } from '../components/ui';
import { CONTACT_EMAIL, DONATION_URL, PLAY_STORE_URL } from '../constants/links';
import { BORDER_RADIUS, FONT, SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';

const open = (url: string) => Linking.openURL(url).catch((error) => console.warn('Cannot open link:', error));

const REASONS = [
  { icon: 'gift-outline', key: 'donate_reason_free' },
  { icon: 'checkmark-done-outline', key: 'donate_reason_data' },
  { icon: 'language-outline', key: 'donate_reason_languages' },
] as const;

/** Donation page (PayPal) and other ways to help. Deliberately ad-free. */
export default function DonateScreen() {
  const { colors } = useTheme();
  const { t } = useLanguage();
  const row = useRow();
  return (
    <Screen title={t('support_title')} back ads={false}>
      <View style={{ alignItems: 'center', gap: SPACING.sm, backgroundColor: colors.primary, borderRadius: BORDER_RADIUS.xl, padding: SPACING.lg }}>
        <Ionicons name="heart" size={44} color={colors.onPrimary} />
        <Text style={{ color: colors.onPrimary, fontSize: FONT.xl, fontWeight: '700', textAlign: 'center' }}>{t('donate_title')}</Text>
        <Text style={{ color: colors.onPrimary, fontSize: FONT.md, lineHeight: 22, textAlign: 'center' }}>{t('donate_message')}</Text>
      </View>
      <Card style={{ marginTop: SPACING.md, gap: SPACING.md }}>
        {REASONS.map((r) => (
          <View key={r.key} style={[row, { alignItems: 'center', gap: SPACING.md }]}>
            <Ionicons name={r.icon} size={22} color={colors.primary} />
            <View style={{ flex: 1 }}><Body>{t(r.key)}</Body></View>
          </View>
        ))}
        <TouchableOpacity onPress={() => open(DONATION_URL)} accessibilityRole="link" testID="donate-paypal"
          style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: SPACING.sm, backgroundColor: colors.paypal, paddingVertical: 14, borderRadius: BORDER_RADIUS.round }}>
          <Ionicons name="logo-paypal" size={22} color={colors.onPaypal} />
          <Text style={{ color: colors.onPaypal, fontSize: FONT.lg, fontWeight: '700' }}>{t('donate_paypal')}</Text>
        </TouchableOpacity>
        <Text style={{ color: colors.textSecondary, fontSize: FONT.xs, textAlign: 'center' }}>{t('donate_paypal_hint')}</Text>
      </Card>
      <SectionTitle title={t('other_ways')} />
      <Card>
        <LinkRow icon="star-outline" title={t('rate_app')} onPress={() => open(PLAY_STORE_URL)} />
        <Divider />
        <LinkRow icon="share-social-outline" title={t('share_app')}
          onPress={() => Share.share({ message: t('share_app_message', { a: PLAY_STORE_URL }) }).catch(() => undefined)} />
        <Divider />
        <LinkRow icon="mail-outline" title={t('contribute_data')} subtitle={t('contribute_data_hint')} onPress={() => open(`mailto:${CONTACT_EMAIL}?subject=AgriPedia`)} />
      </Card>
    </Screen>
  );
}
