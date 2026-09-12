MCP Server transforme un appareil Android exécutant AutoJs6 en serveur [Model Context Protocol](https://modelcontextprotocol.io). Les agents IA sur PC, tels que Claude Code, Cursor ou MCP Inspector, se connectent au téléphone par USB ou Wi-Fi et utilisent des outils pour exécuter des scripts, lire les journaux, inspecter l'arbre des noeuds d'accessibilité, toucher et saisir du texte, prendre des captures d'écran et manipuler des fichiers et des applications.

Aperçu de développement jusqu'à P3.3: le point MCP authentifié, l'appairage, les outils de script, d'interface et de capture sont implémentés. screen_capture renvoie des images JPEG, PNG ou WebP; screen_state indique les dimensions et l'orientation. Le commutateur du tiroir et les réglages sont prévus en P4. Voir [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) pour les progrès et les essais sur appareils.

### Utilisation

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) sur un appareil disposant d'AutoJs6 build 5279 (6.8.0) ou ultérieur.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `MCP Server` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Pour cet aperçu, utilisez les commandes adb et une session de test hôte décrites dans les notes de développement; le commutateur du tiroir et les réglages du plugin sont prévus en P4.
4. Sur le PC, exécutez `adb forward tcp:9637 tcp:9637` et pointez le client MCP vers `http://127.0.0.1:9637/mcp` avec le jeton comme identifiant bearer.

Consultez le [README du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) et [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) pour le guide de connexion et l'avancement actuel.
