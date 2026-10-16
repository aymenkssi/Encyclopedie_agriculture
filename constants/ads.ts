/**
 * AdMob configuration (publisher pub-7488746561313974, the same account as the other apps).
 *
 * The App ID is in app.json twice: in the "react-native-google-mobile-ads" plugin (androidAppId)
 * and in the root "react-native-google-mobile-ads" key (read by the library's Gradle script).
 * Android: AgriPedia's real AdMob App ID (ca-app-pub-7488746561313974~5179184724) and banner unit
 * "Bannière bas" below. Development builds (__DEV__) always show Google's test ads; iOS (not
 * published) keeps Google's test IDs. See play-store/README.md.
 * Never click your own real ads: it can get the AdMob account suspended.
 */
export const BANNER_AD_UNIT_IDS = {
  android: 'ca-app-pub-7488746561313974/2730396616',
  ios: '',
};

/** Set to false to turn every ad off without touching the screens. */
export const ADS_ENABLED = true;
