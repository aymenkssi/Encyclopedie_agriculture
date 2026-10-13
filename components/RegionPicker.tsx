import React, { useState } from 'react';
import { FlatList, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { FONT, SPACING } from '../constants/theme';
import { useLanguage } from '../contexts/LanguageContext';
import { useTheme } from '../contexts/ThemeContext';
import { useLabels } from '../hooks/useLabels';
import { nearestRegion } from '../lib/geo';
import { catalog } from '../lib/knowledge';
import { Region } from '../lib/types';
import { Body, Button, Divider, Sheet, useRow } from './ui';

/**
 * Two-step country -> region picker. Geolocation is optional: the approximate position is asked
 * for only when the user taps "use my location", used once on the phone, never stored nor sent.
 */
export const RegionPicker: React.FC<{ visible: boolean; currentRegionId: string | null; onClose: () => void; onPick: (r: Region) => void }> = (
  { visible, currentRegionId, onClose, onPick },
) => {
  const { t, align } = useLanguage();
  const { tr } = useLabels();
  const { colors } = useTheme();
  const row = useRow();
  const [countryId, setCountryId] = useState<string | null>(currentRegionId ? catalog.regionsById[currentRegionId]?.countryId ?? null : null);
  const [message, setMessage] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);

  const pick = (r: Region) => { onPick(r); onClose(); };

  const locate = async () => {
    setMessage(t('location_searching'));
    setLocating(true);
    try {
      const perm = await Location.requestForegroundPermissionsAsync();
      if (!perm.granted) { setMessage(t('location_unavailable')); return; }
      const pos = (await Location.getLastKnownPositionAsync()) ??
        (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Low }));
      const near = nearestRegion(catalog, pos.coords.latitude, pos.coords.longitude);
      if (near) pick(near.region);
      else setMessage(t('location_not_found'));
    } catch {
      setMessage(t('location_unavailable'));
    } finally {
      setLocating(false);
    }
  };

  const regions = countryId ? catalog.countriesById[countryId]?.regions ?? [] : [];
  return (
    <Sheet visible={visible} onClose={onClose} title={t(countryId ? 'pick_region' : 'pick_country')}>
      {countryId ? (
        <>
          <Button variant="text" icon="arrow-back" label={t('back')} onPress={() => setCountryId(null)} />
          <FlatList data={regions} keyExtractor={(r) => r.id} style={{ maxHeight: 420 }} renderItem={({ item }) => (
            <TouchableOpacity onPress={() => pick(item)} style={[row, { paddingVertical: 12, gap: SPACING.sm, alignItems: 'center' }]} testID={`region-${item.id}`}>
              <Ionicons name={item.id === currentRegionId ? 'radio-button-on' : 'radio-button-off'} size={20} color={colors.primary} />
              <View style={{ flex: 1 }}>
                <Text style={{ color: colors.text, fontSize: FONT.md, textAlign: align }}>{tr(item.names)}</Text>
                <Text style={{ color: colors.textSecondary, fontSize: FONT.sm, textAlign: align }}>{tr(catalog.climateZonesById[item.climateZoneId]?.names)}</Text>
              </View>
            </TouchableOpacity>
          )} />
        </>
      ) : (
        <>
          <Button variant="outline" icon="locate" label={t('use_my_location')} onPress={locate} disabled={locating} />
          <View style={{ marginTop: SPACING.sm }}><Body small secondary>{t('location_privacy')}</Body></View>
          {!!message && <Body small color={locating ? colors.textSecondary : colors.error}>{message}</Body>}
          <Divider />
          <FlatList data={catalog.geography.countries} keyExtractor={(c) => c.id} style={{ maxHeight: 380 }} renderItem={({ item }) => (
            <TouchableOpacity onPress={() => setCountryId(item.id)} style={{ paddingVertical: 14 }} testID={`country-${item.id}`}>
              <Text style={{ color: colors.text, fontSize: FONT.md, textAlign: align }}>{tr(item.names)}</Text>
            </TouchableOpacity>
          )} />
        </>
      )}
      <Button variant="text" label={t('close')} onPress={onClose} />
    </Sheet>
  );
};
