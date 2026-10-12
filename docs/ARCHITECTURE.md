# Architecture

## Modules

| Module | Rôle | Dépendances |
|---|---|---|
| `:core` | Domaine pur Kotlin/JVM : modèle (`model/Model.kt`), chargement (`seed/SeedLoader`) et validation (`seed/SeedValidator`) du jeu de données, `engine/CalendarEngine`, `engine/RecommendationEngine`, `engine/TaskPlanner`, `engine/RegionLocator`, `search/PlantSearch`, `sync/SyncMerger` | kotlinx-serialization |
| `:app` | Application Android : persistance Room, préférences DataStore, rappels WorkManager, UI Compose | `:core`, AndroidX |

Séparer le domaine permet de **tester toute la logique métier sans émulateur** et de réutiliser les moteurs côté serveur
(import, validation, API future).

## Flux de données

```
data/seed/*.json ──(assets)──► ContentImporter ──► Room (tables de contenu normalisées + JSON canonique)
                                                     │
                                                     ▼
                                     Catalog en mémoire (lecture seule) ──► Knowledge
                                                     │                      (CalendarEngine, RecommendationEngine, PlantSearch)
UI Compose ◄── ViewModels ◄── UserRepository (Room : profil, favoris, jardins, parcelles, cultures, tâches, notes)
```

- **Import versionné** : `manifest.json.version` ; au démarrage, si la version embarquée est plus récente, les tables de
  contenu sont remplacées dans une transaction. Les tables utilisateur ne référencent le contenu que par **identifiants
  stables** (slug du nom scientifique) sans clé étrangère en cascade : une mise à jour du contenu ne supprime jamais de
  données personnelles.
- **Hors ligne** : tout le contenu est en base locale ; aucune fonctionnalité ne dépend du réseau. Les API externes ne sont
  utilisées que par les **outils d'import** (`tools/`), jamais par l'application.
- **Catalogue en mémoire** : le jeu de données (quelques centaines de fiches visées) tient en mémoire ; la recherche floue
  (distance de Damerau-Levenshtein restreinte) y est plus pertinente qu'un FTS SQLite qui ne tolère pas les fautes. Au-delà
  de quelques milliers de fiches, prévoir un index FTS4 sur `plant_translation.normalizedNames` comme pré-filtre.

## Décisions techniques (non bloquantes, documentées)

| Décision | Raison |
|---|---|
| Injection manuelle (`AppContainer`) plutôt que Hilt | Moins de génération de code ; remplaçable sans toucher aux écrans |
| Room + colonne `payload` JSON canonique | Tables normalisées et indexées pour les requêtes, reconstruction sans perte du modèle de domaine |
| `AppCompatActivity` + `AppCompatDelegate.setApplicationLocales` | Choix de langue par application sur toutes les versions (API 26+) |
| UUID + `updatedAt` + `deviceId` + suppression logique sur toutes les données utilisateur | Synchronisation future sans doublons ni pertes (`SyncMerger`, last-writer-wins, la suppression gagne en cas d'égalité) ; déjà utilisé par l'import de sauvegarde |
| Pas de photos embarquées | Aucune source d'images sous licence vérifiée n'a pu être consultée ; le champ `image`/`imageCredit` est prévu, l'UI affiche une illustration de catégorie |
| Normales climatiques mensuelles + règles explicables | Calendriers calculés et justifiables, remplaçables par des données explicites régionales |
| `minSdk 26` | `java.time` natif, canaux de notification, icônes adaptatives |

## Extensibilité

- **Nouvelle plante / variété / calendrier régional** : un fichier JSON (voir `DATA_SOURCES.md`), aucun code.
- **Nouvelle langue** : clé supplémentaire dans les textes du jeu de données + `values-xx/strings.xml`.
- **Grandes exploitations** : `UserGardenEntity` / `GardenPlotEntity` / `UserCropEntity` sont la base ; équipes, stocks et
  indicateurs s'ajouteront comme nouvelles entités reliées à `gardenId` (migration Room additive).
- **Synchronisation** : brancher un `RemoteSource` qui échange des `SyncMeta` + payloads, appliquer `SyncMerger.merge`.
- **Premium** : `FeatureGate.isEnabled(feature)` autour des écrans concernés.

## Migrations

`AppDatabase.MIGRATIONS` + schémas exportés dans `app/schemas`. Les migrations destructives sont interdites (données
utilisateur). Le contenu de référence, lui, est réimporté à chaque changement de version du jeu de données.
