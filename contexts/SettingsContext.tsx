import React, { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { catalog } from '../lib/catalogData';
import { EXPERIENCE_LEVELS, ExperienceLevel, FARM_TYPES, FarmType } from '../lib/recommend';
import { ALL_METHODS } from '../lib/types';

export interface Settings {
  onboardingDone: boolean;
  regionId: string | null;
  experience: ExperienceLevel;
  farmType: FarmType;
  preferredMethod: string;
  notificationsEnabled: boolean;
}

export const DEFAULT_SETTINGS: Settings = {
  onboardingDone: false,
  regionId: null,
  experience: 'beginner',
  farmType: 'garden',
  preferredMethod: 'open_field',
  notificationsEnabled: true,
};

const STORAGE_KEY = 'settings';

/** Keeps only valid saved values, so an old or corrupted entry never breaks the app. */
export function sanitizeSettings(raw: unknown): Settings {
  const s = (raw && typeof raw === 'object' ? raw : {}) as Partial<Settings>;
  return {
    onboardingDone: typeof s.onboardingDone === 'boolean' ? s.onboardingDone : DEFAULT_SETTINGS.onboardingDone,
    regionId: typeof s.regionId === 'string' && catalog.regionsById[s.regionId] ? s.regionId : null,
    experience: EXPERIENCE_LEVELS.includes(s.experience as ExperienceLevel) ? s.experience! : DEFAULT_SETTINGS.experience,
    farmType: FARM_TYPES.includes(s.farmType as FarmType) ? s.farmType! : DEFAULT_SETTINGS.farmType,
    preferredMethod: ALL_METHODS.includes(s.preferredMethod as string) ? s.preferredMethod! : DEFAULT_SETTINGS.preferredMethod,
    notificationsEnabled: typeof s.notificationsEnabled === 'boolean' ? s.notificationsEnabled : DEFAULT_SETTINGS.notificationsEnabled,
  };
}

interface SettingsContextType extends Settings {
  loaded: boolean;
  update: (patch: Partial<Settings>) => void;
}

const SettingsContext = createContext<SettingsContextType | undefined>(undefined);

export const SettingsProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((saved) => { if (saved) setSettings(sanitizeSettings(JSON.parse(saved))); })
      .catch((error) => console.error('Error loading settings:', error))
      .finally(() => setLoaded(true));
  }, []);

  const update = useCallback((patch: Partial<Settings>) => {
    setSettings((current) => {
      const next = { ...current, ...patch };
      AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next)).catch((error) => console.error('Error saving settings:', error));
      return next;
    });
  }, []);

  const value = useMemo(() => ({ ...settings, loaded, update }), [settings, loaded, update]);
  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>;
};

export const useSettings = () => {
  const context = useContext(SettingsContext);
  if (!context) throw new Error('useSettings must be used within SettingsProvider');
  return context;
};
