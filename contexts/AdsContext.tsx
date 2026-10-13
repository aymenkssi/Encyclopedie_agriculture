import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  ReactNode,
} from 'react';
import { loadAdsModule, AdsModule } from '../lib/ads';

interface AdsContextType {
  /** The AdMob module, or null where ads are not available (web, Expo Go, disabled). */
  ads: AdsModule | null;
  /** True once consent has been handled and the SDK is initialized. */
  canShowAds: boolean;
  /** True when the user must be offered a way to change their consent (e.g. GDPR). */
  privacyOptionsRequired: boolean;
  showPrivacyOptions: () => Promise<void>;
}

const AdsContext = createContext<AdsContextType | undefined>(undefined);

export const AdsProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [ads] = useState<AdsModule | null>(loadAdsModule);
  const [canShowAds, setCanShowAds] = useState(false);
  const [privacyOptionsRequired, setPrivacyOptionsRequired] = useState(false);
  const startedRef = useRef(false);

  const refreshConsentState = useCallback(async () => {
    if (!ads) return;
    const info = await ads.AdsConsent.getConsentInfo();
    setPrivacyOptionsRequired(
      info.privacyOptionsRequirementStatus ===
        ads.AdsConsentPrivacyOptionsRequirementStatus.REQUIRED
    );
    return info;
  }, [ads]);

  // Start the SDK only when Google's consent flow (UMP) allows requesting ads.
  const startSdk = useCallback(async () => {
    if (!ads || startedRef.current) return;
    const info = await refreshConsentState();
    if (!info?.canRequestAds || startedRef.current) return;
    startedRef.current = true;
    await ads.default().initialize();
    setCanShowAds(true);
  }, [ads, refreshConsentState]);

  useEffect(() => {
    if (!ads) return;
    // Shows the consent form when required (EEA, UK, Switzerland...), then starts the SDK.
    ads.AdsConsent.gatherConsent()
      .then(startSdk)
      .catch((error) => console.warn('AdMob consent gathering failed:', error));
    // Also try right away with the consent obtained in a previous session.
    startSdk().catch((error) => console.warn('AdMob initialization failed:', error));
  }, [ads, startSdk]);

  const showPrivacyOptions = useCallback(async () => {
    if (!ads) return;
    try {
      await ads.AdsConsent.showPrivacyOptionsForm();
      await startSdk();
    } catch (error) {
      console.warn('Could not show the AdMob privacy options:', error);
    }
  }, [ads, startSdk]);

  const value = useMemo(
    () => ({ ads, canShowAds, privacyOptionsRequired, showPrivacyOptions }),
    [ads, canShowAds, privacyOptionsRequired, showPrivacyOptions]
  );

  return <AdsContext.Provider value={value}>{children}</AdsContext.Provider>;
};

export const useAds = () => {
  const context = useContext(AdsContext);
  if (!context) {
    throw new Error('useAds must be used within AdsProvider');
  }
  return context;
};
