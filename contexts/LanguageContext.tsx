import React, { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { getLocales } from 'expo-localization';
import { Platform } from 'react-native';
import { translations } from '../constants/translations';
import { Language, LANGUAGES } from '../lib/types';

export type { Language } from '../lib/types';

type Params = Record<string, string | number>;

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => Promise<void>;
  /** Translated text; `{a}`, `{b}`, `{n}` placeholders are replaced by `params`. */
  t: (key: string, params?: Params) => string;
  /** Plural form of `key` for `count` (also available as `{n}`). */
  tn: (key: string, count: number, params?: Params) => string;
  isRTL: boolean;
  /** Text alignment for the current language. */
  align: 'left' | 'right';
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

/** CLDR plural category (French counts 0 and 1 as "one"; Arabic has six forms). */
export function pluralCategory(lang: Language, n: number): string {
  if (lang === 'fr') return n === 0 || n === 1 ? 'one' : 'other';
  if (lang === 'en') return n === 1 ? 'one' : 'other';
  if (n === 0) return 'zero';
  if (n === 1) return 'one';
  if (n === 2) return 'two';
  const r = n % 100;
  if (r >= 3 && r <= 10) return 'few';
  if (r >= 11 && r <= 99) return 'many';
  return 'other';
}

export function translate(lang: Language, key: string, params?: Params): string {
  const text = translations[lang][key] ?? translations.en[key] ?? key;
  return params ? text.replace(/\{(\w+)\}/g, (match, name) => (params[name] !== undefined ? String(params[name]) : match)) : text;
}

const isLanguage = (value: unknown): value is Language => typeof value === 'string' && (LANGUAGES as string[]).includes(value);

/** First device language the app supports, falling back to French. */
const getDeviceLanguage = (): Language => {
  try {
    for (const locale of getLocales()) if (isLanguage(locale.languageCode)) return locale.languageCode;
  } catch (error) {
    console.error('Error reading device locale:', error);
  }
  return 'fr';
};

export const LanguageProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  // Web pages are pre-rendered: start from a fixed language and apply the browser one after hydration.
  const [language, setLanguageState] = useState<Language>(() => (Platform.OS === 'web' ? 'fr' : getDeviceLanguage()));

  useEffect(() => {
    AsyncStorage.getItem('app_language')
      .then((saved) => {
        if (isLanguage(saved)) setLanguageState(saved);
        else if (Platform.OS === 'web') setLanguageState(getDeviceLanguage());
      })
      .catch((error) => console.error('Error loading language:', error));
  }, []);

  const setLanguage = useCallback(async (lang: Language) => {
    setLanguageState(lang);
    try {
      await AsyncStorage.setItem('app_language', lang);
    } catch (error) {
      console.error('Error saving language:', error);
    }
  }, []);

  const t = useCallback((key: string, params?: Params) => translate(language, key, params), [language]);
  const tn = useCallback(
    (key: string, count: number, params?: Params) => {
      const form = `${key}__${pluralCategory(language, count)}`;
      return translate(language, translations[language][form] ? form : `${key}__other`, { n: count, ...params });
    },
    [language]
  );

  const value = useMemo(
    () => ({ language, setLanguage, t, tn, isRTL: language === 'ar', align: (language === 'ar' ? 'right' : 'left') as 'left' | 'right' }),
    [language, setLanguage, t, tn]
  );
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
};

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (!context) throw new Error('useLanguage must be used within LanguageProvider');
  return context;
};
