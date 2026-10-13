# AgriPedia — encyclopédie agricole (Android, Expo)

AgriPedia est une encyclopédie agricole interactive, **hors ligne et sans compte**, en **français, arabe (RTL) et anglais**.
Elle recommande des cultures selon le **pays, la région, la période (mois, saison ou saison des pluies) et le mode de culture**,
affiche des **calendriers indicatifs calculés à partir du climat régional**, des **guides pas à pas**, les **maladies et ravageurs**,
et permet de gérer ses **jardins, parcelles, cultures, tâches, rappels, notes et favoris**.

> ⚠️ **Données de démonstration.** Les 17 fiches plantes, les normales climatiques des 19 régions et les données sanitaires
> fournies sont un jeu de démonstration **non validé par un agronome** (statut « à vérifier »). L'application l'affiche
> clairement. Voir [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) pour la procédure de remplacement par des données sourcées.

## Fonctionnalités (v0.1)

| Domaine | Ce qui fonctionne |
|---|---|
| Accueil | recherche, sélecteur pays/région (manuel ou géolocalisation facultative), mois / saisons / saison des pluies-sèche, catégories, cultures conseillées pour la région et la période, tâches à faire, activités du mois pour vos plantes, conseils de saison **dérivés des données climatiques**, favoris, consultations récentes |
| Encyclopédie | recherche tolérante aux fautes (FR/AR/EN, nom scientifique, synonymes, variétés), normalisation des accents et des variantes arabes (أ/إ/آ, ة/ه, ى/ي, tashkīl), filtres combinables : catégorie, difficulté, eau, mode de culture, sol, climat, durée de cycle, pays, « adaptée à ma région maintenant » |
| Fiche plante | noms multilingues, nom scientifique + autorité, famille, statut de validation, variétés, description, températures, gel, exposition, sol, pH, eau, cycle, espacements, profondeur, pépinière, rendement (facteurs, sans chiffres inventés), conservation, **calendrier régional** par mode de culture (et par variété si données), hypothèses et limites, guide pas à pas, maladies/ravageurs/moyens de lutte (sans doses), associations, rotation, sources et date de mise à jour, notes personnelles |
| Calendrier | tâches du mois et en retard, création / modification / suppression, rappel, répétition (arrosage…), calendrier des cultures suivies |
| Mes cultures | jardins/exploitations (type, surface, région), parcelles (surface, sol, mode), cultures (mode, date de départ, avancement, progression vers la récolte), **alerte de rotation** par parcelle, génération d'étapes prévisionnelles (repiquage, contrôles sanitaires, récolte) à partir **de vos dates** |
| Profil | région de référence, expérience, type d'espace, mode principal, langue (par application), thème clair/sombre/système, rappels, notes, historique, sources, **sauvegarde/restauration JSON sans doublons** |
| Notifications | rappels locaux (8 h le jour prévu) **uniquement pour les tâches de l'utilisateur** |

## Architecture

Application **Expo / React Native** (SDK 57, TypeScript, expo-router), comme Halal & Mosque Finder.

```
app/          Écrans (expo-router) : onglets + fiche plante, jardin, culture, notes, historique, sources, don.
components/   Éléments d'interface partagés.  contexts/  Langue, thème, réglages, données utilisateur, publicité.
lib/          Logique sans interface, testée par Jest : modèle, moteur de calendrier, recommandations,
              recherche floue, tâches prévisionnelles, rotation, conseils de saison, validation, synchronisation.
constants/    Textes FR/AR/EN (translations.ts), couleurs, liens, IDs AdMob.
data/seed/    Jeu de données JSON (une plante = un fichier) → lib/catalogData.ts par `yarn catalog`.
tools/        Import des normales climatiques (Open-Meteo), contrôle taxonomique (GBIF).
docs/         Architecture, modèle de données, règles de recommandation, sources et qualité des données.
```

