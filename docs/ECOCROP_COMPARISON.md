# Comparaison avec FAO ECOCROP

Généré le 13 octobre 2026 par `tools/import_ecocrop.py` à partir de la base **FAO ECOCROP** (paquet R
[Recocrop](https://github.com/cran/Recocrop) 0.4-2, fichier `inst/parameters/ecocrop.rds`, 1 710 espèces).
Colonnes « actuel » : valeurs du jeu de démonstration **avant** l'import.

## Ce qui a été importé

- **pH optimal du sol** (ECOCROP `PHOPMN`–`PHOPMX`) pour les 17 plantes ; la source `fao-ecocrop` est citée sur chaque fiche.
- Le statut des fiches ne change pas (« à vérifier ») : ECOCROP donne des plages mondiales, pas des recommandations locales.

## Ce qui n'a pas été importé, et pourquoi

**Températures.** ECOCROP donne les limites les plus larges observées dans le monde (tomate 7–35 °C, maïs 10–47 °C).
Le moteur de calendrier d'AgriPedia les utilise comme limites d'une **bonne** culture dans une région. Un essai
d'import complet élargissait les calendriers au-delà des pratiques habituelles :

| Région | Effet observé avec les températures ECOCROP |
|---|---|
| Kaolack (Sénégal) | tomate proposée pendant les mois les plus chauds (le test de réalisme échoue) |
| Dakar (Sénégal) | blé proposé (semis en novembre) |
| Québec (Canada), Biskra (Algérie) | plus aucun calendrier pour le blé |
| Île-de-France | arachide proposée (semis en juin) ; oignon semé en novembre-décembre |

Les températures ECOCROP servent donc de **contrôle** : un écart important avec la valeur actuelle signale une
valeur à revoir, avec une source locale (fiche technique officielle, institut agricole).

**Durée de cycle.** ECOCROP compte toute la saison de culture (`GMIN`–`GMAX`), AgriPedia compte du semis ou
du repiquage jusqu'à la première récolte : les deux ne sont pas comparables directement.

## Tableau

| Plante | ECOCROP | T min / opt / max actuel | ECOCROP | pH actuel | ECOCROP | Cycle actuel | ECOCROP (saison) |
|---|---|---|---|---|---|---|---|
| allium-cepa | Onion (Allium cepa L. v cepa) | 7 / 13–24 / 28 | 4 / 12–25 / 30 | 6.0–7.0 | 6–7 | 90–160 | 85–175 |
| arachis-hypogaea | Groundnut (Arachis hypogaea L.) | 18 / 22–30 / 33 | 10 / 22–32 / 45 | 5.5–7.0 | 5.5–6.5 | 90–150 | 90–150 |
| cicer-arietinum | Chick pea (Cicer arietinum L.) | 5 / 15–25 / 28 | 7 / 15–29 / 35 | 6.0–8.0 | 6–8.5 | 100–170 | 90–180 |
| citrus-sinensis | Sweet orange (Citrus sinensis (L.) Osbeck) | 13 / 20–29 / 33 | 13 / 20–30 / 38 | 6.0–7.5 | 5–6 | — | 180–365 |
| cucurbita-pepo | Vegetable marrow (Cucurbita pepo L.) | 14 / 18–27 / 30 | 6 / 17–30 / 40 | 6.0–7.5 | 5.5–6.8 | 45–90 | 40–100 |
| daucus-carota | Carrot (Daucus carota L.) | 7 / 15–21 / 26 | 3 / 15–24 / 30 | 6.0–7.0 | 5.8–6.8 | 60–120 | 40–150 |
| fragaria-x-ananassa | Garden strawberry (Fragaria x ananassa (D.) G.) | 8 / 15–24 / 28 | 6 / 11–24 / 28 | 5.5–6.5 | 6–6.8 | — | 180–270 |
| lactuca-sativa | Lettuce (Lactuca sativa L.) | 7 / 12–20 / 24 | 5 / 12–21 / 30 | 6.0–7.0 | 6–7 | 45–80 | 35–85 |
| malus-domestica | Apple (Malus domestica Borkh.) | 8 / 15–22 / 27 | 8 / 14–27 / 33 | 6.0–7.0 | 6–7 | — | 180–320 |
| mentha-spicata | Spearmint (Mentha spicata L. v. crispa) | 8 / 15–25 / 30 | 4 / 15–25 / 36 | 6.0–7.5 | 5.5–6.5 | — | 40–270 |
| ocimum-basilicum | Basil (Ocimum basilicum L.) | 12 / 18–27 / 32 | 7 / 18–27 / 36 | 6.0–7.5 | 6–7 | 60–90 | 80–270 |
| olea-europaea | Olive, Europaen (Olea europaea L.) | 10 / 20–27 / 31 | 5 / 20–34 / 40 | 6.0–8.5 | 6–7 | — | 210–300 |
| phaseolus-vulgaris | Bean, Common (Phaseolus vulgaris L.) | 12 / 16–24 / 27 | 7 / 16–25 / 32 | 6.0–7.5 | 5.5–7.5 | 55–110 | 50–270 |
| solanum-lycopersicum | Tomato (Lycopersicon esculentum M.) | 13 / 18–26 / 29 | 7 / 20–27 / 35 | 6.0–7.0 | 5.5–6.8 | 60–100 | 70–150 |
| solanum-tuberosum | Potato (Solanum tuberosum L.) | 8 / 15–20 / 25 | 7 / 15–25 / 30 | 5.0–6.5 | 5–6.2 | 70–150 | 90–160 |
| triticum-aestivum | Wheat, common (Triticum aestivum L.) | -8 / 12–22 / 27 | 5 / 15–23 / 27 | 6.0–7.5 | 6–7 | 200–300 | 90–250 |
| zea-mays | Maize (Zea mays L. s. mays) | 13 / 18–27 / 32 | 10 / 18–33 / 47 | 5.8–7.0 | 5–7 | 75–150 | 65–365 |

Relancer :

```bash
curl -LO https://raw.githubusercontent.com/cran/Recocrop/master/inst/parameters/ecocrop.rds
pip install rdata pandas
python3 tools/import_ecocrop.py --rds ecocrop.rds            # tableau de comparaison
python3 tools/import_ecocrop.py --rds ecocrop.rds --apply    # pH optimal
```
