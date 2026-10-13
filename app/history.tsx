import React from 'react';
import { router } from 'expo-router';
import { PlantRow } from '../components/PlantRow';
import { Screen } from '../components/Screen';
import { HeaderIcon } from '../components/ScreenHeader';
import { EmptyState, SectionTitle } from '../components/ui';
import { useLanguage } from '../contexts/LanguageContext';
import { useUserData } from '../contexts/UserDataContext';
import { useLabels } from '../hooks/useLabels';
import { catalog } from '../lib/knowledge';
import { alive } from '../lib/userData';

export default function HistoryScreen() {
  const { t } = useLanguage();
  const { date, label } = useLabels();
  const { history, clearHistory, data } = useUserData();
  const viewed = history.map((id) => catalog.plantsById[id]).filter(Boolean);
  const finished = alive(data.crops).filter((c) => c.stage === 'finished').sort((a, b) => (b.harvestedOn ?? '').localeCompare(a.harvestedOn ?? ''));
  return (
    <Screen title={t('history')} back right={viewed.length ? <HeaderIcon icon="trash-outline" label={t('clear_history')} onPress={clearHistory} testID="clear-history" /> : undefined}>
      {finished.length > 0 && (
        <>
          <SectionTitle title={t('history_crops')} />
          {finished.map((c) => {
            const plant = catalog.plantsById[c.plantId];
            return plant ? (
              <PlantRow key={c.id} plant={plant} onPress={() => router.push({ pathname: '/crop/[id]', params: { id: c.id } })}
                subtitle={`${label('act', c.startActivity)} ${date(c.startDate)}${c.harvestedOn ? ` → ${date(c.harvestedOn)}` : ''}`} />
            ) : null;
          })}
          <SectionTitle title={t('recently_viewed')} />
        </>
      )}
      {viewed.length === 0 ? <EmptyState icon="time-outline" title={t('no_history_title')} message={t('no_history_text')} />
        : viewed.map((p) => <PlantRow key={p.id} plant={p} onPress={() => router.push({ pathname: '/plant/[id]', params: { id: p.id } })} />)}
    </Screen>
  );
}
