# Architecture

Application **Expo / React Native** (SDK 57, TypeScript, expo-router), sur le même modèle que Halal & Mosque Finder.
L'APK et le fichier Google Play (.aab) sont construits par **EAS Build** (`eas.json`).

## Dossiers

| Dossier | Rôle |
|---|---|
| `lib/` | Logique sans interface, testée par Jest : modèle (`types.ts`), catalogue (`catalog.ts`, `catalogData.ts` généré), moteur de calendrier (`calendar.ts`), recommandations (`recommend.ts`), recherche floue (`search.ts`), tâches prévisionnelles (`tasks.ts`), conseils de saison (`tips.ts`), rotation (`rotation.ts`), validation du jeu de données (`validator.ts`), synchronisation (`sync.ts`), données utilisateur et sauvegarde (`userData.ts`) |
| `app/` | Écrans (expo-router) : onglets Accueil, Encyclopédie, Calendrier, Mes cultures, Profil ; fiche plante, jardin, culture, notes, historique, sources, don |
| `components/` | Éléments d'interface partagés (cartes, puces, feuilles, éditeurs de tâche / note / jardin / culture, sélecteur de région, bannière publicitaire) |
| `contexts/` | Langue (fr/ar/en, RTL), thème clair/sombre, réglages, données utilisateur, publicité (consentement UMP) |
| `constants/` | Textes de l'interface (`translations.ts`), couleurs, liens, IDs AdMob |
| `data/seed/` | Jeu de données JSON (une plante = un fichier), embarqué dans l'application par `yarn catalog` |
| `__tests__/` | Tests Jest : jeu de données, calendriers, recherche, tâches, rotation, traductions |

## Flux de données

```
data/seed/*.json ──(yarn catalog)──► lib/catalogData.ts ──► Catalog en mémoire (lecture seule)
                                                              │  computeCalendar · recommend · PlantSearch
Écrans (app/) ◄── contexts ◄── AsyncStorage (réglages, favoris, jardins, parcelles, cultures, tâches, notes, historique)
```

- **Hors ligne** : tout le contenu est dans le paquet de l'application ; aucune fonctionnalité ne dépend du réseau
  (Internet ne sert qu'à la bannière AdMob). Les API externes ne sont utilisées que par les outils d'import (`tools/`).
- **Identifiants stables** : les données personnelles ne référencent le contenu que par identifiant (slug du nom
  scientifique) : une mise à jour du contenu ne supprime jamais de données personnelles.
- **Catalogue en mémoire** : la recherche floue (distance d'édition restreinte, normalisation des accents et de l'arabe)
  est plus pertinente qu'un index plein texte qui ne tolère pas les fautes.

## Décisions techniques

| Décision | Raison |
|---|---|
| Expo + EAS Build | Même chaîne que les autres applications : `eas build` produit l'APK (test) et l'AAB (Google Play) sans Android Studio |
| AsyncStorage (JSON) plutôt qu'une base SQL | Volume de données personnelles faible ; sauvegarde/restauration directe en JSON |
| Métadonnées de synchronisation sur chaque enregistrement (`id`, `updatedAt`, `deviceId`, `deleted`) | Synchronisation future sans doublons ni pertes (last-writer-wins) ; déjà utilisé par l'import de sauvegarde |
| Gestion manuelle du RTL (`isRTL`, `useRow`) | Changement de langue immédiat, sans redémarrer l'application |
| Pas de `Intl` ni `String.normalize` | Comportement identique sur tous les moteurs JavaScript (Hermes) : noms de mois, pluriels et accents gérés par des tables |
| Rappels locaux (expo-notifications) à 8 h le jour prévu | Uniquement pour les tâches de l'utilisateur, jamais de rappel agronomique universel |
| Pas de photos embarquées | Aucune source d'images sous licence vérifiée ; le champ `image`/`imageCredit` est prévu, l'interface affiche une illustration de catégorie |

## Extensibilité

- **Nouvelle plante / variété / calendrier régional** : un fichier JSON (voir `DATA_SOURCES.md`), puis `yarn catalog`.
- **Nouvelle langue** : clé supplémentaire dans les textes du jeu de données + une entrée dans `constants/translations.ts`.
- **Grandes exploitations** : jardins / parcelles / cultures sont la base ; équipes, stocks et indicateurs s'ajouteront
  comme nouveaux enregistrements reliés à `gardenId`.
- **Synchronisation** : échanger les enregistrements et appliquer `mergeRecords` (`lib/sync.ts`).
