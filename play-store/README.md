# Publication sur Google Play · version 1.0.0

Tout ce qu'il faut pour publier **AgriPedia** (`com.aymen.agripedia`) sur Google Play, sur le même modèle que
Halal & Mosque Finder : application **Expo**, fichier pour Google Play construit avec **EAS Build**.

| Dossier / fichier | Contenu |
|---|---|
| `textes/fr.md`, `textes/en.md`, `textes/ar.md` | Nom, description courte, description complète, notes de version (limites vérifiées par `scripts/check_texts.py` et les tests) |
| `images/icon-512.png` | Icône de la fiche (512 × 512) |
| `images/feature-graphic-fr.png` (`-en`, `-ar`) | Bannière de la fiche (1024 × 500) |
| `images/screenshots-fr/` (`-en`, `-ar`) | 6 captures de téléphone (1080 × 1920) par langue (`scripts/capture-screens.js` puis `scripts/compose-images.js`) |
| `scripts/` | Génération des images et contrôle des textes |

Ordre des captures : accueil (cultures conseillées pour la région), recherche tolérante aux fautes, fiche de la tomate,
calendrier régional, tâches et rappels, mode sombre.

> ⚠️ Avant une diffusion large, lisez la section « Données » du README principal : les fiches actuelles sont des données de
> démonstration, signalées comme telles dans l'application et dans la description.

## Récapitulatif (à copier-coller)

| Élément | Valeur |
|---|---|
| Nom du paquet | `com.aymen.agripedia` |
| Version / code de version | `1.0.0` / `1` (`app.json` › `expo.version`, `expo.android.versionCode`) |
| Nom sur Google Play | `AgriPedia : potager & cultures` (fr), voir `textes/` pour en et ar |
| Catégorie | Éducation (ou Maison et intérieur) |
| E-mail de contact | `aymenkssi@gmail.com` |
| Site web | `https://agripedia.creationapp.academy` |
| Politique de confidentialité | `https://agripedia.creationapp.academy/privacy` |
| AdMob · ID d'application | `ca-app-pub-7488746561313974~5179184724` |
| AdMob · bloc bannière « Bannière bas » | `ca-app-pub-7488746561313974/2730396616` |
| `app-ads.txt` | `google.com, pub-7488746561313974, DIRECT, f08c47fec0942fa0` (`site/app-ads.txt`) |
| Autorisations | localisation approximative (au premier plan, à la demande), notifications, Internet (publicité) |
| Annonces | oui (bannière AdMob, consentement UMP en Europe) |
| Public cible | 18 ans et plus (recommandé) |

### Ordre de publication

1. Mettre le site en ligne (`deploy/README.md`) et vérifier `/privacy` et `/app-ads.txt` (étape 0.1).
2. Publier le message de consentement européen dans AdMob (étape 0.2).
3. Construire le `.aab` avec EAS (étape 1) et un APK de test pour vérifier l'application sur un téléphone.
4. Créer l'application dans la Play Console (étape 2), remplir la fiche (étape 3) et les déclarations (étape 4).
5. Test interne, puis test fermé si le compte l'exige (étape 5), puis production (étape 6).
6. Relier l'application publiée dans AdMob (étape 7).

> Les annonces réelles ne s'affichent que dans les builds `preview` et `production` ; les builds de développement
> affichent toujours les annonces de test de Google. Ne cliquez jamais sur vos propres annonces.

---

## 0. Avant de commencer (une seule fois)

1. **Site en ligne** (politique de confidentialité obligatoire : localisation + publicité ; `app-ads.txt` pour AdMob) :
   suivre `deploy/README.md` (DNS `agripedia` puis `docker compose up -d`). Vérifier que
   https://agripedia.creationapp.academy/privacy et https://agripedia.creationapp.academy/app-ads.txt s'ouvrent.
