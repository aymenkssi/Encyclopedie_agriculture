# Consignes pour Claude

- Répondre en français.
- Ne jamais pousser directement sur `main` : travailler sur une branche, puis **ouvrir la pull request vers `main` sans demander** (ou mettre à jour la PR ouverte de la branche). Si la PR précédente a été fusionnée, repartir de `main` à jour.

## Vérifications avant chaque push

```bash
./gradlew :core:test                      # sans SDK Android : jeu de données + moteurs
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug   # avec SDK Android
```

Sans SDK Android, seul `:core` est compilé : la CI GitHub Actions (`android.yml`) compile l'app, lance lint et les tests
sur émulateur ; vérifier qu'elle est verte avant de considérer le travail fini.

## Points d'attention

- Application Android native : Kotlin, Jetpack Compose, Material 3, Room. La logique sans interface va dans `core/` avec ses tests.
- Textes de l'interface : `tools/strings_source.py` (fr, en, ar), puis `python3 tools/generate_strings.py`. Ne jamais éditer les `strings.xml` à la main (un test vérifie que les trois langues ont les mêmes clés).
- Contenu agricole : `data/seed/` (une plante = un fichier JSON). Ne jamais inventer de valeurs agronomiques ni marquer « verified » sans source ; incrémenter `manifest.json.version` à chaque changement.
- Couleurs : via `MaterialTheme.colorScheme` (mode sombre), jamais codées en dur dans les écrans.
- Navigation après un travail asynchrone : revenir sur le fil principal (`withContext(Dispatchers.Main.immediate)`).
- Publication Google Play et AdMob : voir `play-store/README.md`. Site (confidentialité, app-ads.txt) : `site/` et `deploy/`.
- Avant chaque build envoyé à Google Play : augmenter `versionCode` dans `app/build.gradle.kts`.

## Liens du développeur

- Don PayPal : https://www.paypal.com/donate/?hosted_button_id=LW7K92GXSH38U (`Links.DONATION_URL`)
- Compte AdMob (éditeur) : `pub-7488746561313974` — ligne app-ads.txt : `google.com, pub-7488746561313974, DIRECT, f08c47fec0942fa0`
- Site : https://agripedia.creationapp.academy (`Links.WEBSITE`)
- Contact : aymenkssi@gmail.com
