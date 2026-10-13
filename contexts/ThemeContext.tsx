import React, { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';
import { Platform, useColorScheme } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { DARK_COLORS, LIGHT_COLORS, Palette } from '../constants/theme';

export type ThemePreference = 'system' | 'light' | 'dark';

const STORAGE_KEY = 'theme_preference';
const PREFERENCES: ThemePreference[] = ['system', 'light', 'dark'];

interface ThemeContextType {
  colors: Palette;
  isDark: boolean;
  preference: ThemePreference;
  setPreference: (preference: ThemePreference) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const systemScheme = useColorScheme();
  const [preference, setPreferenceState] = useState<ThemePreference>('system');
  // Web pages are pre-rendered in light mode: apply the real scheme only once hydrated.
  const [hydrated, setHydrated] = useState(Platform.OS !== 'web');

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((saved) => {
        if (saved && (PREFERENCES as string[]).includes(saved)) setPreferenceState(saved as ThemePreference);
      })
      .catch((error) => console.error('Error loading theme:', error))
      .finally(() => setHydrated(true));
  }, []);

  const setPreference = useCallback((next: ThemePreference) => {
    setPreferenceState(next);
    AsyncStorage.setItem(STORAGE_KEY, next).catch((error) => console.error('Error saving theme:', error));
  }, []);

  const isDark = hydrated && (preference === 'dark' || (preference === 'system' && systemScheme === 'dark'));

  const value = useMemo(
    () => ({ colors: isDark ? DARK_COLORS : LIGHT_COLORS, isDark, preference, setPreference }),
    [isDark, preference, setPreference]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
};

/** Builds a StyleSheet from the active palette, recomputed only when the theme changes. */
export function useThemedStyles<T>(factory: (colors: Palette) => T): T {
  const { colors } = useTheme();
  return useMemo(() => factory(colors), [factory, colors]);
}
