import React from 'react';
import { Linking, View } from 'react-native';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { useLabels } from '../hooks/useLabels';
import { catalog } from '../lib/knowledge';
import { Body, Card, Divider, InfoRow, SectionTitle, StatusBadge } from './ui';

/** Cited sources and the meaning of the validation levels. */
export function SourcesList({ ids, lastReviewed }: { ids: string[]; lastReviewed?: string }) {
  const { t } = useLanguage();
  const { tr, date } = useLabels();
  const { colors } = useTheme();
  const sources = ids.map((id) => catalog.sourcesById[id]).filter(Boolean);
  return (
    <>
      {!!lastReviewed && <InfoRow label={t('last_reviewed')} value={/^\d{4}-\d{2}-\d{2}$/.test(lastReviewed) ? date(lastReviewed) : lastReviewed} />}
      {sources.length === 0 && <Body small secondary>{t('no_data')}</Body>}
      {sources.map((s) => (
        <Card key={s.id} onPress={s.url ? () => Linking.openURL(s.url!) : undefined}>
          <Body bold color={s.url ? colors.primary : undefined}>{s.title}</Body>
          {!!s.publisher && <Body small secondary>{s.publisher}</Body>}
          {!!s.license && <Body small secondary>{t('license', { a: s.license })}</Body>}
          <Body small secondary>{s.accessedOn ? t('accessed_on', { a: s.accessedOn }) : t('not_consulted')}</Body>
          {!!tr(s.notes) && <Body small>{tr(s.notes)}</Body>}
        </Card>
      ))}
      <Divider />
      <SectionTitle title={t('validation_levels')} />
      {(['verified', 'indicative', 'to_verify'] as const).map((s) => (
        <View key={s} style={{ marginVertical: 4, gap: 4 }}>
          <StatusBadge status={s} />
          <Body small secondary>{t(`status_${s}_desc`)}</Body>
        </View>
      ))}
    </>
  );
}
