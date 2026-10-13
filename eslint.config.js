// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  expoConfig,
  {
    ignores: ['dist/*', 'play-store/build/*'],
  },
  {
    // Node.js scripts (catalog generation, icons, store images).
    files: ['scripts/**/*.js', 'play-store/scripts/**/*.js'],
    languageOptions: { globals: { __dirname: 'readonly', require: 'readonly', module: 'writable', process: 'readonly', console: 'readonly' } },
  },
]);
