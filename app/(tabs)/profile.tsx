import React, { useState } from 'react';
import { Linking, Share, Switch, View } from 'react-native';
import { router } from 'expo-router';
import Constants from 'expo-constants';
import { ConfirmSheet } from '../../components/ConfirmSheet';
import { RegionPicker } from '../../components/RegionPicker';
import { Screen } from '../../components/Screen';
import { Body, Card, Chip, ChipRow, Divider, InfoNote, LinkRow, Select, SectionTitle, useRow } from '../../components/ui';
import { CONTACT_EMAIL, PLAY_STORE_URL, PRIVACY_POLICY_URL } from '../../constants/links';
import { SPACING } from '../../constants/theme';
import { useAds } from '../../contexts/AdsContext';
import { Language, useLanguage } from '../../contexts/LanguageContext';
import { useSettings } from '../../contexts/SettingsContext';
import { ThemePreference, useTheme } from '../../contexts/ThemeContext';
import { useUserData } from '../../contexts/UserDataContext';
import { useLabels } from '../../hooks/useLabels';
import { pickBackup, shareBackup } from '../../lib/backupIo';
import { today } from '../../lib/dates';
import { catalog } from '../../lib/knowledge';
import { ensureNotificationPermission } from '../../lib/notify';
import { defaultMethodFor, EXPERIENCE_LEVELS, FARM_TYPES } from '../../lib/recommend';
import { ALL_METHODS } from '../../lib/types';

const LANGUAGE_NAMES: [Language, string][] = [['fr', 'Français'], ['ar', 'العربية'], ['en', 'English']];
const THEMES: ThemePreference[] = ['system', 'light', 'dark'];
const open = (url: string) => Linking.openURL(url).catch((error) => console.warn('Cannot open link:', error));

