******

### Historique des versions

******

# v1.0.0

###### 2026/09/10

* `Note` Aperçu de développement : le plugin s'enregistre auprès du centre de plugins d'AutoJs6, mais le point de terminaison MCP et ses outils ne sont pas encore disponibles
* `Fonctionnalité` Identité de plugin `mcp-server` avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.MCP_SERVER` pour la découverte par l'hôte
* `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
* `Fonctionnalité` Point de terminaison Streamable HTTP sur `http://127.0.0.1:9637/mcp` avec l'outil `device_ping`, hébergé par un service de premier plan que adb ou l'hôte peut activer et désactiver (aperçu de développement)
* `Fonctionnalité` Renforcement du transport du point de terminaison `/mcp` : l'adresse et le port de liaison proviennent du magasin de configuration du serveur, le corps des requetes est limite a 1 MiB, les connexions inactives se ferment apres 60 s, et un port deja occupe ou une liaison refusee se termine par un etat `port_in_use` / `bind_failed` avec une indication au lieu d'un plantage
* `Fonctionnalité` Protection contre le DNS rebinding devant le transport du SDK : le mode loopback n'accepte que `localhost` / `127.0.0.1` / `[::1]` comme `Host`, le mode LAN ajoute les adresses IPv4 actuelles de l'appareil et des noms d'hote supplementaires facultatifs et les actualise au changement de reseau ; les origines de navigateur sont refusees sauf si le commutateur "mode developpeur" admet l'origine loopback de l'Inspector via CORS
* `Fonctionnalité` Identite du serveur `autojs6-mcp-server` avec la version du plugin et les capacites tools (`listChanged`), resources et prompts ; `tools/list` conserve l'ordre d'enregistrement pour que les clients puissent le mettre en cache
* `Fonctionnalité` Authentification par jeton bearer pour chaque requete `/mcp` : un jeton de 32 octets genere au premier demarrage, enveloppe avec une cle AES-GCM de l'Android Keystore et conserve dans le stockage prive du plugin, jamais sauvegarde ; un en-tete `Authorization` absent ou incorrect est refuse apres une comparaison en temps constant par `401` + `WWW-Authenticate: Bearer` et une erreur JSON-RPC `-32001` ; le jeton n'atteint jamais le journal
* `Fonctionnalité` Appairage a la premiere utilisation devant le transport : un client non appaire peut faire `initialize` et lister tools, resources et prompts, mais son premier `tools/call`, `resources/read`, `resources/subscribe` ou `prompts/get` recoit `PAIRING_REQUIRED` (`-32002`) jusqu'a la confirmation sur le telephone dans les 60 s ; un refus ou un delai depasse recoit `PAIRING_DENIED` (`-32003`) pendant 30 s ; le client est identifie par le nom de `clientInfo` (ou `User-Agent`) et la classe d'adresse (loopback / LAN), si bien qu'une rotation du jeton conserve les appairages, et jusqu'a 32 clients peuvent etre appaires
* `Fonctionnalité` Confirmation de l'appairage sur le telephone par deux canaux : une notification a haute priorite avec les actions Autoriser / Refuser, plus une boite de dialogue tant que l'ecran est deverrouille ; la configuration du serveur, le jeton et les clients appaires vivent dans des fichiers remplaces atomiquement que le processus du serveur et la page des parametres partagent sans cache perime
* `Dépendance` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) sur le moteur Ktor 3.5.1 CIO
* `Dépendance` Ajout de Ktor 3.5.1 `ktor-server-test-host` pour les tests de transport JVM (portee de test uniquement)
