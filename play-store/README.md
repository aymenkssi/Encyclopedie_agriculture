# Publication sur Google Play · version 1.0.0

Tout ce qu'il faut pour publier **AgriPedia** (`com.aymen.agripedia`) sur Google Play, sur le même modèle que
Halal & Mosque Finder. Différence principale : AgriPedia est une application Android **native (Kotlin)** ; le fichier
pour Google Play est construit par **GitHub Actions** (workflow « Release (Google Play) ») au lieu d'EAS.

| Dossier / fichier | Contenu |
|---|---|
| `textes/fr.md`, `textes/en.md`, `textes/ar.md` | Nom, description courte, description complète, notes de version (limites vérifiées par `scripts/check_texts.py` et les tests) |
| `images/icon-512.png` | Icône de la fiche (512 × 512) |
| `images/feature-graphic-fr.png` (`-en`, `-ar`) | Bannière de la fiche (1024 × 500) |
| `images/screenshots-fr/` (`-en`, `-ar`) | 5 captures de téléphone (1080 × 1920) par langue, produites par le workflow « Store screenshots » |
| `scripts/` | Génération des images et contrôle des textes |

Ordre des captures : accueil (cultures conseillées pour la région), recherche tolérante aux fautes, fiche de la tomate,
calendrier régional, mode sombre.

> ⚠️ Avant une diffusion large, lisez la section « Données » du README principal : les fiches actuelles sont des données de
> démonstration, signalées comme telles dans l'application et dans la description.

---

## 0. Avant de commencer (une seule fois)

1. **Site en ligne** (politique de confidentialité obligatoire : localisation + publicité ; `app-ads.txt` pour AdMob) :
   suivre `deploy/README.md` (DNS `agripedia` puis `docker compose up -d`). Vérifier que
   https://agripedia.creationapp.academy/privacy et https://agripedia.creationapp.academy/app-ads.txt s'ouvrent.
2. **AdMob** (même compte éditeur `pub-7488746561313974` que vos autres apps) :
   - AdMob › **Applications** › **Ajouter une application** › Android › « Non publiée sur Google Play » › nom `AgriPedia`.
     Notez l'**ID d'application** (`ca-app-pub-7488746561313974~…`).
   - Dans l'app : **Blocs d'annonces** › **Bannière** › nom `Bannière bas`. Notez l'**ID du bloc** (`ca-app-pub-7488746561313974/…`).
   - **Confidentialité et messages** › **Réglementations européennes** › **Créer un message** pour AgriPedia
     (français, anglais, arabe) › **Publier**. Sans ce message, presque aucune annonce n'est diffusée en Europe.
   - Envoyez-moi les deux IDs : je les mets dans le projet. Sinon, ajoutez-les vous-même en secrets (étape 1).
     Tant qu'ils ne sont pas renseignés, l'application affiche les **annonces de test** de Google (et toujours en debug).
3. **Clé d'importation** (signature de l'app, à faire une seule fois et à **conserver précieusement**) :

   ```bash
   keytool -genkeypair -v -keystore agripedia-upload.jks -alias agripedia \
           -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 agripedia-upload.jks > agripedia-upload.jks.b64     # macOS : base64 -i agripedia-upload.jks
   ```

   Gardez `agripedia-upload.jks` et ses mots de passe hors du dépôt (gestionnaire de mots de passe, sauvegarde).
   Google Play gère la clé de signature finale (« Signature d'application Play ») ; cette clé ne sert qu'à envoyer les versions.

## 1. Construire le fichier pour Google Play (.aab)

1. GitHub › dépôt › **Settings** › **Secrets and variables** › **Actions** › **New repository secret** :

   | Secret | Valeur |
   |---|---|
   | `AGRIPEDIA_KEYSTORE_BASE64` | contenu de `agripedia-upload.jks.b64` |
   | `AGRIPEDIA_SIGNING_STOREPASSWORD` | mot de passe du keystore |
   | `AGRIPEDIA_SIGNING_KEYALIAS` | `agripedia` |
   | `AGRIPEDIA_SIGNING_KEYPASSWORD` | mot de passe de la clé |
   | `AGRIPEDIA_ADMOB_APPID` | ID d'application AdMob |
   | `AGRIPEDIA_ADMOB_BANNERID` | ID du bloc bannière |

2. **Actions** › **Release (Google Play)** › **Run workflow** (branche `main`).
3. À la fin, télécharger l'artefact **agripedia-release** : `app-release.aab` (pour Google Play), `app-release.apk`
   (pour tester sur un téléphone) et `mapping.txt` (à importer dans la Play Console pour lire les plantages).

- Version **1.0.0**, code de version **1** (`app/build.gradle.kts`). Google Play refuse un code déjà envoyé (même en test
  interne) : **avant chaque nouvel envoi, augmenter `versionCode` de 1**, et `versionName` (1.0.1, 1.1.0…) quand la
  nouvelle version est publiée.
- Alternative sans GitHub : Android Studio › **Build** › **Generate Signed App Bundle** avec la même clé.

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
   `images/feature-graphic-<langue>.png` ; **captures** les 5 fichiers de `images/screenshots-<langue>/`, dans l'ordre.
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

1. **Tester et publier** › **Test interne** › **Créer une version** : importer `app-release.aab`, nom `1.0.0 (1)`,
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

- **Captures** : GitHub › **Actions** › **Store screenshots** › **Run workflow**, puis copier le contenu de l'artefact
  `play-store-images` dans `images/`.
- **Icône et bannières** seulement, en local (Node + Playwright) :

```bash
mkdir -p play-store/build/fonts && cd play-store/build/fonts
npm pack @fontsource/figtree@5.2.8 @fontsource/noto-kufi-arabic@5.2.8
for f in *.tgz; do mkdir -p "${f%.tgz}" && tar -xzf "$f" -C "${f%.tgz}"; done
cd ../../..
npm install --no-save playwright@1.56.1 && npx playwright install chromium
node play-store/scripts/compose-images.js
```
