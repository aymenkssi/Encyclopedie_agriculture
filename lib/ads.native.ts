import { Platform } from 'react-native';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import { ADS_ENABLED, BANNER_AD_UNIT_IDS } from '../constants/ads';
import type { AdsModule } from './ads';

export type { AdsModule } from './ads';

// Expo Go does not ship the AdMob native module: ads need a development or store build.
const isExpoGo = Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

/**
 * Loads the AdMob module lazily so that the app still runs (without ads)
 * wherever the native module is missing.
 */
export function loadAdsModule(): AdsModule | null {
  if (!ADS_ENABLED || isExpoGo) return null;
  try {
    // A lazy require (not an import) so a missing native module cannot crash app start-up.
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('react-native-google-mobile-ads');
  } catch (error) {
    console.warn('AdMob is unavailable in this build:', error);
    return null;
  }
}

export function getBannerAdUnitId(ads: AdsModule): string {
  const configured = Platform.OS === 'ios' ? BANNER_AD_UNIT_IDS.ios : BANNER_AD_UNIT_IDS.android;
  return __DEV__ || !configured ? ads.TestIds.ADAPTIVE_BANNER : configured;
}
