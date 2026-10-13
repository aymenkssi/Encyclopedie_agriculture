# Règles de calendrier et de recommandation

Toutes les règles sont dans `lib/calendar.ts` et `lib/recommend.ts` et couvertes par des tests (`__tests__/`). Le moteur ne
produit **aucun texte** : il renvoie des codes (`NoteCode`) traduits par l'interface, pour que les trois langues reçoivent
exactement le même raisonnement.

## Priorité des sources de calendrier

1. Entrées explicites du jeu de données **pour la région** (`SeasonalCalendarEntry.regionId`).
2. Entrées explicites pour la **zone climatique + hémisphère** (`climateZoneId`, `hemisphere`).
3. **Calcul** à partir des besoins de la plante et des normales climatiques de la région.

Les entrées explicites remplacent le calcul activité par activité, et peuvent être restreintes à une **variété** ou à un
**mode de culture**.

## Calcul pour les cultures annuelles (et bisannuelles cultivées comme annuelles)

Pour chaque mois *m* (1–12, cycliques) :

- température effective = moyenne mensuelle de la région (+4 °C hypothèse « serre/tunnel non chauffé ») ;
- **mois de gel** = liste explicite de la région, sinon moyenne des minimales < 2 °C, sinon moyenne < 7 °C ;
- mois **cultivable** si `temperature.min ≤ T ≤ temperature.max` et, pour une plante gélive, hors mois de gel
  (tolérance « légère » : minimales ≥ −3 °C).

Un mois de **démarrage** *s* est retenu si :

1. tous les mois du cycle moyen (`ceil(moyenne(cycleDays)/30)`) sont cultivables ;
2. la température moyenne sur le cycle atteint le **seuil de maturation** = milieu entre `min` et `optimalMin` ;
3. au moins un mois de récolte (`s + cycleMin/30 … s + cycleMax/30`) atteint ce seuil.

On en déduit : semis direct (si germination ≥ `germinationMin`), repiquage, semis en pépinière (repiquage −
`nurseryWeeks`, pépinière supposée abritée), plantation (multiplication végétative), récolte, préparation du sol, suivi
sanitaire, fertilisation, préparation de la rotation, et **irrigation** pour les mois de croissance où la pluie mensuelle
est inférieure au seuil du besoin en eau (faible 20 mm, moyen 50 mm, élevé 80 mm).

**Climat tropical / semi-aride** : quand la région a une `rainySeasonMonths`, les démarrages tombant dans les deux premiers
mois de la saison des pluies sont **mis en avant** (cultures pluviales) — le calendrier ne se fonde donc pas sur les saisons
calendaires. Les saisons proposées à l'utilisateur deviennent « saison des pluies / saison sèche ».

**Hydroponie** : milieu contrôlé, toute l'année, signalé comme tel.

## Plantes pérennes

Calendrier issu de la **phénologie** (mois de plantation, taille, floraison, récolte pour l'hémisphère nord), décalé de
6 mois dans l'hémisphère sud ; en climat tropical, statut abaissé à « à vérifier » avec avertissement. La recommandation
est **bloquée** si :

- minimale absolue estimée (minimale mensuelle la plus froide − 7 °C) < `hardinessMin` ;
- besoin de froid (`requiresWinterChill`) et mois le plus froid > `chillColdestMonthMaxC`.

## Niveaux de recommandation

| Niveau | Condition |
|---|---|
| Conseillé maintenant | calendrier disponible, mode de culture adapté, un semis/plantation possible dans la période, **et** zone climatique habituelle de la plante |
| Possible | adapté à la région mais pas de démarrage dans la période, ou zone climatique inhabituelle |
| Déconseillé | mode de culture non prévu, rusticité/froid insuffisants, ou aucune fenêtre trouvée |
| Données insuffisantes | pas de normales climatiques ni de calendrier explicite |

Le score (classement uniquement) tient compte de la difficulté et du niveau de l'utilisateur, de l'alignement sur la saison
des pluies et des besoins d'irrigation. **Aucun niveau n'est présenté comme une garantie** : l'écran rappelle que sol,
microclimat, variété et conduite comptent autant que la région.

## Tâches prévisionnelles

`planTasks` (`lib/tasks.ts`) part **de la date réelle saisie par l'utilisateur** : repiquage (+ `nurseryWeeks.min`), contrôles sanitaires
toutes les 2 semaines jusqu'à la récolte, début de récolte (+ `cycleDays.min`), préparation de la rotation
(+ `cycleDays.max`). Sans donnée, aucune date n'est inventée. Aucune notification universelle n'est envoyée.

## Limites connues

- Normales mensuelles : pas de prise en compte fine de l'altitude, des microclimats ni de la variabilité interannuelle.
- Le seuil de maturation est une approximation des sommes de températures (degrés-jours) qui seront préférables quand les
  données par culture seront disponibles.
- Blé : les paramètres décrivent le blé d'hiver ; le blé de printemps des climats continentaux devra être modélisé comme
  variété avec son propre cycle/calendrier.
