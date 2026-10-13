# Sources, licences et qualité des données

## État actuel (v0.1) — à lire

Le jeu de données livré est un **jeu de démonstration** :

- 17 fiches (tomate, pomme de terre, carotte, laitue, courgette, oignon, haricot, pois chiche, arachide, blé tendre, maïs,
  fraisier, olivier, pommier, oranger, basilic, menthe verte) rédigées à partir de connaissances horticoles générales,
  **non validées par un agronome**, toutes marquées `isDemo: true` et `status: "to_verify"`.
- 19 régions (France, Maroc, Algérie, Tunisie, Égypte, Sénégal, Canada/Québec, Brésil) avec des **normales climatiques
  approximatives** saisies manuellement (`climateStatus: "to_verify"`, source `climate-approx`).
- Aucun rendement chiffré, aucune dose de produit : uniquement des facteurs et des méthodes.
- Aucune photographie : aucune source d'images n'a pu être consultée et sa licence vérifiée.

**Pourquoi** : l'environnement de développement de cette version n'avait accès à aucune source externe (FAO, POWO, GBIF,
Open-Meteo, dépôts Google étaient bloqués par la politique réseau). Plutôt que d'inventer des références, chaque source est
déclarée avec `accessedOn: null` (« non consultée ») et l'application affiche ces statuts.

## Sources prévues et usage

| Source | Usage | Licence / conditions (à revérifier avant intégration) | Outil |
|---|---|---|---|
| Kew — Plants of the World Online | noms acceptés, autorités, familles, synonymes | données CC BY (vérifier) | contrôle manuel |
| GBIF Backbone Taxonomy | noms scientifiques, synonymes, noms vernaculaires | CC BY 4.0 | `tools/check_taxonomy_gbif.py` |
| FAO ECOCROP / GAEZ | plages de température, pH, pluie, cycles | conditions FAO | saisie avec citation |
| Services agricoles nationaux / instituts (INRAE, ONSSA, INRA Maroc, INRAA, ISRA…) | calendriers régionaux, variétés | selon l'organisme | entrées `calendar` scopées `regionId` |
| Open-Meteo (ERA5) / normales OMM / services météo nationaux | normales mensuelles | Open-Meteo CC BY 4.0 | `tools/import_climate_normals.py` |
| Beck et al. 2018 (Köppen-Geiger) | classification climatique | CC BY 4.0 | |
| EPPO Global Database | noms des organismes nuisibles, hôtes | conditions EPPO | |
| Registres nationaux (E-Phy/ANSES, ONSSA…) | produits **autorisés** | publics | renvoi, jamais de dose dans l'app |

Aucune clé d'API n'est utilisée ni requise par l'application.

## Procédure d'enrichissement

1. Ajouter/modifier `data/seed/plants/<id>.json` (id = slug du nom scientifique accepté) et l'ajouter à `manifest.json`.
2. Renseigner `sourceIds`, `lastReviewed`, et un `status` par élément ; `verified` exige au moins une source et `isDemo: false`.
3. Pour une recommandation régionale, ajouter une entrée `calendar` avec `regionId` (ou `climateZoneId` + `hemisphere`),
   ses `assumptions` (hypothèses et limites) et ses `sourceIds`.
4. Remplacer les normales : `python3 tools/import_climate_normals.py` (réseau requis), relire le diff.
5. `yarn catalog && yarn test` — le validateur bloque les références cassées, traductions manquantes, incohérences.
6. Incrémenter `manifest.json.version` ; le nouveau contenu est livré avec la prochaine version de l'application, sans
   toucher aux données des utilisateurs.

## Distinction des types d'information

- **Botanique** (nom, famille, synonymes) : `Plant.scientificName…`, sources `kind: botanical`.
- **Agronomique** (températures, sol, cycles, guides, calendriers) : sources `kind: agronomic`, statut par élément.
- **Climatique** (normales régionales) : `Region.climateStatus`, sources `kind: climate`.
- **Sanitaire** (maladies, ravageurs, traitements) : sources `kind: health`, avertissement réglementaire obligatoire pour
  les traitements chimiques (vérifié par le validateur).
