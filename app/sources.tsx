import React from 'react';
import { Screen } from '../components/Screen';
import { SourcesList } from '../components/SourcesList';
import { Body, Card, DemoBanner, InfoRow, SectionTitle } from '../components/ui';
import { useLanguage } from '../contexts/LanguageContext';
import { useLabels } from '../hooks/useLabels';
import { catalog } from '../lib/knowledge';

/** Data quality, how recommendations are computed, and every cited source. */
export default function SourcesScreen() {
  const { t } = useLanguage();
  const { tr } = useLabels();
  const regions = catalog.geography.countries.reduce((n, c) => n + c.regions.length, 0);
  return (
    <Screen title={t('sources_and_data')} back>
      {catalog.manifest.demo && <DemoBanner text={tr(catalog.manifest.disclaimer)} />}
      <Card>
        <InfoRow label={t('dataset_version')} value={String(catalog.manifest.version)} />
        <InfoRow label={t('generated_on')} value={catalog.manifest.generatedOn} />
        <InfoRow label={t('plants_count')} value={String(catalog.plants.length)} />
        <InfoRow label={t('regions_count')} value={String(regions)} />
      </Card>
      <SectionTitle title={t('how_data_is_used')} />
      <Card><Body small>{t('how_data_is_used_text')}</Body></Card>
      <SectionTitle title={t('sources')} />
      <SourcesList ids={catalog.reference.sources.map((s) => s.id)} />
    </Screen>
  );
}
