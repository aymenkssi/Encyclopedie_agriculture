/**
 * Colour tokens. Screens never hard-code colours: they read the active palette from
 * ThemeContext (useTheme / useThemedStyles), so light and dark modes stay in sync.
 * Nature palette: plant green, dark green, natural beige, earth brown, clay.
 */
export const LIGHT_COLORS = {
  primary: '#2E6B3A',
  primaryDark: '#1F4E29',
  primaryContainer: '#C9EBC8',
  onPrimary: '#FFFFFF',
  onPrimaryContainer: '#0E3A18',

  earth: '#6B5B3E',
  earthContainer: '#F4DFBA',
  onEarthContainer: '#3A2F14',

  clay: '#9A4A2B',
  clayContainer: '#FFDBCF',
  onClayContainer: '#3A0B00',

  background: '#FBF8F1',
  surface: '#FFFFFF',
  surfaceAlt: '#F1ECE0',
  chip: '#EDE6D6',

  text: '#1B1C18',
  textSecondary: '#55594F',
  textLight: '#FFFFFF',

  success: '#2E7D32',
  warning: '#B26A00',
  error: '#B3261E',
  errorContainer: '#F9DEDC',
  onErrorContainer: '#410E0B',

  paypal: '#0070BA',
  onPaypal: '#FFFFFF',

  border: '#E3DDD0',
  divider: '#CFC8B8',
  monthOff: '#E8E2D4',
};

export type Palette = typeof LIGHT_COLORS;

export const DARK_COLORS: Palette = {
  primary: '#9CD49E',
  primaryDark: '#7DBF80',
  primaryContainer: '#1D4A26',
  onPrimary: '#00390F',
  onPrimaryContainer: '#C9EBC8',

  earth: '#D8C39E',
  earthContainer: '#524428',
  onEarthContainer: '#F4DFBA',

  clay: '#FFB59C',
  clayContainer: '#7B3317',
  onClayContainer: '#FFDBCF',

  background: '#121612',
  surface: '#1A1E19',
  surfaceAlt: '#242923',
  chip: '#2C322B',

  text: '#E6E7E0',
  textSecondary: '#B4B9AD',
  textLight: '#FFFFFF',

  success: '#81C784',
  warning: '#FFB74D',
  error: '#F2B8B5',
  errorContainer: '#601410',
  onErrorContainer: '#F9DEDC',

  paypal: '#0070BA',
  onPaypal: '#FFFFFF',

  border: '#2F362E',
  divider: '#3E463D',
  monthOff: '#2C322B',
};

export const SPACING = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32, xxl: 48 };
export const BORDER_RADIUS = { sm: 6, md: 10, lg: 16, xl: 22, round: 999 };
export const FONT = { xs: 11, sm: 13, md: 15, lg: 17, xl: 20, xxl: 24 };
