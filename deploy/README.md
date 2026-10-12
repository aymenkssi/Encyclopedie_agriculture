# Site d'AgriPedia sur le VPS

Un conteneur nginx, derrière le **Traefik déjà en place** sur le VPS (le même que les apps Umrah et Halal & Mosque Finder),
sert le dossier `site/` du dépôt. L'application n'a **pas d'API** : l'encyclopédie est embarquée dans l'APK.

| Adresse | Contenu |
|---|---|
| `https://agripedia.creationapp.academy/` | Page d'accueil |
| `https://agripedia.creationapp.academy/privacy` | Politique de confidentialité (EN, FR, AR), ouverte depuis l'app (Profil) et déclarée sur Google Play |
| `https://agripedia.creationapp.academy/app-ads.txt` | Fichier AdMob (`google.com, pub-7488746561313974, DIRECT, f08c47fec0942fa0`) |

## 1. DNS

Dans la zone DNS de `creationapp.academy`, ajoutez :

| Type | Nom | Valeur |
|---|---|---|
| A | `agripedia` | IP du VPS (la même que `halalmosquefinder`) |

Vérifiez avec `ping agripedia.creationapp.academy` avant de lancer : Traefik en a besoin pour le certificat HTTPS.

## 2. Installer et lancer

```bash
cd /opt/apps
git clone https://github.com/aymenkssi/Encyclopedie_agriculture.git agripedia   # première fois
cd agripedia && git pull
cd deploy
cp .env.example .env                     # valeurs identiques aux autres apps
docker compose up -d
docker compose ps                        # le service "web" doit être "healthy"
curl https://agripedia.creationapp.academy/app-ads.txt
# google.com, pub-7488746561313974, DIRECT, f08c47fec0942fa0
```

Le projet Docker s'appelle `agripedia` et son routeur Traefik `agripedia` : aucun conflit avec les autres apps.

## 3. Mettre à jour le site

Les pages viennent directement du dossier `site/` : après un `git pull`, elles sont en ligne sans redémarrage.

## Dépannage

| Symptôme | Cause et solution |
|---|---|
| `curl` : *self-signed certificate* et `curl -k` renvoie `404 page not found` | Traefik ignore le conteneur : vérifier `docker compose ps` (il doit être `healthy`), sinon `docker inspect --format '{{json .State.Health}}' agripedia-web-1`. |
| `curl -k` fonctionne mais le certificat reste auto-signé | Certificat Let's Encrypt pas encore émis : vérifier le DNS, attendre une minute, `docker restart traefik`, lire `docker logs traefik`. |
