import React, { useEffect, useRef, useState } from 'react';
import { AppState, Platform, StyleSheet, View } from 'react-native';
import type { BannerAd as BannerAdType } from 'react-native-google-mobile-ads';
import { useAds } from '../contexts/AdsContext';
import { getBannerAdUnitId } from '../lib/ads';
import { Palette } from '../constants/theme';
import { useThemedStyles } from '../contexts/ThemeContext';

/**
 * Anchored adaptive AdMob banner, meant to sit at the bottom of a screen,
 * just above the tab bar. Renders nothing when ads are unavailable or not allowed.
 */
export const AdBanner: React.FC = () => {
  const styles = useThemedStyles(makeStyles);
  const { ads, canShowAds } = useAds();
  const [failed, setFailed] = useState(false);
  const bannerRef = useRef<BannerAdType | null>(null);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (state) => {
      if (state !== 'active') return;
      // Give a banner that failed (e.g. no network) another chance when the app comes back.
      setFailed(false);
      // iOS can drop the banner's web view while the app is in the background.
      if (Platform.OS === 'ios') bannerRef.current?.load();
    });
    return () => subscription.remove();
  }, []);

  if (!ads || !canShowAds || failed) return null;

  const { BannerAd, BannerAdSize } = ads;
  return (
    <View style={styles.container}>
      <BannerAd
        ref={bannerRef}
        unitId={getBannerAdUnitId(ads)}
        size={BannerAdSize.ANCHORED_ADAPTIVE_BANNER}
        onAdFailedToLoad={(error) => {
          console.warn('Banner ad failed to load:', error);
          setFailed(true);
        }}
      />
    </View>
  );
};

const makeStyles = (c: Palette) =>
  StyleSheet.create({
  container: {
    alignItems: 'center',
    backgroundColor: c.surface,
    borderTopWidth: 1,
    borderTopColor: c.border,
  },
});