Détails : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · [docs/DATA_MODEL.md](docs/DATA_MODEL.md) ·
[docs/RECOMMENDATION_RULES.md](docs/RECOMMENDATION_RULES.md) · [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md).

## Lancer, tester, générer l'APK

Prérequis : Node 22 et Yarn 1.

```bash
yarn install
yarn start                 # Expo (application Expo Go / build de développement), « w » pour le navigateur
yarn typecheck && yarn lint && yarn test      # vérifications (aussi exécutées par la CI)
yarn catalog               # après toute modification de data/seed/
```

**APK avec EAS** (compte Expo gratuit) :

```bash
npx eas-cli login
npx eas-cli build --platform android --profile preview      # APK à installer sur un téléphone (lien en fin de build)
npx eas-cli build --platform android --profile production   # .aab pour Google Play
```

La CI GitHub Actions (`.github/workflows/ci.yml`) vérifie le code (types, lint, tests, export web) puis compile
l'application Android native (`expo prebuild` + Gradle) et publie un APK de test en artefact `agripedia-apk`.

## Langues

- Interface : `constants/translations.ts` (fr, ar, en). Un test vérifie que les trois langues ont les mêmes clés et les
  mêmes paramètres, et que chaque texte utilisé par les écrans existe.
- Contenu : chaque texte du jeu de données est une table `{"fr", "en", "ar"}` ; ajouter une langue = ajouter une clé
  (repli automatique : langue demandée → en → fr).
- Arabe : mise en page de droite à gauche, recherche normalisée (أ/إ/آ, ة/ه, ى/ي, tashkīl).

## Confidentialité

Aucun compte, aucun serveur, aucune donnée personnelle collectée par AgriPedia : tout reste sur le téléphone. Permissions :
localisation **approximative** (au clic sur « Utiliser ma position », utilisée une fois sur l'appareil, jamais stockée),
notifications, Internet (uniquement pour la bannière publicitaire). Politique publiée :
https://agripedia.creationapp.academy/privacy (`site/privacy.html`).

## Publicité, dons et publication

- **Bannière Google AdMob** au-dessus de la barre de navigation (compte `pub-7488746561313974`), précédée du formulaire de
  consentement de Google (RGPD) ; « Confidentialité des annonces » dans le Profil. Jamais sur la page de don. Les IDs réels
  se mettent dans `app.json` et `constants/ads.ts` ; en attendant, ce sont les annonces de test de Google.
- **Soutenir AgriPedia** : don PayPal (même lien que les autres apps), noter, partager, proposer des données.
- **Site** (`site/`, déployé par `deploy/` derrière Traefik) : accueil, `/privacy`, `/app-ads.txt`.
- **Google Play** : tout est dans [`play-store/README.md`](play-store/README.md) (textes FR/EN/AR, icône, bannières,
  captures, déclarations, build `.aab` avec EAS). Paquet : `com.aymen.agripedia`.

## Évolution commerciale

Un découpage gratuit / premium / pro (planification avancée, exports, synchronisation multi-appareils) pourra s'appuyer
sur les données déjà structurées. Aucun abonnement ni achat intégré n'est implémenté.

## Vérifications automatisées (CI)

| Étape | Contenu |
|---|---|
| `yarn catalog` | `lib/catalogData.ts` est à jour avec `data/seed/` |
| `yarn typecheck`, `yarn lint` | TypeScript strict, ESLint (règles Expo et React) |
| `yarn test` | validation du jeu de données (références, traductions FR/AR/EN, règles d'honnêteté), moteur de calendrier (gel, chaleur, saison des pluies, serre, hydroponie, hémisphère sud), recommandations, recherche floue multilingue, synchronisation, tâches prévisionnelles, rotation, conseils de saison, traductions de l'interface |
| `expo export --platform web` | tous les écrans se construisent |
| `android` | `expo prebuild` + Gradle : APK de test publié en artefact `agripedia-apk` |