2. **AdMob** ✅ fait : application `AgriPedia` (Android) et bloc bannière `Bannière bas` créés, IDs intégrés au projet
   (`app.json` et `constants/ads.ts`, voir le récapitulatif ci-dessus). Reste à faire dans AdMob :
   - **Confidentialité et messages** › **Réglementations européennes** › **Créer un message** pour AgriPedia
     (français, anglais, arabe) › **Publier**. Sans ce message, presque aucune annonce n'est diffusée en Europe.
   - Le statut « Requires review » de l'application AdMob passe à « Approuvé » une fois l'application publiée sur
     Google Play et reliée dans AdMob (étape 7).
3. **Compte Expo** (gratuit) : https://expo.dev/signup, puis sur l'ordinateur (ou le serveur) qui lance les builds :
   `npx eas-cli login`. Au premier build, accepter la création du projet EAS (`eas init` est proposé automatiquement).

## 1. Construire l'APK (test) et le fichier pour Google Play (.aab)

```bash
cd Encyclopedie_agriculture
git pull && yarn install
npx eas-cli build --platform android --profile preview      # APK : installable directement sur un téléphone
npx eas-cli build --platform android --profile production   # .aab : le fichier à envoyer sur Google Play
```

- Le lien de téléchargement (et un QR code pour l'APK) s'affiche à la fin du build, et sur https://expo.dev › projet
  › **Builds**.
- Au premier build, laisser EAS **créer la clé de signature** et la garder sur le compte Expo (elle servira pour toutes
  les mises à jour ; Google Play gère ensuite la clé finale avec « Signature d'application Play »).
- Version **1.0.0**, code de version **1** (`app.json` › `expo.version` et `expo.android.versionCode`). Google Play refuse
  un code déjà envoyé (même en test interne) : **avant chaque nouveau build production, augmenter
  `android.versionCode` de 1**, et `version` (1.0.1, 1.1.0…) quand la nouvelle version est publiée.
- Sans EAS : chaque exécution de la CI GitHub (onglet **Actions** › **CI**) publie aussi un APK de test dans l'artefact
  `agripedia-apk` (signé avec une clé de debug : pour essayer l'application, pas pour Google Play).

## 2. Créer l'application dans la Play Console

https://play.google.com/console › **Créer une application** :

| Champ | Valeur |
|---|---|
| Nom de l'application | `AgriPedia : potager & cultures` |
| Langue par défaut | Français – fr-FR |
| Application ou jeu | Application |
| Sans frais ou payante | Sans frais |
| Déclarations | Cocher les règles du programme pour les développeurs et les lois d'exportation des États-Unis |

## 3. Fiche du Store

**Développer la présence sur le Store** › **Fiche principale du Store**.

1. Pour chaque langue (**Gérer les traductions** › **Ajouter vos propres traductions** : `en-US`, `ar`, en plus de `fr-FR`) :
   **Nom**, **description courte**, **description complète** depuis `textes/<langue>.md` ; **bannière**
   `images/feature-graphic-<langue>.png` ; **captures** les 6 fichiers de `images/screenshots-<langue>/`, dans l'ordre.
2. **Icône** : `images/icon-512.png`.
3. **Paramètres du Store** : catégorie **Éducation** (ou **Maison et intérieur**), tags « Jardinage », « Agriculture » si proposés.
4. **Coordonnées** : e-mail `aymenkssi@gmail.com`, site `https://agripedia.creationapp.academy` (sert `app-ads.txt`).

## 4. Contenu de l'application (déclarations)

| Rubrique | Réponse |
|---|---|
| Règles de confidentialité | `https://agripedia.creationapp.academy/privacy` |
| Annonces | **Oui, mon application contient des annonces** |
| Identifiant publicitaire | **Oui**, pour la **Publicité ou le marketing** |
| Accès à l'application | Toutes les fonctionnalités sont disponibles sans restriction (pas de compte) |
| Public cible | **18 ans et plus** conseillé (en dessous de 13 ans, les règles « Familles » imposent d'autres réglages AdMob) |
| Classification du contenu | **Tous les autres types d'applications** ; **Non** à toutes les questions (violence, jeux d'argent…) ; **Non** au partage de la position |
| Actualités, santé, finance, gouvernement | Non |
| Autorisations | Localisation approximative au premier plan uniquement, notifications |

### Sécurité des données

- **Données collectées ou partagées ?** Oui (uniquement par le SDK publicitaire). **Chiffrées en transit ?** Oui.
  **Suppression sur demande ?** Non applicable : AgriPedia n'a aucun serveur, tout reste sur le téléphone.
- **Position approximative** : utilisée sur l'appareil pour choisir la région, **non collectée** par AgriPedia (jamais
  envoyée). Ne la déclarez que via le SDK AdMob ci-dessous.
- **Google AdMob** : cocher ce que le SDK Google Mobile Ads collecte d'après la page officielle
  (<https://developers.google.com/admob/android/privacy/play-data-disclosure>) : *Position approximative*,
  *Interactions avec l'application*, *Diagnostics et journaux de plantage*, *Identifiants de l'appareil ou autres*,
  pour la **Publicité ou le marketing**, l'**Analyse** et la **Prévention des fraudes et sécurité**.

## 5. Tester avant la production

1. **Tester et publier** › **Test interne** › **Créer une version** : importer le fichier `.aab` d'EAS, nom `1.0.0 (1)`,
   notes de version (bloc ci-dessous), ajouter votre Gmail dans les testeurs et installer l'app depuis le lien de test.
2. **Comptes récents** : si l'accès à la production est verrouillé, faire d'abord un **test fermé** avec **au moins
   12 testeurs** pendant **14 jours d'affilée**, puis **Demander l'accès à la production**.

```
<fr-FR>
Première version : cultures adaptées à votre région et à la saison, calendrier agricole, guides pas à pas, maladies et ravageurs, gestion du potager et rappels, en français, arabe et anglais.
</fr-FR>
<en-US>
First release: crops suited to your region and season, farming calendar, step-by-step guides, pests and diseases, garden management and reminders, in English, Arabic and French.
</en-US>
<ar>
الإصدار الأول: محاصيل ملائمة لمنطقتك وللموسم، تقويم زراعي، أدلة خطوة بخطوة، الأمراض والآفات، إدارة البستان والتذكيرات، بالعربية والفرنسية والإنجليزية.
</ar>
```

## 6. Publier en production

**Production** › **Créer une version** (ou **Promouvoir la version**) › **Pays et régions** : tous › **Envoyer pour examen**.

## 7. Activer les publicités AdMob

1. Une fois l'app publiée : AdMob › **Applications** › AgriPedia › **Paramètres de l'application** › **Plate-forme de
   téléchargement** › **Ajouter** › `com.aymen.agripedia`.
2. **`app-ads.txt`** : servi sur `https://agripedia.creationapp.academy/app-ads.txt` ; AdMob le vérifie en 24 h.
3. Ne jamais cliquer sur ses propres annonces ; déclarer son téléphone dans AdMob › **Paramètres** › **Appareils de test**.

Les bannières s'affichent au-dessus de la barre de navigation, jamais sur la page « Soutenir AgriPedia » ni pendant
l'accueil initial. Hors ligne, la bannière disparaît et l'application fonctionne normalement.

## Refaire les images

En local (Node + Playwright), depuis la racine du projet :

```bash
mkdir -p play-store/build/fonts && cd play-store/build/fonts
npm pack @fontsource/figtree@5.2.8 @fontsource/noto-kufi-arabic@5.2.8
for f in *.tgz; do mkdir -p "${f%.tgz}" && tar -xzf "$f" -C "${f%.tgz}"; done
cd ../../..
npm install --no-save playwright@1.56.1 && npx playwright install chromium
npx expo export --platform web --output-dir play-store/build/web   # version web de l'application
node play-store/scripts/capture-screens.js    # captures brutes fr/en/ar avec un jardin de démonstration
node play-store/scripts/compose-images.js     # icône, bannières et captures encadrées dans images/
```
