# Consignes pour Claude

- Répondre en français.
- Ne jamais pousser directement sur `main` : travailler sur une branche, puis **ouvrir la pull request vers `main` sans demander** (ou mettre à jour la PR ouverte de la branche). Si la PR précédente a été fusionnée, repartir de `main` à jour.

## Vérifications avant chaque push

```bash
yarn catalog                                  # si data/seed/ a changé (régénère lib/catalogData.ts)
yarn typecheck && yarn lint && yarn test
npx expo export --platform web                # tous les écrans se construisent
```

La CI GitHub Actions (`ci.yml`) refait ces vérifications puis compile l'APK Android (`expo prebuild` + Gradle) ;
vérifier qu'elle est verte avant de considérer le travail fini.

## Points d'attention

- Application **Expo / React Native** (SDK 57, TypeScript, expo-router), même structure que Halal & Mosque Finder.
  APK et .aab : `npx eas-cli build --platform android --profile preview` (APK) / `--profile production` (.aab).
- La logique sans interface va dans `lib/` avec ses tests Jest (`__tests__/`).
- Textes de l'interface : `constants/translations.ts` (fr, en, ar, mêmes clés et mêmes paramètres ; un test le vérifie,
  ainsi que l'existence de chaque clé utilisée par `t('…')` / `tn('…')`).
- Contenu agricole : `data/seed/` (une plante = un fichier JSON), puis `yarn catalog`. Ne jamais inventer de valeurs
  agronomiques ni marquer « verified » sans source ; incrémenter `manifest.json.version` à chaque changement.
- Couleurs : via `useTheme().colors` (mode sombre), jamais codées en dur dans les écrans.
- Arabe : mise en page RTL manuelle (`useRow()`, `align`, `HScroll`), comme dans Halal & Mosque Finder.
- Pas de `Intl` ni de `String.normalize` (moteur Hermes) : mois, pluriels et accents passent par des tables.
- Publication Google Play et AdMob : voir `play-store/README.md`. Site (confidentialité, app-ads.txt) : `site/` et `deploy/`.
- Avant chaque build envoyé à Google Play : augmenter `android.versionCode` dans `app.json`.

## Liens du développeur

- Don PayPal : https://www.paypal.com/donate/?hosted_button_id=LW7K92GXSH38U (`DONATION_URL` dans `constants/links.ts`)
- Compte AdMob (éditeur) : `pub-7488746561313974` — ligne app-ads.txt : `google.com, pub-7488746561313974, DIRECT, f08c47fec0942fa0`
- Site : https://agripedia.creationapp.academy (`WEBSITE_URL`)
- Contact : aymenkssi@gmail.com
