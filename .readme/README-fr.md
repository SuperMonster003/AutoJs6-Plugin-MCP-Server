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

Aperçu P4: 37 outils, dont 33 activés par défaut, avec un bouton dans le volet AutoJs6 et une page de paramètres du plugin. Nécessite la version AutoJs6 P4 correspondante. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Fonctionnalités prévues

******

La feuille de route livre les capacités suivantes par étapes:

- Paramètres du téléphone pour l'état, USB, le port et le réseau local, les jetons, la révocation des associations, les groupes et root, le mode développeur, les configurations copiables Claude Code / Cursor / Codex / HTTP et l'historique, avec l'apparence AutoJs6. Les changements réseau redémarrent le serveur actif; jetons et autorisations prennent effet immédiatement. Les fenêtres secrètes bloquent les captures.
- Exécution de scripts : exécuter du JavaScript depuis un texte ou un fichier dans AutoJs6, lister et arrêter les moteurs, et lire la sortie récente de la console.
- Interface d'accessibilité : exporter l'arbre des noeuds dans un format texte compact, trouver des noeuds avec la syntaxe de sélecteur d'AutoJs6, cliquer, appuyer longuement, faire défiler, saisir du texte et déclencher des touches globales comme Retour et Accueil.
- Groupe de capture (P3.3): screen_capture renvoie des images MCP avec recadrage, scale ou maxWidth, JPEG / PNG / WebP et qualité réglable. Valeurs par défaut: JPEG, qualité 70, côté le plus long de 1280 px. Au-delà de 4 MiB de base64, la qualité ou les dimensions diminuent et les métadonnées indiquent les ajustements. screen_state fournit l'état, les dimensions, l'orientation et la densité. Le catalogue compte 37 outils. Le repli MediaProjection nécessite AutoJs6 compilé le 2026-09-13 ou après et un accord sur le téléphone, réutilisé par la session hôte.
- Outils du répertoire de travail (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open avec ligne et colonne à partir de 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast et shell_exec. Lecture binaire en base64, au plus 1 MiB de données brutes. Les écritures respectent aussi le budget du host (normalement 96 KiB avec les échappements JSON). Suppression et Shell sont désactivés par défaut; root exige allowShellRoot et une autorisation shell.root du host. La version P3.4 correspondante du host est nécessaire.
- Les ressources MCP (P3.5) donnent accès en lecture seule aux fichiers de travail, aux exemples du serveur hôte, aux informations de l'appareil et à la console récente, selon l'appairage et les groupes actifs. Les lectures de texte et de données binaires signalent toute troncature. Les modèles write_autojs6_script, automate_task et debug_selector proposent des instructions en anglais et en chinois, avec repli en anglais pour les autres langues du téléphone.
- Chemins de connexion : USB via `adb forward`, réseau local avec activation explicite, pont stdio côté PC et tunnel public optionnel avec OAuth 2.1.
- Sécurité : jeton bearer renouvelable, confirmation d'appairage à la première utilisation sur le téléphone et interrupteurs d'outils par groupe ; le serveur n'écoute par défaut que sur l'interface de bouclage.

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) sur un appareil disposant d'AutoJs6 build 5279 (6.8.0) ou ultérieur.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `MCP Server` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Activez MCP Server dans le volet AutoJs6. Appuyez longuement sur son titre ou ouvrez ses paramètres depuis le Centre de plugins. Copiez la configuration du client PC.
4. Sur le PC, exécutez `adb forward tcp:9637 tcp:9637` et pointez le client MCP vers `http://127.0.0.1:9637/mcp` avec le jeton comme identifiant bearer.
5. Confirmez la première demande d'association sur le téléphone. Arrêtez ensuite le serveur depuis le volet, les paramètres ou la notification.

> Bouton MCP Server avec guides d'installation, d'activation, d'autorisation et de compatibilité; arrêt synchronisé avec la notification; paramètres conservés à la reconnexion; accès vérifié à la même page depuis le volet et le Centre de plugins. Restaure le serveur à l'ouverture d'AutoJs6 sauf arrêt par l'utilisateur en l'absence de l'hôte; aucun lancement au démarrage de l'appareil.

******

### Configuration du client

******

Claude Code enregistre le serveur en une seule commande ; les autres clients utilisent la même URL et le même en-tête dans leur configuration MCP:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

