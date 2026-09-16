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

Version 1.0.2: 37 outils (33 activés par défaut), ressources et invites MCP, un bouton dans le volet AutoJs6 et une page de paramètres du plugin. Nécessite AutoJs6 6.8.0 (build 5279) ou plus récent; les ressources optionnelles autojs6://docs/ demandent aussi le plugin AutoJs6 Offline Docs et un hôte doté de ses méthodes de relais. L'avancement et les preuves sont consignés dans [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Fonctionnalités

******

Le plugin fournit les capacités suivantes:

- Paramètres du téléphone pour l'état, USB, le port et le réseau local, les jetons, la révocation des associations, les groupes et root, le mode développeur, les configurations copiables Claude Code / Cursor / Codex / HTTP et l'historique, avec l'apparence AutoJs6. Les changements réseau redémarrent le serveur actif; jetons et autorisations prennent effet immédiatement. Les fenêtres secrètes bloquent les captures.
- Exécution de scripts : exécuter du JavaScript depuis un texte ou un fichier dans AutoJs6, lister et arrêter les moteurs, et lire la sortie récente de la console.
- Interface d'accessibilité : exporter l'arbre des noeuds dans un format texte compact, trouver des noeuds avec la syntaxe de sélecteur d'AutoJs6, cliquer, appuyer longuement, faire défiler, saisir du texte et déclencher des touches globales comme Retour et Accueil.
- Groupe de capture (P3.3): screen_capture renvoie des images MCP avec recadrage, scale ou maxWidth, JPEG / PNG / WebP et qualité réglable. Valeurs par défaut: JPEG, qualité 70, côté le plus long de 1280 px. Au-delà de 4 MiB de base64, la qualité ou les dimensions diminuent et les métadonnées indiquent les ajustements. screen_state fournit l'état, les dimensions, l'orientation et la densité. Le catalogue compte 37 outils. Le repli MediaProjection nécessite AutoJs6 compilé le 2026-09-13 ou après et un accord sur le téléphone, réutilisé par la session hôte.
- Outils du répertoire de travail (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open avec ligne et colonne à partir de 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast et shell_exec. Lecture binaire en base64, au plus 1 MiB de données brutes. Les écritures respectent aussi le budget du host (normalement 96 KiB avec les échappements JSON). Suppression et Shell sont désactivés par défaut; root exige allowShellRoot et une autorisation shell.root du host. La version P3.4 correspondante du host est nécessaire.
- Les ressources MCP (P3.5) donnent accès en lecture seule aux fichiers de travail, aux exemples du serveur hôte, à la documentation hors ligne lorsque le plugin AutoJs6 Offline Docs est installé, aux informations de l'appareil et à la console récente, selon l'appairage et les groupes actifs. Les lectures de texte et de données binaires signalent toute troncature. Les modèles write_autojs6_script, automate_task et debug_selector proposent des instructions en anglais et en chinois, avec repli en anglais pour les autres langues du téléphone.
- Chemins de connexion : USB via `adb forward`, réseau local avec activation explicite et pont stdio côté PC pour les clients sans transport HTTP.
- Sécurité : jeton bearer renouvelable, confirmation d'appairage à la première utilisation sur le téléphone et interrupteurs d'outils par groupe ; le serveur n'écoute par défaut que sur l'interface de bouclage.

******

### Outils

******

Le tableau ci-dessous est généré à partir de l'instantané du catalogue d'outils du plugin (`app/src/test/resources/tool-catalog.snapshot.json`); les descriptions sont les textes anglais reçus par les clients et chaque groupe peut être désactivé depuis la page de paramètres:

| Outil | Groupe | Par défaut | Description |
|---|---|---|---|
| `device_ping` | `device` | activé | Confirms that the AutoJs6 MCP Server plugin is reachable and returns its version, the device model, the Android API level, and the device time. |
| `device_info` | `device` | activé | Returns the device build, screen, battery, memory, AutoJs6 host version and process, accessibility service state, screen state, locale, and time zone as AutoJs6 reports them (schema autojs6-bridge-device-info-v1). No hardware identifiers. |
| `script_run` | `script` | activé | Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API) and by default waits for it to finish. Use it for automation steps: toasts, UI actions, file work, app launches. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_run_file` | `script` | activé | Runs a script file that already exists on the device (AutoJs6 picks the engine from the suffix) and by default waits for it to finish. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_stop` | `script` | activé | Stops one running AutoJs6 script by the executionId that script_run, script_run_file, or script_list reported. |
| `script_stop_all` | `script` | activé | Stops every script AutoJs6 is running, including ones started on the phone, and returns how many were stopped. |
| `script_list` | `script` | activé | Lists the scripts AutoJs6 is running or starting, with executionId, name, path, working directory, state, and uptime. |
| `console_tail` | `script` | activé | Returns the newest lines of the AutoJs6 console, which every script shares; optionally only entries after sinceId or at least a level. nextSinceId in the result continues from where this call ended. |
| `ui_dump` | `ui` | activé | Dumps the accessibility node tree of the active window as compact text: one node per line with a #n reference, an indent per depth, the short class name, the state markers that apply (clickable, long_clickable, checkable, checked, scrollable, editable, focused, selected, !enabled, hidden), the text in quotes, desc=, id= (name part only), and the position (bounds [l,t][r,b] for a node with children, c=(x,y) for a leaf). Pass a #n reference as nodeRef to ui_click, ui_long_click, ui_set_text, or ui_scroll; references stay valid until the next ui_dump or for 60 s. Call it before acting and again after the screen changed. format json returns the nodes as objects with every flag; format xml returns the uiautomator-style export. |
| `ui_find` | `ui` | activé | Finds the nodes of the active window that match every condition of the selector, optionally waiting up to timeoutMs for the first match, and returns up to limit of them with #n references, bounds, and center. An empty count is not an error; ui_explain_selector tells which condition fails. |
| `ui_current_window` | `ui` | activé | Returns the package and activity in the foreground, whether the AutoJs6 accessibility service is available, and the accessibility windows with their type, title, bounds, and focus. |
| `ui_explain_selector` | `ui` | activé | Explains why a selector matches or not: evaluates its conditions one by one over the active window and reports how many nodes pass each step cumulatively, the first failing condition, the matches, and the near misses. Use it when ui_find returns nothing. |
| `ui_wait_for` | `ui` | activé | Waits until a node matching the selector appears (default) or disappears, polling the active window every 0.5 s for up to timeoutMs, and answers TIMEOUT when the state is not reached. Use it after an action that opens a screen or dismisses a dialog. |
| `ui_click` | `ui` | activé | Clicks a node given by nodeRef (a #n reference from the last ui_dump), by selector (the first match in pre-order), or by x and y (a coordinate tap, allowed only while the ui_gesture group is enabled). The accessibility click climbs to the nearest clickable ancestor when the node itself is not clickable. Returns the node it acted on. Give nodeRef or selector, not both. |
| `ui_long_click` | `ui` | activé | Long-presses a node given by nodeRef or selector (the accessibility long click climbs to the nearest node that accepts it), or by x and y as a 700 ms press at that point (allowed only while the ui_gesture group is enabled). Give nodeRef or selector, not both. |
| `ui_set_text` | `ui` | activé | Sets the text of an editable node (an EditText, marked editable by ui_dump) given by nodeRef or selector; append adds to the current text instead of replacing it. Works without focus or the keyboard; ACTION_FAILED means the node is not editable or not enabled. Give nodeRef or selector, not both. |
| `ui_scroll` | `ui` | activé | Scrolls a node given by nodeRef or selector, or the first scrollable node of the window when neither is given: forward, down, and right move towards the end, backward, up, and left towards the start; times repeats the step. performed counts the steps the node accepted, fewer than requested means it reached the end. Give nodeRef or selector, not both. |
| `ui_press_key` | `ui` | activé | Presses a global key through the accessibility service: back, home, recents, notifications (opens the notification shade), quick_settings, power_dialog, or lock_screen (Android 9 or later). |
| `ui_swipe` | `ui_gesture` | désactivé | Swipes one finger from (x1, y1) to (x2, y2) in device pixels over durationMs; take the coordinates from ui_dump bounds or a screenshot. Part of the ui_gesture group, which is off by default. |
| `ui_gesture` | `ui_gesture` | désactivé | Performs a free-path one-finger gesture through the given points over durationMs (at most 10 s): the first point is the touch down, the last the lift. Part of the ui_gesture group, which is off by default. |
| `screen_capture` | `screen` | activé | Capture the phone screen as an MCP image with dimensions, size, duration and capture source. Uses accessibility on Android 11+ and falls back to MediaProjection, which requires consent on the phone the first time. Defaults to JPEG quality 70 and a longest edge of 1280 pixels. Choose scale or maxWidth to override the size. Images above the 4 MiB base64 limit are retried at lower quality or smaller dimensions; metadata reports adjustments. At most 30 captures per minute per client; a RATE_LIMITED result names the wait in retryAfterMs. |
| `screen_state` | `screen` | activé | Read whether the screen is on, its current width and height, orientation, rotation, and density. Does not request screen capture consent. |
| `files_list` | `files` | activé | Lists workspace files with metadata. Results are bounded and report truncation. |
| `files_stat` | `files` | activé | Returns existence, type, size, and modification time of a workspace path. |
| `files_read` | `files` | activé | Reads up to 1 MiB. Use encoding base64 for binary data; encoding, bytes, totalBytes, and truncated identify the representation and limit. |
| `files_write` | `files` | activé | Writes UTF-8 text and refreshes the host explorer. Content is limited to 1 MiB and the negotiated Binder request budget (normally 96 KiB including JSON escaping); oversized calls fail before writing. |
| `files_mkdir` | `files` | activé | Creates a workspace directory and missing parents, then refreshes the host explorer. |
| `files_rename` | `files` | activé | Moves a workspace file or directory to another workspace path and refreshes the host explorer. |
| `files_delete` | `files_delete` | désactivé | Deletes a workspace entry. The separate files_delete group is off by default. The workspace root cannot be deleted. |
| `editor_open` | `files` | activé | Opens a workspace file in the AutoJs6 editor at a one-based line and column. Lines outside the file are ignored by the editor. |
| `app_launch` | `device` | activé | Opens an installed Android application. Provide exactly one of packageName or appName. |
| `app_list` | `device` | activé | Lists up to 1000 Android applications visible to AutoJs6, optionally matching a package name or label. Android package visibility restrictions apply. |
| `clipboard_get` | `device` | activé | Reads clipboard text (up to 64 KiB). Android may restrict clipboard access while AutoJs6 is in the background. |
| `clipboard_set` | `device` | activé | Replaces clipboard text, including an empty string to clear it. |
| `device_ensure_accessibility` | `device` | activé | Asks AutoJs6 to enable its accessibility service using its configured secure-settings, root, or Shizuku strategy. Waits up to 10 s for an operational service; failure includes manual activation guidance. |
| `toast` | `device` | activé | Shows a short Android toast on the phone. |
| `shell_exec` | `shell` | désactivé | Runs an Android shell command in the host workspace. The shell group is off by default; root also requires the separate allow root switch and a shell.root host grant. Reports exit code, timeout, stdout, stderr, and truncation. maxOutputBytes bounds stdout and stderr together. |

******

### Utilisation

******

1. Installez l'APK du plugin depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) sur un appareil disposant d'AutoJs6 build 5279 (6.8.0) ou ultérieur.
2. Ouvrez le centre de plugins d'AutoJs6, vérifiez que `MCP Server` est reconnu et activez-le. Les paquets officiels passent automatiquement la vérification de signature.
3. Activez MCP Server dans le volet AutoJs6. Appuyez longuement sur son titre ou ouvrez ses paramètres depuis le Centre de plugins. Copiez la configuration du client PC.
4. Sur le PC, exécutez `adb forward tcp:9637 tcp:9637` et pointez le client MCP vers `http://127.0.0.1:9637/mcp` avec le jeton comme identifiant bearer.
5. Confirmez la première demande d'association sur le téléphone. Arrêtez ensuite le serveur depuis le volet, les paramètres ou la notification.

> Bouton MCP Server avec guides d'installation, d'activation, d'autorisation et de compatibilité; arrêt synchronisé avec la notification; paramètres conservés à la reconnexion; accès vérifié à la même page depuis le volet et le Centre de plugins. Restaure le serveur à l'ouverture d'AutoJs6 sauf arrêt par l'utilisateur en l'absence de l'hôte; aucun lancement au démarrage de l'appareil.

<p align="center">
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/drawer-en.png?raw=true" alt="Bouton MCP Server dans le volet AutoJs6" width="300" />
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/settings-en.png?raw=true" alt="Page de paramètres de MCP Server" width="300" />
</p>

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

Réseau local: activez "Autoriser les connexions du réseau local" sur la page des paramètres. La page liste alors les adresses actuelles du téléphone (elles suivent les changements de Wi-Fi) et rappelle que le client doit rejoindre le même réseau; les réseaux invités, l'isolation du point d'accès et le pare-feu du PC sont les blocages habituels. Les demandes d'appairage venant du réseau local sont signalées comme telles, et une notification quotidienne le rappelle tant que le serveur reste joignable depuis le réseau; le rappel peut être désactivé. Sous Android 17 ou version ultérieure, autorisez les appareils à proximité avant de permettre ce plugin dans le centre de plugins AutoJs6. Cette autorisation se gère aussi dans les paramètres du plugin. Sans autorisation, le plugin reste désactivé et le démarrage automatique est ignoré sans message. Cette autorisation appartient au plugin et est indépendante de celle de AutoJs6.

Les deux chemins utilisent le même jeton et le même appairage côté téléphone. Les clients sans transport HTTP utilisent le pont stdio décrit dans Clients.

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

Claude Desktop et les autres clients stdio uniquement: installez le pont avec `npm install -g autojs6-mcp-bridge`, enregistrez `autojs6-mcp-bridge --serial <serial>` comme serveur stdio et placez `AUTOJS6_MCP_TOKEN` dans son bloc d'environnement (les extraits pour Claude Desktop et Claude Code sont dans le [README du pont](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge)). Le pont 0.1.0 accompagne le plugin 1.0.0 et transmet la version de protocole du client telle quelle; vérifié avec Claude Code 2.1.257 en stdio.

******

### Questions fréquentes

******

- 401 Unauthorized: le jeton est absent, mal saisi ou a été renouvelé. Copiez à nouveau la configuration depuis la page des paramètres; après un renouvellement du jeton, chaque client a besoin de la nouvelle valeur.
- Délai d'appairage dépassé: le premier appel d'un nouveau client attend environ une minute que vous touchiez Autoriser sur le téléphone. Déverrouillez le téléphone, acceptez la boîte de dialogue ou l'action de la notification, puis répétez l'appel. Refuser lance une courte pause, après laquelle l'appel suivant redemande.
- HOST_UNAVAILABLE: AutoJs6 n'est pas lancé ou sa session de plugin est fermée. Ouvrez AutoJs6, laissez l'interrupteur du tiroir activé et vérifiez l'état de connexion sur la page des paramètres.
- Accessibilité désactivée: les outils `ui_*` et la capture d'écran ont besoin du service d'accessibilité d'AutoJs6. Appelez `device_ensure_accessibility` ou activez le service dans les paramètres d'accessibilité du système.
- Port occupé: le tiroir signale `port_in_use`. Changez le port sur la page des paramètres et redirigez le nouveau port avec adb.
- Réseau local injoignable: activez l'accès depuis le réseau local, utilisez une adresse listée sur la page des paramètres, gardez le PC et le téléphone sur le même réseau sans isolation invité, et autorisez le port dans le pare-feu du PC. L'économie d'énergie Wi-Fi du téléphone ajoute quelques centaines de millisecondes par appel.
- Le serveur disparaît après l'extinction de l'écran: les téléphones qui limitent l'utilisation de la batterie par l'application (HyperOS et MIUI le font par défaut pour les applications installées manuellement) arrêtent le service de premier plan environ une minute après l'extinction de l'écran sur batterie. La page des paramètres affiche alors un avertissement avec le bouton "Paramètres de batterie"; choisissez-y "Sans restriction" pour MCP Server. Le paramètre facultatif "Arrêt automatique en cas d'inactivité" (désactivé par défaut) arrête aussi le serveur après les minutes choisies sans requête et laisse une notification qui l'indique.

******

### Permissions et sécurité

******

Le plugin respecte des limites explicites :

- Les points d'entrée Binder et la page des paramètres sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, si bien que seul AutoJs6 peut les atteindre ; la boîte de dialogue d'appairage, son récepteur et la page d'historique des versions ne sont pas exportés. Seul le service de premier plan qui héberge l'écouteur accepte adb (`android.permission.DUMP`), ce qui constitue l'interrupteur de démarrage / d'arrêt du développeur.
- La permission INTERNET ne sert qu'à l'écouteur HTTP propre du plugin ; le plugin n'émet aucune requête sortante et ne collecte aucune donnée. Le HTTP en clair n'est autorisé que vers les adresses de bouclage par la configuration de sécurité réseau.
- Le serveur écoute sur 127.0.0.1 par défaut. L'accès depuis le réseau local reste désactivé tant que vous ne l'activez pas ; le jeton, la confirmation d'appairage, la liste des Host autorisés et les limites de débit s'appliquent toujours sur le réseau local, et une notification quotidienne vous le rappelle tant qu'il est actif.
- Sous Android 17 ou version ultérieure, autorisez les appareils à proximité avant de permettre ce plugin dans le centre de plugins AutoJs6. Cette autorisation se gère aussi dans les paramètres du plugin. Sans autorisation, le plugin reste désactivé et le démarrage automatique est ignoré sans message. Cette autorisation appartient au plugin et est indépendante de celle de AutoJs6.
- Le jeton d'accès provient d'une source aléatoire sûre, est enveloppé avec une clé AES-GCM de l'Android Keystore et réside dans l'espace privé du plugin, jamais sauvegardé ; les sauvegardes et les transferts entre appareils sont désactivés. La page des paramètres n'affiche que ses 4 derniers caractères, les boîtes de dialogue du jeton complet bloquent les captures d'écran et les copies sont marquées sensibles pour le presse-papiers.
- Les journaux ne contiennent jamais le jeton, les corps des requêtes, le contenu des fichiers ni les captures d'écran ; le plugin ne journalise que les noms d'outils, les noms de clients et les empreintes du jeton. Cela a été vérifié avec logcat sur deux appareils pendant de vrais appels de fichiers et de captures (docs/dev/p6-security-audit.md).
- Les appels d'outils passent par le courtier de capacités d'AutoJs6 et ne dépassent jamais ce que l'hôte lui-même est autorisé à faire ; les commandes shell, la suppression de fichiers et les gestes restent désactivés tant que vous n'activez pas leurs groupes, et un shell root exige en plus son propre interrupteur et une autorisation de l'hôte.
- Les appairages peuvent être révoqués un par un ou tous à la fois sur la page des paramètres ; un client révoqué doit être confirmé à nouveau sur le téléphone avant son prochain appel d'outil. La rotation du jeton conserve les appairages mais coupe tout client qui utilise encore l'ancien jeton.

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

#### v1.0.2

_2026/09/16_

- `Amélioration` Autorisation du réseau local sous Android 17 intégrée au parcours activation et aux paramètres du plugin, sans page du lanceur; sans autorisation, le plugin reste désactivé et le démarrage automatique est silencieux
- `Amélioration` Cibler Android 17 (SDK 37) avec des autorisations réseau local propres au plugin et une aide à la récupération

#### v1.0.1

_2026/09/16_

- `Fonctionnalité` Ressources de documentation hors ligne optionnelles: lorsque le plugin AutoJs6 Offline Docs est installé et que l'hôte le relaie via app.listDocs / app.readDoc, resources/list ajoute autojs6://docs/ (un index avec les URI enfants) et une ressource autojs6://docs/{+path} par page de documentation, et resources/templates/list ajoute le modèle docs; sans le plugin, ou sur un hôte dépourvu de ces méthodes, rien n'est listé et _meta.docsCatalogStatus en donne la raison
- `Amélioration` compileSdk passe à 37 (Android 17) ; targetSdk reste à 36 jusqu'à la vérification du comportement dépendant de la cible
- `Amélioration` Conformité MCP (P6): la suite officielle @modelcontextprotocol/conformance 0.1.16 a été exécutée sur deux appareils contre le chemin /mcp avec état. 9 de ses 32 scénarios serveur passent (initialize, ping, tools/list, résultats d'outil texte et erreur, resources/list, prompts/list, flux SSE concurrents, protection contre le DNS rebinding); 18 appellent les fixtures de référence de la suite elle-même (outils test_*, prompts et ressources test://, auxquels ce serveur répond par un résultat d'outil inconnu, -32602 ou isError) et 5 exigent des capacités que le serveur ne déclare pas (logging, completions, abonnements aux ressources). L'en-tête Origin de bouclage est désormais accepté dans tous les modes, comme la suite l'attend; les en-têtes CORS et les réponses preflight restent réservés au mode développeur. Le modèle sans état 2026-07-28 n'a pas de route (Roadmap D9). Détails dans docs/dev/p6-conformance.md.
- `Amélioration` Audit de sécurité (P6): les sept points de la liste (stockage du jeton, expurgation des journaux, composants exportés, portée du trafic en clair, réseau local désactivé par défaut, révocation des appairages, groupes d'outils désactivés par défaut) sont vérifiés dans le code et sur deux appareils dans docs/dev/p6-security-audit.md, et la section sécurité du README décrit désormais ces limites. Le HTTP en clair est limité aux adresses de bouclage par une configuration de sécurité réseau au lieu du drapeau usesCleartextTraffic de toute l'application ; le plugin n'ouvre aucune connexion cliente et l'écouteur n'a pas besoin de ce drapeau.
- `Amélioration` Référence de performance (P6): ui_dump à 50 / 200 / 400 noeuds, screen_capture en trois tailles, les allers-retours de script_run et quatre requêtes simultanées ont été chronométrés sur un émulateur API 24, un téléphone Sony (API 33) et une Xiaomi Pad (API 35) et consignés dans docs/dev/p6-performance-baseline.md comme référence sans seuils. Un appel traité par le seul plugin répond en 20 ms environ sur l'émulateur et le téléphone, ui_dump croît d'environ 0,05 ms par noeud, la capture par accessibilité répond en moins de 100 ms alors que la voie MediaProjection sur API 24 prend environ 1,35 s par capture, et quatre requêtes simultanées se terminent en 1,0 à 1,8 fois un aller-retour dans la limite de quatre appels simultanés de l'hôte.

#### v1.0.0

_2026/09/16_

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
- `Fonctionnalité` Limites de débit par client (P6): au plus 20 requêtes par seconde et 30 appels screen_capture par minute et par client. Une requête au-delà de la limite reçoit HTTP 429 avec un en-tête Retry-After et une erreur JSON-RPC RATE_LIMITED portant retryAfterMs; une capture au-delà de la limite reçoit un résultat d'outil RATE_LIMITED avec retryAfterMs pour que le modèle puisse attendre. Le débit des requêtes d'accessibilité du grant de l'hôte s'applique en plus.
- `Fonctionnalité` Arrêt automatique en cas d'inactivité et batterie (P6): la page des paramètres propose "Arrêt automatique en cas d'inactivité" (désactivé par défaut; 5 / 15 / 30 / 60 / 120 minutes). L'écouteur s'arrête de lui-même après la durée choisie sans requête des clients, enregistre la raison interne idle_timeout, laisse une notification qui disparaît seule et ne compte pas comme un arrêt par l'utilisateur; une requête en cours (un appel d'outil qui s'exécute) n'est jamais de l'inactivité, tandis qu'un client qui garde son flux ouvert sans envoyer de requête ne maintient pas le serveur. Quand Android limite l'utilisation de la batterie par l'application (HyperOS et MIUI le font par défaut pour les applications installées manuellement et arrêtent le service de premier plan environ une minute après l'extinction de l'écran sur batterie), la page des paramètres affiche un avertissement avec le bouton "Paramètres de batterie" et AutoJs6 reçoit un événement warning. Le temps CPU au repos et l'estimation batterystats d'une heure d'inactivité sont consignés dans docs/dev/p6-battery-and-residency.md.
- `Correctif` Le rebuild de l'IDE ne recherche plus d'APK pour les tests unitaires JVM. Les tâches de vérification des APK assemblent automatiquement leurs entrées et fonctionnent après un clean.
- `Correctif` Les proportions de l'icône du centre de plugins variaient entre les modes clair et sombre; le mode nuit utilise aussi l'icône adaptative, avec des couches redimensionnées pour conserver le dessin complet et les marges de ic_launcher_round.png, seul le fond changeant de couleur
- `Correctif` La navigation au clavier avec Tab sur la page des paramètres ignorait le bouton retour de la barre d'outils; le cycle Tab couvre désormais le bouton retour et tous les contrôles, y compris sur Android 7. Les tests sur appareil vérifient les libellés du lecteur d'écran et l'utilisation au clavier.
- `Correctif` La carte arguments de script_run et script_run_file déclarait le type de ses valeurs comme un tableau JSON Schema que certains clients MCP rejettent ou affaiblissent; le schéma utilise désormais des branches anyOf à type unique. Le README gagne les sections Clients et Questions fréquentes avec la matrice des clients testés.
- `Correctif` Les corps de requête hostiles sont refusés avant qu'un analyseur ne s'y enfonce: un JSON imbriqué sur plus de 64 niveaux, un corps qui répète un id de requête et un id encore en cours sur la même session (qui laissait la première requête sans réponse) reçoivent 400 avec une erreur JSON-RPC; un flux GET sans session active reçoit 400 / 404 au lieu d'un flux d'événements vide. Les tests JVM et sur appareil (API 28 / 31 / 33 / 35) couvrent les corps trop longs ou trop profonds, l'UTF-8 invalide, les méthodes inconnues, les combinaisons d'en-têtes, le base64 volumineux et 64 sessions simultanées.
- `Correctif` Matrice de cycle de vie (P6): une requête portant l'id de session d'un processus d'écoute précédent (après que l'écouteur a été tué ou redémarré par une rotation du jeton) est laissée au 404 du transport pour que le client refasse initialize, au lieu d'ouvrir une demande d'appairage sous le User-Agent de la requête pour un client déjà appairé; les notifications d'appairage qu'un processus d'écoute mort a laissées dans le volet sont effacées au démarrage de l'écouteur suivant. La mort de l'hôte, celle de l'écouteur, celle des deux à la fois, l'arrêt forcé depuis les paramètres système, la rotation du jeton pendant une session active et un appairage en attente traversant la mort de l'hôte et de l'écouteur sont consignés avec leurs états attendus et leurs chemins de reprise dans docs/dev/lifecycle-matrix.md et vérifiés sur appareils.
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

La compilation nécessite JDK 21 ou ultérieur et Android SDK 37 ; les versions de Gradle et des plugins sont gérées de manière centralisée par `version.properties` et `io.github.supermonster003.autojs6-platform-versions`.

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
