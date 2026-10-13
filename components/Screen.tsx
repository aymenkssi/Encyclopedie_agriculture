import React, { ReactNode } from 'react';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { SPACING } from '../constants/theme';
import { useTheme } from '../contexts/ThemeContext';
import { AdBanner } from './AdBanner';
import { ScreenHeader } from './ScreenHeader';

/** Standard page: header, scrollable content and the ad banner at the bottom. */
export const Screen: React.FC<{
  title: string; back?: boolean; right?: ReactNode; children: ReactNode; scroll?: boolean; ads?: boolean; fab?: ReactNode; testID?: string;
}> = ({ title, back, right, children, scroll = true, ads = true, fab, testID }) => {
  const { colors } = useTheme();
  return (
    <SafeAreaView edges={back ? ['top', 'bottom'] : ['top']} style={{ flex: 1, backgroundColor: colors.background }} testID={testID}>
      <ScreenHeader title={title} back={back} right={right} />
      <View style={{ flex: 1 }}>
        {scroll ? (
          <ScrollView contentContainerStyle={{ padding: SPACING.md, paddingBottom: fab ? 96 : SPACING.xl }} keyboardShouldPersistTaps="handled">
            {children}
          </ScrollView>
        ) : children}
        {fab}
      </View>
      {ads && <AdBanner />}
    </SafeAreaView>
  );
};
