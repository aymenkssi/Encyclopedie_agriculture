# AgriPedia — encyclopédie agricole Android

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
| Accueil | recherche, sélecteur pays/région (manuel ou géolocalisation facultative), mois / saisons / saison des pluies-sèche, catégories, cultures conseillées pour la région et la période, tâches à faire, activités du mois pour vos plantes, conseils de saison **dérivés des données climatiques**, favoris, consultations récentes, bannière hors ligne |
| Encyclopédie | recherche tolérante aux fautes (FR/AR/EN, nom scientifique, synonymes, variétés), normalisation des accents et des variantes arabes (أ/إ/آ, ة/ه, ى/ي, tashkīl), filtres combinables : catégorie, difficulté, eau, mode de culture, sol, climat, durée de cycle, pays, « adaptée à ma région maintenant » |
| Fiche plante | noms multilingues, nom scientifique + autorité, famille, statut de validation, variétés, description, températures, gel, exposition, sol, pH, eau, cycle, espacements, profondeur, pépinière, rendement (facteurs, sans chiffres inventés), conservation, **calendrier régional** par mode de culture (et par variété si données), hypothèses et limites, guide pas à pas, maladies/ravageurs/moyens de lutte (sans doses), associations, rotation, sources et date de mise à jour, notes personnelles |
| Calendrier | tâches du mois et en retard, création / modification / suppression, rappel, répétition (arrosage…), calendrier des cultures suivies |
| Mes cultures | jardins/exploitations (type, surface, région), parcelles (surface, sol, mode), cultures (variété, mode, date de départ, avancement), **alerte de rotation** par parcelle, génération d'étapes prévisionnelles (repiquage, contrôles sanitaires, récolte) à partir **de vos dates** |
| Profil | région de référence, expérience, type d'espace, mode principal, langue (par application), thème clair/sombre/système, rappels, notes, historique, sources, **sauvegarde/restauration JSON sans doublons** |
| Notifications | rappels locaux quotidiens (WorkManager) **uniquement pour les tâches de l'utilisateur** |

## Architecture

```
core/   Kotlin/JVM pur — modèle, chargement + validation du jeu de données, moteur de calendrier,
        moteur de recommandation, recherche floue, planificateur de tâches, fusion de synchronisation.
        Testé sans SDK Android (./gradlew :core:test).
app/    Android (Jetpack Compose, Material 3, Navigation, Room, DataStore, WorkManager, AppCompat locales).
data/seed/  Jeu de données JSON (partagé : assets de l'APK + tests du core). Ajouter une plante = ajouter un fichier.
tools/  Génération des chaînes FR/AR/EN, import des normales climatiques (Open-Meteo), contrôle taxonomique (GBIF).
docs/   Architecture, modèle de données, règles de recommandation, sources et qualité des données.
```

Détails : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · [docs/DATA_MODEL.md](docs/DATA_MODEL.md) ·
[docs/RECOMMENDATION_RULES.md](docs/RECOMMENDATION_RULES.md) · [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md).

## Compiler et lancer

Prérequis : JDK 17+, Android SDK (plateforme 35) — Android Studio Ladybug ou plus récent.

```bash
./gradlew :core:test            # valide le jeu de données + tests des moteurs (sans SDK Android)
./gradlew :app:assembleDebug    # APK : app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # tests Room sur émulateur/appareil
./gradlew :app:installDebug
```

Sans SDK Android configuré (`local.properties` / `ANDROID_HOME`), le module `:app` est ignoré pour que `:core` reste
compilable. La CI GitHub Actions (`.github/workflows/android.yml`) construit l'APK, exécute les tests et lint, et publie l'APK
en artefact.

## Langues

- Interface : `app/src/main/res/values` (fr, par défaut), `values-en`, `values-ar`, générés depuis `tools/strings_source.py`
  (`python3 tools/generate_strings.py`). Un test vérifie que les trois langues ont les mêmes clés et les mêmes paramètres.
- Contenu : chaque texte du jeu de données est une table `{"fr", "en", "ar"}` ; ajouter une langue = ajouter une clé
  (repli automatique : langue demandée → en → fr).
- Arabe : `supportsRtl`, icônes « auto-mirrored », recherche normalisée.

## Confidentialité

Aucun compte, aucun serveur, aucune donnée personnelle collectée par AgriPedia : tout reste sur le téléphone. Permissions :
localisation **approximative** (au clic sur « Utiliser ma position », utilisée une fois sur l'appareil, jamais stockée),
notifications, Internet et état du réseau (uniquement pour la bannière publicitaire). Politique publiée :
https://agripedia.creationapp.academy/privacy (`site/privacy.html`).

## Publicité, dons et publication

- **Bannière Google AdMob** au-dessus de la barre de navigation (compte `pub-7488746561313974`), précédée du formulaire de
  consentement de Google (RGPD) ; « Confidentialité des annonces » dans le Profil. Jamais sur la page de don. Les IDs réels
  se configurent dans les secrets GitHub ; en attendant, ce sont les annonces de test de Google.
- **Soutenir AgriPedia** : don PayPal (même lien que les autres apps), noter, partager, proposer des données.
- **Site** (`site/`, déployé par `deploy/` derrière Traefik) : accueil, `/privacy`, `/app-ads.txt`.
- **Google Play** : tout est dans [`play-store/README.md`](play-store/README.md) (textes FR/EN/AR, icône, bannières,
  captures, déclarations, build signé `.aab` via le workflow « Release (Google Play) »). Paquet : `com.aymen.agripedia`.

## Évolution commerciale

`FeatureGate` (dans `AppContainer.kt`) prépare un découpage gratuit / premium / pro (planification avancée, exports,
synchronisation multi-appareils). Aucun abonnement ni achat intégré n'est implémenté.

## Vérifications automatisées (CI)

| Étape | Contenu |
|---|---|
| `:core:test` | 45+ tests : validation du jeu de données (références, traductions FR/AR/EN, règles d'honnêteté), moteur de calendrier (gel, chaleur, saison des pluies, serre, hydroponie, hémisphère sud, réalisme), recommandations, recherche floue multilingue, fusion de synchronisation, tâches prévisionnelles, géolocalisation |
| `:app:assembleDebug` | APK publié en artefact `agripedia-debug-apk` |
| `:app:testDebugUnitTest` | cohérence des ressources FR/AR/EN (clés et paramètres), chargement du jeu de données embarqué |
| `:app:lintDebug` | lint Android sans erreur |
| `emulator-tests` | émulateur API 34 : installation, lancement, onboarding, 5 onglets, recherche « tomtae » → fiche tomate (guide, santé), import Room + reconstruction du catalogue, favoris et tâches répétitives persistés |

Diagnostic : `AGRI_DUMP=1 ./gradlew :core:test --tests '*DumpTest*' -i` affiche les calendriers calculés pour plusieurs régions.
