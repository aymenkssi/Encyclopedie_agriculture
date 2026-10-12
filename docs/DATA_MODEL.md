# Modèle de données

Identifiants **stables** : slug du nom scientifique pour les plantes (`solanum-lycopersicum`), codes ISO 3166-1 pour les
pays, `pays-région` pour les régions, slugs pour les références. Données utilisateur : **UUID**.

## Contenu de référence (Room, remplacé à chaque nouvelle version du jeu de données)

| Entité demandée | Table Room | Contenu / relations |
|---|---|---|
| Plant | `plant` | colonnes indexées (`scientificName`, `family`, `categoryId`, `difficulty`…), statut, `isDemo`, `lastReviewed`, `payload` JSON canonique |
| PlantTranslation | `plant_translation` (PK plantId+lang) | nom commun, autres noms, noms normalisés indexés, description |
| PlantVariety | `plant_variety` | → plant |
| PlantCategory | `plant_category` | noms multilingues |
| Country | `country` | hémisphère |
| Region | `region` | → country, → climate_zone ; coordonnées ; normales mensuelles (T moy., T min., pluie), mois de gel, saison des pluies, sols dominants, statut, sources |
| ClimateZone | `climate_zone` | codes Köppen |
| SoilType | `soil_type` | |
| CultivationMethod | `cultivation_method` | pleine terre, serre/tunnel, pot, hydroponie |
| GrowingGuide / GrowingStage | `growing_guide`, `growing_stage` | étapes ordonnées, conseils, erreurs fréquentes, restriction par mode |
| SeasonalCalendar | `seasonal_calendar` | activité × mois, portée **région** ou **zone + hémisphère**, filtrable par **variété** et **mode de culture**, statut, hypothèses, sources |
| PlantDisease / Pest / Treatment | `plant_disease`, `pest`, `treatment` | agent causal, type, symptômes, conditions, prévention, moyens de lutte (culturaux → chimiques réglementés, **sans doses**) |
| PlantCompanion | `plant_companion` | relation favorable / défavorable + justification + statut |
| CropRotation | `crop_rotation` | groupe, délai de retour, familles précédentes favorables / à éviter |
| DataSource | `data_source` | type (botanique, agronomique, climat, santé, démo), URL, licence, date de consultation |
| (liens) | `plant_link` | plante ↔ sol / mode / zone / maladie / ravageur / source (index `kind,targetId`) |
| (méta) | `content_meta` | version du jeu de données, manifeste |

Le **statut de validation** (`verified` / `indicative` / `to_verify`) existe sur les plantes, variétés, guides, calendriers,
associations, rotations, maladies, ravageurs, traitements et normales climatiques. Le validateur refuse `verified` sans
source et refuse `verified` sur un contenu de démonstration.

## Données utilisateur (jamais effacées par une mise à jour du contenu)

| Entité | Table | Notes |
|---|---|---|
| UserProfile | `user_profile` | pays, région, expérience, type d'espace, mode principal |
| UserFavorite | `user_favorite` | unique par plante, suppression logique |
| (historique) | `recent_view` | local, non synchronisé |
| UserGarden | `user_garden` | type (balcon → grande exploitation), surface, région |
| GardenPlot | `garden_plot` | → garden (cascade), surface, sol, mode |
| UserCrop | `user_crop` | → garden ; parcelle, plante, variété, mode, activité et date de départ, avancement, quantité, date de fin |
| AgriculturalTask | `agricultural_task` | échéance, fait/non fait, rappel, répétition, origine (utilisateur / prévisionnelle), liens plante/culture |
| UserNote | `user_note` | liée à une plante et/ou une culture |

Champs de synchronisation sur chaque enregistrement synchronisable : `id` (UUID), `updatedAt`, `deviceId`, `deleted`.

## Format JSON du jeu de données

- `manifest.json` : `version`, `generatedOn`, `demo`, `plantFiles[]`, `disclaimer{fr,en,ar}`.
- `reference.json` : catégories, zones climatiques, sols, modes de culture, sources.
- `geography.json` : pays → régions (normales sur 12 mois).
- `health.json` : maladies, ravageurs, traitements.
- `plants/<id>.json` : une fiche complète (variétés, guides, calendriers, associations, rotation, phénologie).

Le schéma exact est celui des classes `@Serializable` de `core/.../model/Model.kt` ; `SeedValidator` vérifie l'intégrité
référentielle, les traductions, les plages numériques et les règles d'honnêteté.
