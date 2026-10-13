import React from 'react';
import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLanguage } from '../../contexts/LanguageContext';
import { useTheme } from '../../contexts/ThemeContext';

const TABS = [
  { name: 'home', title: 'tab_home', icon: 'home' },
  { name: 'encyclopedia', title: 'tab_encyclopedia', icon: 'book' },
  { name: 'calendar', title: 'tab_calendar', icon: 'calendar' },
  { name: 'crops', title: 'tab_crops', icon: 'leaf' },
  { name: 'profile', title: 'tab_profile', icon: 'person' },
] as const;

export default function TabsLayout() {
  const { colors } = useTheme();
  const { t } = useLanguage();
  // Keep the tab bar above the Android navigation bar.
  const insets = useSafeAreaInsets();
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textSecondary,
        tabBarStyle: {
          backgroundColor: colors.surface, borderTopColor: colors.border, borderTopWidth: 1,
          height: 64 + insets.bottom, paddingBottom: 8 + insets.bottom, paddingTop: 6,
        },
        tabBarLabelStyle: { fontSize: 10, fontWeight: '600' },
      }}
    >
      {TABS.map((tab) => (
        <Tabs.Screen key={tab.name} name={tab.name} options={{
          title: t(tab.title),
          tabBarButtonTestID: `tab-${tab.name}`,
          tabBarIcon: ({ color, size, focused }) => (
            <Ionicons name={(focused ? tab.icon : `${tab.icon}-outline`) as keyof typeof Ionicons.glyphMap} size={size} color={color} />
          ),
        }} />
      ))}
    </Tabs>
  );
}
