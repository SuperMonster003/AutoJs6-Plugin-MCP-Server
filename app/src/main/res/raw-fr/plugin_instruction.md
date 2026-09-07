MCP Server transforme un appareil Android exécutant AutoJs6 en serveur [Model Context Protocol](https://modelcontextprotocol.io). Les agents IA sur PC, tels que Claude Code, Cursor ou MCP Inspector, se connectent au téléphone par USB ou Wi-Fi et utilisent des outils pour exécuter des scripts, lire les journaux, inspecter l'arbre des noeuds d'accessibilité, toucher et saisir du texte, prendre des captures d'écran et manipuler des fichiers et des applications.

Le projet est au stade du squelette : cette version enregistre le plugin auprès du centre de plugins d'AutoJs6 et prépare l'infrastructure de build, de documentation et de tests. Le point de terminaison MCP et ses outils ne sont pas encore disponibles. L'avancement est suivi point par point dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) sur un appareil disposant d'AutoJs6 build 5279 (6.8.0) ou ultérieur.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `MCP Server` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Activez le serveur MCP depuis le tiroir d'AutoJs6 ou la page de réglages du plugin ; le téléphone affiche l'adresse du point de terminaison et le jeton d'appairage.
4. Sur le PC, exécutez `adb forward tcp:9637 tcp:9637` et pointez le client MCP vers `http://127.0.0.1:9637/mcp` avec le jeton comme identifiant bearer.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) pour le guide de connexion et l'avancement actuel.
