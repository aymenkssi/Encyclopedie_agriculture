import { LocalizedNames, LocalizedText } from './types';

/** Localized text with fallback: requested language -> en -> fr -> first available. */
export function pick(text: LocalizedText | undefined, lang: string): string {
  if (!text) return '';
  return text[lang] ?? text.en ?? text.fr ?? Object.values(text).find((v) => !!v) ?? '';
}

export function pickNames(names: LocalizedNames | undefined, lang: string): string[] {
  if (!names) return [];
  for (const key of [lang, 'en', 'fr']) {
    const list = names[key];
    if (list && list.length) return list;
  }
  return Object.values(names).find((l) => l && l.length) ?? [];
}