export default function Profile() {
  const { colors, preference, setPreference } = useTheme();
  const { t, language, setLanguage } = useLanguage();
  const { label, tr } = useLabels();
  const settings = useSettings();
  const { exportBackup, importBackup } = useUserData();
  const { privacyOptionsRequired, showPrivacyOptions } = useAds();
  const row = useRow();
  const [picker, setPicker] = useState(false);
  const [message, setMessage] = useState<{ text: string; error?: boolean } | null>(null);
  const [pendingImport, setPendingImport] = useState<string | null>(null);
  const region = settings.regionId ? catalog.regionsById[settings.regionId] : null;

  const doExport = async () => {
    try {
      await shareBackup(exportBackup(), `agripedia-backup-${today()}.json`);
      setMessage({ text: t('backup_exported') });
    } catch (error) {
      console.warn('Backup export failed:', error);
      setMessage({ text: t('backup_failed'), error: true });
    }
  };
  const doPick = async () => {
    try {
      const json = await pickBackup();
      if (json != null) setPendingImport(json);
    } catch (error) {
      console.warn('Backup import failed:', error);
      setMessage({ text: t('backup_failed'), error: true });
    }
  };
  const doImport = () => {
    const json = pendingImport;
    setPendingImport(null);
    try {
      setMessage({ text: t('backup_restored', { n: importBackup(json ?? '') }) });
    } catch {
      setMessage({ text: t('backup_failed'), error: true });
    }
  };
  const toggleNotifications = async (on: boolean) => {
    if (on) await ensureNotificationPermission();
    settings.update({ notificationsEnabled: on });
  };

  return (
    <Screen title={t('my_profile')}>
      <InfoNote text={t('no_account_needed')} />
      <SectionTitle title={t('my_space')} />
      <Card>
        <LinkRow icon="location-outline" title={t('region')} testID="profile-region" onPress={() => setPicker(true)}
          subtitle={region ? `${tr(region.names)} · ${tr(catalog.countriesById[region.countryId]?.names)}` : t('no_region')} />
        <Select label={t('experience')} value={settings.experience} options={EXPERIENCE_LEVELS} display={(e) => label('exp', e)}
          onChange={(experience) => settings.update({ experience })} />
        <Select label={t('farm_type')} value={settings.farmType} options={FARM_TYPES} display={(f) => label('farm', f)}
          onChange={(farmType) => settings.update({ farmType, preferredMethod: defaultMethodFor(farmType) })} />
        <Select label={t('preferred_method')} value={settings.preferredMethod} options={ALL_METHODS}
          display={(m) => tr(catalog.methodsById[m]?.names) || m} onChange={(preferredMethod) => settings.update({ preferredMethod })} />
      </Card>

      <SectionTitle title={t('settings')} />
      <Card>
        <Body secondary small>{t('language')}</Body>
        <ChipRow>
          {LANGUAGE_NAMES.map(([code, name]) => (
            <Chip key={code} label={name} selected={language === code} onPress={() => setLanguage(code)} testID={`profile-lang-${code}`} />
          ))}
        </ChipRow>
        <Body secondary small>{t('theme')}</Body>
        <ChipRow>
          {THEMES.map((th) => <Chip key={th} label={t(`theme_${th}`)} selected={preference === th} onPress={() => setPreference(th)} testID={`theme-${th}`} />)}
        </ChipRow>
        <View style={[row, { alignItems: 'center', justifyContent: 'space-between', gap: SPACING.sm, marginTop: SPACING.sm }]}>
          <View style={{ flex: 1 }}><Body>{t('notifications')}</Body><Body small secondary>{t('notifications_hint')}</Body></View>
          <Switch value={settings.notificationsEnabled} onValueChange={toggleNotifications} trackColor={{ true: colors.primary }} testID="notifications" />
        </View>
      </Card>

      <Card>
        <LinkRow icon="document-text-outline" title={t('my_notes')} onPress={() => router.push('/notes')} testID="profile-notes" />
        <Divider />
        <LinkRow icon="time-outline" title={t('history')} onPress={() => router.push('/history')} testID="profile-history" />
        <Divider />
        <LinkRow icon="library-outline" title={t('sources_and_data')} onPress={() => router.push('/sources')} testID="profile-sources" />
      </Card>

      <SectionTitle title={t('backup')} />
      <Card>
        <Body small secondary>{t('backup_hint')}</Body>
        <LinkRow icon="cloud-upload-outline" title={t('export')} onPress={doExport} testID="backup-export" />
        <Divider />
        <LinkRow icon="cloud-download-outline" title={t('import_')} onPress={doPick} testID="backup-import" />
        {message && <Body small color={message.error ? colors.error : colors.primary}>{message.text}</Body>}
      </Card>

      <SectionTitle title={t('support_title')} />
      <Card>
        <LinkRow icon="heart" color={colors.clay} title={t('support_title')} subtitle={t('support_home_hint')} onPress={() => router.push('/donate')} testID="profile-donate" />
        <Divider />
        <LinkRow icon="share-social-outline" title={t('share_app')}
          onPress={() => Share.share({ message: t('share_app_message', { a: PLAY_STORE_URL }) }).catch(() => undefined)} />
        <Divider />
        <LinkRow icon="star-outline" title={t('rate_app')} onPress={() => open(PLAY_STORE_URL)} />
        <Divider />
        <LinkRow icon="mail-outline" title={t('contribute_data')} subtitle={t('contribute_data_hint')}
          onPress={() => open(`mailto:${CONTACT_EMAIL}?subject=AgriPedia`)} />
      </Card>

      <SectionTitle title={t('privacy')} />
      <Card>
        <Body small>{t('privacy_text')}</Body>
        <LinkRow icon="shield-checkmark-outline" title={t('privacy_policy')} onPress={() => open(PRIVACY_POLICY_URL)} testID="privacy-policy" />
        {privacyOptionsRequired && (
          <>
            <Divider />
            <LinkRow icon="options-outline" title={t('ad_privacy_options')} onPress={() => showPrivacyOptions()} />
          </>
        )}
      </Card>

      <View style={{ marginTop: SPACING.md, gap: 2 }}>
        <Body small secondary>{`${t('app_version')} : ${Constants.expoConfig?.version ?? '—'}`}</Body>
        <Body small secondary>{`${t('dataset_version')} : ${catalog.manifest.version} (${catalog.manifest.generatedOn})`}</Body>
      </View>

      <RegionPicker visible={picker} currentRegionId={settings.regionId} onClose={() => setPicker(false)} onPick={(r) => settings.update({ regionId: r.id })} />
      <ConfirmSheet visible={pendingImport != null} title={t('import_')} message={t('import_confirm')} confirmLabel={t('import_')} destructive={false}
        onCancel={() => setPendingImport(null)} onConfirm={doImport} />
    </Screen>
  );
}