Activez MCP Server dans le volet AutoJs6. Appuyez longuement sur son titre ou ouvrez ses paramètres depuis le Centre de plugins. Copiez la configuration du client PC. Confirmez la première demande d'association sur le téléphone. Arrêtez ensuite le serveur depuis le volet, les paramètres ou la notification.

******

### Chemins de connexion

******

USB: `adb forward tcp:9637 tcp:9637` fait correspondre le port du téléphone au PC; avec plusieurs appareils ajoutez `-s <serial>` (trouvez-le avec `adb devices`), et les émulateurs fonctionnent de la même façon. Si le port est pris d'un côté ou de l'autre, changez-le sur la page des paramètres et redirigez le nouveau. La carte Connexion propose la commande de redirection prête à copier.

Réseau local: activez "Autoriser les connexions du réseau local" sur la page des paramètres. La page liste alors les adresses actuelles du téléphone (elles suivent les changements de Wi-Fi) et rappelle que le client doit rejoindre le même réseau; les réseaux invités, l'isolation du point d'accès et le pare-feu du PC sont les blocages habituels. Les demandes d'appairage venant du réseau local sont signalées comme telles, et une notification quotidienne le rappelle tant que le serveur reste joignable depuis le réseau; le rappel peut être désactivé.

Les deux chemins utilisent le même jeton et le même appairage côté téléphone. Un pont stdio pour les clients sans prise en charge HTTP est prévu séparément.

******

### Clients

******

La page des paramètres copie une configuration prête avec le vrai jeton pour chaque client ci-dessous; les extraits ici utilisent `<token>` comme espace réservé. Tous les clients parlent Streamable HTTP avec un en-tête Authorization, et le premier appel d'un nouveau client est confirmé sur le téléphone. Vérifiés: Claude Code, Codex CLI et MCP Inspector; les autres clients utilisent la même URL et le même en-tête mais n'ont pas encore été testés par le mainteneur.

Claude Code: exécutez la commande indiquée sous "Configuration du client" (la page des paramètres la copie avec le jeton); ensuite `claude mcp list` affiche `autojs6` comme Connected.

Cursor: ajoutez l'entrée ci-dessous à `mcp.json`:

```json
{
  "mcpServers": {
    "autojs6": {
      "url": "http://127.0.0.1:9637/mcp",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

Codex CLI: placez le jeton dans la variable d'environnement `AUTOJS6_MCP_TOKEN` (la page des paramètres copie une commande PowerShell pour cela) et ajoutez le serveur à `config.toml`, ou exécutez `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN`:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: le mode CLI ne demande aucune configuration supplémentaire, et l'interface web atteint le téléphone via son propre proxy Node. N'activez le mode développeur sur la page des paramètres que si une page de navigateur se connecte directement au point de terminaison:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline, VS Code Copilot Chat, Gemini CLI et clients similaires: utilisez la même URL et le même en-tête dans leur configuration MCP; la page des paramètres propose un extrait JSON générique avec `"type": "http"`.

Claude Desktop ne lance que des serveurs stdio; un programme pont est prévu pour lui (voir la feuille de route).

******

### Questions fréquentes

******

- 401 Unauthorized: le jeton est absent, mal saisi ou a été renouvelé. Copiez à nouveau la configuration depuis la page des paramètres; après un renouvellement du jeton, chaque client a besoin de la nouvelle valeur.
- Délai d'appairage dépassé: le premier appel d'un nouveau client attend environ une minute que vous touchiez Autoriser sur le téléphone. Déverrouillez le téléphone, acceptez la boîte de dialogue ou l'action de la notification, puis répétez l'appel. Refuser lance une courte pause, après laquelle l'appel suivant redemande.
- HOST_UNAVAILABLE: AutoJs6 n'est pas lancé ou sa session de plugin est fermée. Ouvrez AutoJs6, laissez l'interrupteur du tiroir activé et vérifiez l'état de connexion sur la page des paramètres.
- Accessibilité désactivée: les outils `ui_*` et la capture d'écran ont besoin du service d'accessibilité d'AutoJs6. Appelez `device_ensure_accessibility` ou activez le service dans les paramètres d'accessibilité du système.
- Port occupé: le tiroir signale `port_in_use`. Changez le port sur la page des paramètres et redirigez le nouveau port avec adb.
- Réseau local injoignable: activez l'accès depuis le réseau local, utilisez une adresse listée sur la page des paramètres, gardez le PC et le téléphone sur le même réseau sans isolation invité, et autorisez le port dans le pare-feu du PC. L'économie d'énergie Wi-Fi du téléphone ajoute quelques centaines de millisecondes par appel.

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

`McpServerPluginService` implémente le contrat mcp-server-api de l'hôte `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` dans le processus `:mcp_server` et répond à `org.autojs.plugin.MCP_SERVER` (category `mcp-server`). `McpServerPluginInfoService` répond à `org.autojs.plugin.INFO` avec PluginInfo. `WakeActivity` permet à l'hôte d'activer le plugin.

******

### Feuille de route

******

Les plans et l'avancement du plugin sont tenus sous forme de liste cochable dans ROADMAP.md, organisée par phase avec des critères d'acceptation et des niveaux de preuve. Les éléments non cochés expriment une intention et non une capacité actuelle ; les discussions via Issues sont les bienvenues.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/09/15_

- `Note` Aperçu P4: 37 outils, dont 33 activés par défaut, avec un bouton dans le volet AutoJs6 et une page de paramètres du plugin. Nécessite la version AutoJs6 P4 correspondante. ROADMAP.md.
- `Fonctionnalité` Paramètres du téléphone pour l'état, USB, le port et le réseau local, les jetons, la révocation des associations, les groupes et root, le mode développeur, les configurations copiables Claude Code / Cursor / Codex / HTTP et l'historique, avec l'apparence AutoJs6. Les changements réseau redémarrent le serveur actif; jetons et autorisations prennent effet immédiatement. Les fenêtres secrètes bloquent les captures.
- `Fonctionnalité` Les ressources MCP (P3.5) donnent accès en lecture seule aux fichiers de travail, aux exemples du serveur hôte, aux informations de l'appareil et à la console récente, selon l'appairage et les groupes actifs. Les lectures de texte et de données binaires signalent toute troncature. Les modèles write_autojs6_script, automate_task et debug_selector proposent des instructions en anglais et en chinois, avec repli en anglais pour les autres langues du téléphone.
- `Fonctionnalité` Outils du répertoire de travail (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open avec ligne et colonne à partir de 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast et shell_exec. Lecture binaire en base64, au plus 1 MiB de données brutes. Les écritures respectent aussi le budget du host (normalement 96 KiB avec les échappements JSON). Suppression et Shell sont désactivés par défaut; root exige allowShellRoot et une autorisation shell.root du host. La version P3.4 correspondante du host est nécessaire.
- `Fonctionnalité` Identité de plugin `mcp-server` avec le service INFO, la Wake Activity et le squelette du service `org.autojs.plugin.MCP_SERVER` pour la découverte par l'hôte
- `Fonctionnalité` README, instructions du centre de plugins et journal des modifications en 10 langues
- `Fonctionnalité` Point de terminaison Streamable HTTP sur `http://127.0.0.1:9637/mcp` avec l'outil `device_ping`, hébergé par un service de premier plan que adb ou l'hôte peut activer et désactiver (aperçu de développement)
- `Fonctionnalité` Renforcement du transport du point de terminaison `/mcp` : l'adresse et le port de liaison proviennent du magasin de configuration du serveur, le corps des requetes est limite a 1 MiB, les connexions inactives se ferment apres 60 s, et un port deja occupe ou une liaison refusee se termine par un etat `port_in_use` / `bind_failed` avec une indication au lieu d'un plantage
- `Fonctionnalité` Protection contre le DNS rebinding devant le transport du SDK : le mode loopback n'accepte que `localhost` / `127.0.0.1` / `[::1]` comme `Host`, le mode LAN ajoute les adresses IPv4 actuelles de l'appareil et des noms d'hote supplementaires facultatifs et les actualise au changement de reseau ; les origines de navigateur sont refusees sauf si le commutateur "mode developpeur" admet l'origine loopback de l'Inspector via CORS
- `Fonctionnalité` Identite du serveur `autojs6-mcp-server` avec la version du plugin et les capacites tools (`listChanged`), resources et prompts ; `tools/list` conserve l'ordre d'enregistrement pour que les clients puissent le mettre en cache
- `Fonctionnalité` Authentification par jeton bearer pour chaque requete `/mcp` : un jeton de 32 octets genere au premier demarrage, enveloppe avec une cle AES-GCM de l'Android Keystore et conserve dans le stockage prive du plugin, jamais sauvegarde ; un en-tete `Authorization` absent ou incorrect est refuse apres une comparaison en temps constant par `401` + `WWW-Authenticate: Bearer` et une erreur JSON-RPC `-32001` ; le jeton n'atteint jamais le journal
- `Fonctionnalité` Appairage a la premiere utilisation devant le transport : un client non appaire peut faire `initialize` et lister tools, resources et prompts, mais son premier `tools/call`, `resources/read`, `resources/subscribe` ou `prompts/get` recoit `PAIRING_REQUIRED` (`-32002`) jusqu'a la confirmation sur le telephone dans les 60 s ; un refus ou un delai depasse recoit `PAIRING_DENIED` (`-32003`) pendant 30 s ; le client est identifie par le nom de `clientInfo` (ou `User-Agent`) et la classe d'adresse (loopback / LAN), si bien qu'une rotation du jeton conserve les appairages, et jusqu'a 32 clients peuvent etre appaires
- `Fonctionnalité` Confirmation de l'appairage sur le telephone par deux canaux : une notification a haute priorite avec les actions Autoriser / Refuser, plus une boite de dialogue tant que l'ecran est deverrouille ; la configuration du serveur, le jeton et les clients appaires vivent dans des fichiers remplaces atomiquement que le processus du serveur et la page des parametres partagent sans cache perime
- `Fonctionnalité` Catalogue d'outils avec interrupteurs par groupe (decision D6) : `device_ping` (local au plugin), `device_info` (`device.info` d'AutoJs6) et `script_run` (`engines.execScript` d'AutoJs6 : execute du JavaScript, attend jusqu'a `timeoutMs` qu'il se termine et renvoie le resultat avec les dernieres lignes de console, en envoyant des notifications de progression pendant l'execution) ; chaque outil declare un JSON Schema ferme (`additionalProperties: false`) et ses arguments sont valides avant que quoi que ce soit n'atteigne AutoJs6 ; les interrupteurs de groupe `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` sont conserves dans `tool_groups.json`, un groupe desactive disparait de `tools/list` des la requete suivante et ses outils repondent `TOOL_DISABLED`
- `Fonctionnalité` Pont vers l'hote : le service `org.autojs.plugin.MCP_SERVER` implemente le vrai Binder `IMcpServerPlugin` (`getInfo` / `getCapabilities` annoncent la version de contrat 1, les groupes d'outils, les versions du protocole MCP et la version du SDK ; `openServer` n'admet que l'AutoJs6 installe avec la meme signature et renvoie un `IMcpServerSession` avec `getStatus` / `updateConfig` / `stop` / `close`) ; les appels d'outils passent par le broker de capacites de l'hote avec des identifiants de requete monotones, un delai par appel, le plafond de 4 appels simultanes et les categories d'erreur de l'hote traduites en `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR` ; quand AutoJs6 meurt, l'ecouteur continue de tourner et les outils dependant de l'hote repondent `HOST_UNAVAILABLE` jusqu'a sa reconnexion ; l'etat et les evenements (`pairing_requested`, `client_paired`, `tool_call`, `warning`) parviennent a l'hote par son rappel
- `Fonctionnalité` La notification du service de premier plan affiche le point d'acces, l'etat de connexion d'AutoJs6 et le nombre de clients appaires, avec une action Arreter ; un toast indique le point d'acces quand les notifications sont bloquees ; `dumpsys activity service` imprime en plus la session de l'hote, les interrupteurs de groupe et les outils enregistres
- `Fonctionnalité` Groupe de scripts complete : `script_run_file` execute un fichier de script present sur l'appareil, `script_stop` / `script_stop_all` arretent une ou toutes les executions d'AutoJs6, `script_list` liste celles en cours et `console_tail` renvoie les dernieres lignes de la console avec un curseur `nextSinceId` et un filtre par niveau ; `script_run` et `script_run_file` renvoient desormais `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, l'exception avec sa ligne et les dernieres lignes de la console, et pendant l'attente une notification de progression part toutes les 2 s avec la derniere ligne de la console
- `Fonctionnalité` Les reponses du point de terminaison MCP sont diffusees en flux d'evenements envoyes par le serveur (le mode de reponse JSON du SDK n'est pas utilise), si bien qu'une notification liee a une requete, comme le battement de progression d'un script en cours, parvient au client dans la reponse de cette requete
- `Fonctionnalité` Groupe UI ajoute (roadmap P3.2) : `ui_dump` renvoie la fenetre active sous forme d'arbre compact de noeuds avec des references `#n` (`format` text / json / xml, `maxNodes` jusqu'a 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` interrogent un selecteur, `ui_current_window` et `ui_explain_selector` indiquent la fenetre et la raison de l'echec d'un selecteur, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` agissent sur un `nodeRef` (relocalise par son empreinte, `NODE_REF_STALE` s'il a disparu) ou un `selector`, `ui_press_key` appuie sur back / home / recents / notifications / quick_settings / power_dialog / lock_screen, et le groupe `ui_gesture` (desactive par defaut) ajoute `ui_swipe`, `ui_gesture` et la forme par coordonnees des outils de clic (`TOOL_DISABLED` tant que le groupe est desactive) ; l'instantane du catalogue d'outils passe a 20 outils ; les gestes par coordonnees necessitent un hote AutoJs6 compile le 2026-09-11 ou apres (un hote plus ancien repond au hasard "the system cancelled ...")
- `Fonctionnalité` Groupe de capture (P3.3): screen_capture renvoie des images MCP avec recadrage, scale ou maxWidth, JPEG / PNG / WebP et qualité réglable. Valeurs par défaut: JPEG, qualité 70, côté le plus long de 1280 px. Au-delà de 4 MiB de base64, la qualité ou les dimensions diminuent et les métadonnées indiquent les ajustements. screen_state fournit l'état, les dimensions, l'orientation et la densité. Le catalogue compte 22 outils. Le repli MediaProjection nécessite AutoJs6 compilé le 2026-09-13 ou après et un accord sur le téléphone, réutilisé par la session hôte.
- `Fonctionnalité` Chemin par réseau local (P5.1): avec l'accès depuis le réseau local activé, la page des paramètres liste les adresses actuelles du téléphone (mises à jour quand le Wi-Fi change) avec des rappels sur le même réseau et le pare-feu; une demande d'appairage venant du réseau local est signalée dans la boîte de dialogue et la notification; un rappel quotidien indique que le serveur reste joignable depuis le réseau local et peut être désactivé sans redémarrer l'écouteur. Le README documente les chemins USB et réseau local.
- `Correctif` Le rebuild de l'IDE ne recherche plus d'APK pour les tests unitaires JVM. Les tâches de vérification des APK assemblent automatiquement leurs entrées et fonctionnent après un clean.
- `Correctif` Les proportions de l'icône du centre de plugins variaient entre les modes clair et sombre; le mode nuit utilise aussi l'icône adaptative, avec des couches redimensionnées pour conserver le dessin complet et les marges de ic_launcher_round.png, seul le fond changeant de couleur
- `Correctif` La navigation au clavier avec Tab sur la page des paramètres ignorait le bouton retour de la barre d'outils; le cycle Tab couvre désormais le bouton retour et tous les contrôles, y compris sur Android 7. Les tests sur appareil vérifient les libellés du lecteur d'écran et l'utilisation au clavier.
- `Correctif` La carte arguments de script_run et script_run_file déclarait le type de ses valeurs comme un tableau JSON Schema que certains clients MCP rejettent ou affaiblissent; le schéma utilise désormais des branches anyOf à type unique. Le README gagne les sections Clients et Questions fréquentes avec la matrice des clients testés.
- `Amélioration` La vérification de compilation rejette les dépendances natives involontaires et produit un rapport JSON
- `Dépendance` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) sur le moteur Ktor 3.5.1 CIO
- `Dépendance` Ajout de Ktor 3.5.1 `ktor-server-test-host` pour les tests de transport JVM (portee de test uniquement)
- `Dépendance` Ajout de `mcp-server-api.aar` (module `plugin-api/mcp-server-api` d'AutoJs6, build de l'hote 6.8.0 / 5279, MPL 2.0) comme contrat Binder entre AutoJs6 et le plugin, empreinte verrouillee dans `locks/host-api-aars.lock`
- `Dépendance` Mise à jour des AAR common-plugin-api et mcp-server-api de l'hôte P4: extension facultative de paramètres v1, ordre AIDL inchangé, SHA-256 verrouillés et compatibilité SDK 36.

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


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
