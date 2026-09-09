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
* `Dépendance` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) sur le moteur Ktor 3.5.1 CIO
* `Dépendance` Ajout de Ktor 3.5.1 `ktor-server-test-host` pour les tests de transport JVM (portee de test uniquement)
