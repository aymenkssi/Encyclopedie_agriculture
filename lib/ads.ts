// Web / fallback implementation: AdMob only exists on Android and iOS.
// The native implementation lives in ads.native.ts.
export type AdsModule = typeof import('react-native-google-mobile-ads');

export function loadAdsModule(): AdsModule | null {
  return null;
}

export function getBannerAdUnitId(ads: AdsModule): string {
  return ads.TestIds.ADAPTIVE_BANNER;
}
