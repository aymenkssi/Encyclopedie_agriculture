# Modèle de données

Identifiants **stables** : slug du nom scientifique pour les plantes (`solanum-lycopersicum`), codes ISO 3166-1 pour les
pays, `pays-région` pour les régions, slugs pour les références. Données utilisateur : **UUID**.

## Contenu de référence (embarqué, lecture seule)

Le jeu de données `data/seed/` est converti en module TypeScript (`yarn catalog` → `lib/catalogData.ts`) puis indexé en
mémoire (`lib/catalog.ts`) : plantes, variétés, catégories, pays et régions (normales mensuelles de température moyenne,
de température minimale et de pluie, mois de gel, saison des pluies, sols dominants), zones climatiques, sols, modes de
culture, guides et étapes, calendriers (portée région ou zone + hémisphère, filtrables par variété et mode de culture),
maladies, ravageurs, moyens de lutte (sans doses), associations, rotation et sources.

Le **statut de validation** (`verified` / `indicative` / `to_verify`) existe sur les plantes, variétés, guides, calendriers,
associations, rotations, maladies, ravageurs, traitements et normales climatiques. Le validateur refuse `verified` sans
source et refuse `verified` sur un contenu de démonstration.

## Données utilisateur (jamais effacées par une mise à jour du contenu)

Enregistrées sur le téléphone (AsyncStorage, `contexts/UserDataContext.tsx`, types dans `lib/userData.ts`) :

| Entité | Notes |
|---|---|
| Réglages | région, expérience, type d'espace, mode principal, rappels (`contexts/SettingsContext.tsx`) |
| Favori | unique par plante, suppression logique |
| Historique | fiches consultées, local, non synchronisé |
| Jardin | type (balcon → grande exploitation), surface, région |
| Parcelle | → jardin, surface, sol, mode |
| Culture | → jardin ; parcelle, plante, variété, mode, activité et date de départ, avancement, quantité, date de fin |
| Tâche | échéance, fait/non fait, rappel, répétition, origine (utilisateur / prévisionnelle), liens plante/culture |
| Note | liée à une plante et/ou une culture |

Champs de synchronisation sur chaque enregistrement synchronisable : `id` (UUID), `updatedAt`, `deviceId`, `deleted`.

## Format JSON du jeu de données

- `manifest.json` : `version`, `generatedOn`, `demo`, `plantFiles[]`, `disclaimer{fr,en,ar}`.
- `reference.json` : catégories, zones climatiques, sols, modes de culture, sources.
- `geography.json` : pays → régions (normales sur 12 mois).
- `health.json` : maladies, ravageurs, traitements.
- `plants/<id>.json` : une fiche complète (variétés, guides, calendriers, associations, rotation, phénologie).

Le schéma exact est celui des types de `lib/types.ts` ; `lib/validator.ts` (exécuté par `yarn test`) vérifie l'intégrité
référentielle, les traductions, les plages numériques et les règles d'honnêteté.
