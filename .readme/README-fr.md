<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Expose l'automatisation de l'appareil aux agents IA via le Model Context Protocol</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### Introduction

******

MCP Server transforme un appareil Android exécutant AutoJs6 en serveur [Model Context Protocol](https://modelcontextprotocol.io). Les agents IA sur PC, tels que Claude Code, Cursor ou MCP Inspector, se connectent au téléphone par USB ou Wi-Fi et utilisent des outils pour exécuter des scripts, lire les journaux, inspecter l'arbre des noeuds d'accessibilité, toucher et saisir du texte, prendre des captures d'écran et manipuler des fichiers et des applications.

Le serveur s'exécute dans le processus propre du plugin et est joignable via un unique point de terminaison Streamable HTTP. AutoJs6 fournit au plugin un courtier de capacités via Binder, de sorte que chaque appel d'outil est exécuté par l'hôte avec ses permissions, ses moteurs et son service d'accessibilité existants ; le plugin ne duplique jamais les fonctionnalités de l'hôte.

******

### État

******

Le projet est au stade du squelette : cette version enregistre le plugin auprès du centre de plugins d'AutoJs6 et prépare l'infrastructure de build, de documentation et de tests. Le point de terminaison MCP et ses outils ne sont pas encore disponibles. L'avancement est suivi point par point dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Fonctionnalités prévues

******

La feuille de route livre les capacités suivantes par étapes:

- Exécution de scripts : exécuter du JavaScript depuis un texte ou un fichier dans AutoJs6, lister et arrêter les moteurs, et lire la sortie récente de la console.
- Interface d'accessibilité : exporter l'arbre des noeuds dans un format texte compact, trouver des noeuds avec la syntaxe de sélecteur d'AutoJs6, cliquer, appuyer longuement, faire défiler, saisir du texte et déclencher des touches globales comme Retour et Accueil.
- Captures d'écran : capturer l'écran en PNG ou JPEG avec une taille plafonnée adaptée aux modèles multimodaux.
- Fichiers, applications et appareil : lire et écrire des fichiers dans le répertoire de travail d'AutoJs6, lancer des applications, interroger la fenêtre au premier plan et rapporter les informations de l'appareil.
- Chemins de connexion : USB via `adb forward`, réseau local avec activation explicite, pont stdio côté PC et tunnel public optionnel avec OAuth 2.1.
- Sécurité : jeton bearer renouvelable, confirmation d'appairage à la première utilisation sur le téléphone et interrupteurs d'outils par groupe ; le serveur n'écoute par défaut que sur l'interface de bouclage.

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) sur un appareil disposant d'AutoJs6 build 5279 (6.8.0) ou ultérieur.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `MCP Server` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Activez le serveur MCP depuis le tiroir d'AutoJs6 ou la page de réglages du plugin ; le téléphone affiche l'adresse du point de terminaison et le jeton d'appairage.
4. Sur le PC, exécutez `adb forward tcp:9637 tcp:9637` et pointez le client MCP vers `http://127.0.0.1:9637/mcp` avec le jeton comme identifiant bearer.

> Les étapes 3 et 4 décrivent le flux prévu et deviennent disponibles une fois les phases correspondantes de la feuille de route terminées. Le plugin prend en charge Android 7.0 (API 24) ou ultérieur.

******

### Configuration du client

******

Claude Code enregistre le serveur en une seule commande ; les autres clients utilisent la même URL et le même en-tête dans leur configuration MCP:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

Remplacez le jeton par la valeur affichée sur le téléphone. La commande ne fonctionne qu'une fois le serveur démarrable (voir `État`).

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entrée Binder sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, si bien que seul AutoJs6 peut s'y lier.
- La permission INTERNET ne sert qu'à l'écouteur HTTP propre du plugin ; le plugin n'émet aucune requête sortante et ne collecte aucune donnée.
- Les appels d'outils passent par le courtier de capacités d'AutoJs6 et ne dépassent jamais ce que l'hôte lui-même est autorisé à faire ; les groupes dangereux comme les commandes shell et la suppression de fichiers restent désactivés tant que l'utilisateur ne les active pas.
- Les sauvegardes sont désactivées et les jetons ne sont stockés que dans l'espace privé du plugin.

N'obtenez le plugin que depuis la page officielle [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) ou le centre de plugins d'AutoJs6. Les paquets de sources inconnues peuvent échouer à la vérification de l'hôte ou présenter des risques même lorsque le numéro de version semble identique.

******

### Interface du plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins ; l'hôte utilise ces identifiants pour découvrir le plugin et négocier la compatibilité:

```text
application id: io.github.supermonster003.autojs6.plugin.mcp.server
plugin id: mcp-server
engine: mcp-server
variant: default
service action: org.autojs.plugin.MCP_SERVER
service category: mcp-server
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.mcp.server.api.IMcpServerPlugin
minimum host build: 5279 (6.8.0)
default endpoint: http://127.0.0.1:9637/mcp
```

`McpServerPluginService` répond à l'action `org.autojs.plugin.MCP_SERVER` (catégorie `mcp-server`) et s'exécute dans le processus `:mcp_server`. Le contrat AIDL `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` est défini par l'hôte dans son module `mcp-server-api` et arrive avec la phase P1 de la feuille de route ; d'ici là, le service n'expose que le descripteur du contrat. `McpServerPluginInfoService` répond à `org.autojs.plugin.INFO` avec le `PluginInfo` standard, et `WakeActivity` permet à l'hôte de réveiller le processus du plugin sur les appareils qui maintiennent les applications nouvellement installées à l'arrêt.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/09/07_

- `Note` Aperçu de développement : le plugin s'enregistre auprès du centre de plugins d'AutoJs6, mais le point de terminaison MCP et ses outils ne sont pas encore disponibles
- `Fonctionnalité` Identité de plugin `mcp-server` avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.MCP_SERVER` pour la découverte par l'hôte
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Fonctionnalité` Point de terminaison Streamable HTTP sur `http://127.0.0.1:9637/mcp` avec l'outil `device_ping`, hébergé par un service de premier plan que adb ou l'hôte peut activer et désactiver (aperçu de développement)
- `Dépendance` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) sur le moteur Ktor 3.5.1 CIO

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation et vérification

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources ; les utilisateurs ordinaires peuvent simplement installer l'APK préconstruit depuis la page Releases.

Compiler un APK de débogage:

```powershell
.\gradlew.bat :app:assembleDebug
```

Exécuter les tests unitaires JVM et compiler l'APK de tests d'instrumentation:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compiler l'APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collecter l'artefact de release et ajouter la version et le condensé CRC32 à son nom de fichier:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Vérifier que les sources de documentation multilingues et les artefacts générés sont synchronisés (également appliqué par la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilation nécessite JDK 21 ou ultérieur et Android SDK 36 ; les versions de Gradle et des plugins sont gérées de manière centralisée par `version.properties` et `io.github.supermonster003.autojs6-platform-versions`.

******

### Localisation et génération de la documentation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Les fichiers JSON de langue sous `.readme/` et `.changelog/` sont la source unique du README, des instructions du centre de plugins et du journal des modifications. Modifiez toujours ces sources JSON et relancez `py .python/generate_markdown.py` ; les artefacts README, `plugin_instruction.md` et journal des modifications générés ne sont jamais édités à la main. Exécutez `py .python/generate_markdown.py --check` pour vérifier tous les artefacts générés.

******

### Licence

******

Le code du projet est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE). Les composants tiers et leurs licences sont listés dans les [Avis de tiers](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Spécification du Model Context Protocol: https://modelcontextprotocol.io
- Avis de tiers: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md
