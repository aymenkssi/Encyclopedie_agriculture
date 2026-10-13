import React from 'react';
import { Stack, ThemeProvider as NavigationThemeProvider, DarkTheme, DefaultTheme } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AdsProvider } from '../contexts/AdsContext';
import { LanguageProvider } from '../contexts/LanguageContext';
import { SettingsProvider } from '../contexts/SettingsContext';
import { ThemeProvider, useTheme } from '../contexts/ThemeContext';
import { UserDataProvider } from '../contexts/UserDataContext';

function ThemedStack() {
  const { colors, isDark } = useTheme();
  const base = isDark ? DarkTheme : DefaultTheme;
  // Navigation containers use the palette too, so there is no white flash in dark mode.
  const navigationTheme = {
    ...base,
    colors: { ...base.colors, primary: colors.primary, background: colors.background, card: colors.surface, text: colors.text, border: colors.border },
  };
  return (
    <NavigationThemeProvider value={navigationTheme}>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.background }, animation: 'slide_from_right' }}>
        <Stack.Screen name="index" options={{ animation: 'none' }} />
        <Stack.Screen name="onboarding" options={{ animation: 'fade' }} />
        <Stack.Screen name="(tabs)" options={{ animation: 'fade' }} />
      </Stack>
    </NavigationThemeProvider>
  );
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <ThemeProvider>
        <LanguageProvider>
          <SettingsProvider>
            <UserDataProvider>
              <AdsProvider>
                <ThemedStack />
              </AdsProvider>
            </UserDataProvider>
          </SettingsProvider>
        </LanguageProvider>
      </ThemeProvider>
    </SafeAreaProvider>
  );
}
