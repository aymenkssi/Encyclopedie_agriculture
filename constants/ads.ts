/**
 * AdMob configuration (publisher pub-7488746561313974, the same account as the other apps).
 *
 * The App ID is in app.json twice: in the "react-native-google-mobile-ads" plugin (androidAppId)
 * and in the root "react-native-google-mobile-ads" key (read by the library's Gradle script).
 * Until AgriPedia is registered in AdMob, app.json holds Google's TEST App ID and the banner below
 * is empty: Google's test ads are shown. See play-store/README.md, step 0.
 * Never click your own real ads: it can get the AdMob account suspended.
 */
export const BANNER_AD_UNIT_IDS = {
  android: '', // e.g. 'ca-app-pub-7488746561313974/XXXXXXXXXX'
  ios: '',
};

/** Set to false to turn every ad off without touching the screens. */
export const ADS_ENABLED = true;
