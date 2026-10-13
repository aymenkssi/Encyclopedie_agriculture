import React from 'react';
import { ActivityIndicator, View } from 'react-native';
import { Redirect } from 'expo-router';
import { useSettings } from '../contexts/SettingsContext';
import { useTheme } from '../contexts/ThemeContext';

/** Entry: onboarding on first launch, then the tabs. */
export default function Index() {
  const { loaded, onboardingDone } = useSettings();
  const { colors } = useTheme();
  if (!loaded) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background }}>
        <ActivityIndicator color={colors.primary} size="large" />
      </View>
    );
  }
  return <Redirect href={onboardingDone ? '/(tabs)/home' : '/onboarding'} />;
}
