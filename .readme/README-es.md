<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-mcp-server-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Expone la automatización del dispositivo a agentes de IA mediante el Model Context Protocol</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-MCP-Server?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-MCP-Server?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/.readme/README-ar.md)

******

### Introducción

******

MCP Server convierte un dispositivo Android con AutoJs6 en un servidor [Model Context Protocol](https://modelcontextprotocol.io). Los agentes de IA del PC, como Claude Code, Cursor o MCP Inspector, se conectan al teléfono por USB o Wi-Fi y usan herramientas para ejecutar scripts, leer registros, inspeccionar el árbol de nodos de accesibilidad, tocar y escribir, tomar capturas de pantalla y trabajar con archivos y aplicaciones.

El servidor se ejecuta dentro del propio proceso del plugin y se alcanza mediante un único punto de conexión Streamable HTTP. AutoJs6 entrega al plugin un intermediario de capacidades a través de Binder, de modo que cada llamada a una herramienta la ejecuta el anfitrión con sus permisos, motores y servicio de accesibilidad existentes; el plugin nunca duplica la funcionalidad del anfitrión.

******

### Estado

******

Versión 1.0.1: 37 herramientas (33 activadas por defecto), recursos y plantillas MCP, un interruptor en el panel de AutoJs6 y una página de ajustes del complemento. Requiere AutoJs6 6.8.0 (compilación 5279) o posterior; los recursos opcionales autojs6://docs/ necesitan además el plugin AutoJs6 Offline Docs y un anfitrión con sus métodos de retransmisión. El progreso y las evidencias se registran en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Funciones

******

El complemento ofrece las siguientes capacidades:

- Ajustes del teléfono para estado del servidor, USB, puerto y red local, token, revocación de vínculos, grupos y root, modo de desarrollador, configuraciones copiables de Claude Code / Cursor / Codex / HTTP e historial de versiones, siguiendo el aspecto de AutoJs6. Los cambios de red reinician el servidor activo; tokens y permisos se aplican inmediatamente. Los diálogos secretos bloquean capturas.
- Ejecución de scripts: ejecutar JavaScript desde texto o desde un archivo dentro de AutoJs6, listar y detener motores, y leer la salida reciente de la consola.
- Interfaz de accesibilidad: volcar el árbol de nodos en un formato de texto compacto, buscar nodos con la sintaxis de selectores de AutoJs6, hacer clic, mantener pulsado, desplazar, establecer texto y pulsar teclas globales como Atrás e Inicio.
- Grupo de capturas (P3.3): screen_capture devuelve imágenes MCP con recorte, scale o maxWidth, JPEG / PNG / WebP y control de calidad. Valores predeterminados: JPEG, calidad 70 y lado mayor de 1280 px. Si base64 supera 4 MiB, se reintenta con menor calidad o tamaño y los metadatos indican el ajuste. screen_state informa del estado, tamaño, orientación y densidad. El catálogo incluye 37 herramientas. La alternativa MediaProjection requiere AutoJs6 compilado el 2026-09-13 o después y autorización en el teléfono, reutilizada por la sesión del host.
- Herramientas del directorio de trabajo (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open con fila y columna desde 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast y shell_exec. Lectura binaria en base64, hasta 1 MiB de datos originales. La escritura respeta además el límite del host (normalmente 96 KiB con escapes JSON). El borrado y Shell están desactivados inicialmente; root requiere allowShellRoot y el permiso shell.root del host. Se necesita el host P3.4 correspondiente.
- Los recursos MCP (P3.5) ofrecen archivos de trabajo de solo lectura, ejemplos del anfitrión, la documentación sin conexión cuando el plugin AutoJs6 Offline Docs está instalado, información del dispositivo y salida reciente de consola, respetando el emparejamiento y los grupos activos. Las lecturas de texto y datos binarios indican el truncamiento. Las plantillas write_autojs6_script, automate_task y debug_selector están disponibles en inglés y chino, con inglés para los demás idiomas del teléfono.
- Rutas de conexión: USB mediante `adb forward`, red local con activación explícita y un puente stdio en el PC para clientes sin transporte HTTP.
- Seguridad: un token bearer rotatorio, confirmación de emparejamiento en el teléfono en el primer uso e interruptores de herramientas por grupo; el servidor solo escucha en la interfaz de bucle local de forma predeterminada.

******

### Herramientas

******

La tabla siguiente se genera a partir de la instantánea del catálogo de herramientas del complemento (`app/src/test/resources/tool-catalog.snapshot.json`); las descripciones son los textos en inglés que reciben los clientes y cada grupo puede desactivarse en la página de ajustes:

| Herramienta | Grupo | Predeterminado | Descripción |
|---|---|---|---|
| `device_ping` | `device` | activado | Confirms that the AutoJs6 MCP Server plugin is reachable and returns its version, the device model, the Android API level, and the device time. |
| `device_info` | `device` | activado | Returns the device build, screen, battery, memory, AutoJs6 host version and process, accessibility service state, screen state, locale, and time zone as AutoJs6 reports them (schema autojs6-bridge-device-info-v1). No hardware identifiers. |
| `script_run` | `script` | activado | Runs JavaScript source in AutoJs6 (its Rhino engine with the full AutoJs6 API) and by default waits for it to finish. Use it for automation steps: toasts, UI actions, file work, app launches. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_run_file` | `script` | activado | Runs a script file that already exists on the device (AutoJs6 picks the engine from the suffix) and by default waits for it to finish. The result carries executionId, status (finished, error, running), durationMs, the exception with its line when the script threw, and the newest console lines. A script still running after the wait keeps running: script_stop stops it, script_list shows it, console_tail follows its output. |
| `script_stop` | `script` | activado | Stops one running AutoJs6 script by the executionId that script_run, script_run_file, or script_list reported. |
| `script_stop_all` | `script` | activado | Stops every script AutoJs6 is running, including ones started on the phone, and returns how many were stopped. |
| `script_list` | `script` | activado | Lists the scripts AutoJs6 is running or starting, with executionId, name, path, working directory, state, and uptime. |
| `console_tail` | `script` | activado | Returns the newest lines of the AutoJs6 console, which every script shares; optionally only entries after sinceId or at least a level. nextSinceId in the result continues from where this call ended. |
| `ui_dump` | `ui` | activado | Dumps the accessibility node tree of the active window as compact text: one node per line with a #n reference, an indent per depth, the short class name, the state markers that apply (clickable, long_clickable, checkable, checked, scrollable, editable, focused, selected, !enabled, hidden), the text in quotes, desc=, id= (name part only), and the position (bounds [l,t][r,b] for a node with children, c=(x,y) for a leaf). Pass a #n reference as nodeRef to ui_click, ui_long_click, ui_set_text, or ui_scroll; references stay valid until the next ui_dump or for 60 s. Call it before acting and again after the screen changed. format json returns the nodes as objects with every flag; format xml returns the uiautomator-style export. |
| `ui_find` | `ui` | activado | Finds the nodes of the active window that match every condition of the selector, optionally waiting up to timeoutMs for the first match, and returns up to limit of them with #n references, bounds, and center. An empty count is not an error; ui_explain_selector tells which condition fails. |
| `ui_current_window` | `ui` | activado | Returns the package and activity in the foreground, whether the AutoJs6 accessibility service is available, and the accessibility windows with their type, title, bounds, and focus. |
| `ui_explain_selector` | `ui` | activado | Explains why a selector matches or not: evaluates its conditions one by one over the active window and reports how many nodes pass each step cumulatively, the first failing condition, the matches, and the near misses. Use it when ui_find returns nothing. |
| `ui_wait_for` | `ui` | activado | Waits until a node matching the selector appears (default) or disappears, polling the active window every 0.5 s for up to timeoutMs, and answers TIMEOUT when the state is not reached. Use it after an action that opens a screen or dismisses a dialog. |
| `ui_click` | `ui` | activado | Clicks a node given by nodeRef (a #n reference from the last ui_dump), by selector (the first match in pre-order), or by x and y (a coordinate tap, allowed only while the ui_gesture group is enabled). The accessibility click climbs to the nearest clickable ancestor when the node itself is not clickable. Returns the node it acted on. Give nodeRef or selector, not both. |
| `ui_long_click` | `ui` | activado | Long-presses a node given by nodeRef or selector (the accessibility long click climbs to the nearest node that accepts it), or by x and y as a 700 ms press at that point (allowed only while the ui_gesture group is enabled). Give nodeRef or selector, not both. |
| `ui_set_text` | `ui` | activado | Sets the text of an editable node (an EditText, marked editable by ui_dump) given by nodeRef or selector; append adds to the current text instead of replacing it. Works without focus or the keyboard; ACTION_FAILED means the node is not editable or not enabled. Give nodeRef or selector, not both. |
| `ui_scroll` | `ui` | activado | Scrolls a node given by nodeRef or selector, or the first scrollable node of the window when neither is given: forward, down, and right move towards the end, backward, up, and left towards the start; times repeats the step. performed counts the steps the node accepted, fewer than requested means it reached the end. Give nodeRef or selector, not both. |
| `ui_press_key` | `ui` | activado | Presses a global key through the accessibility service: back, home, recents, notifications (opens the notification shade), quick_settings, power_dialog, or lock_screen (Android 9 or later). |
| `ui_swipe` | `ui_gesture` | desactivado | Swipes one finger from (x1, y1) to (x2, y2) in device pixels over durationMs; take the coordinates from ui_dump bounds or a screenshot. Part of the ui_gesture group, which is off by default. |
| `ui_gesture` | `ui_gesture` | desactivado | Performs a free-path one-finger gesture through the given points over durationMs (at most 10 s): the first point is the touch down, the last the lift. Part of the ui_gesture group, which is off by default. |
| `screen_capture` | `screen` | activado | Capture the phone screen as an MCP image with dimensions, size, duration and capture source. Uses accessibility on Android 11+ and falls back to MediaProjection, which requires consent on the phone the first time. Defaults to JPEG quality 70 and a longest edge of 1280 pixels. Choose scale or maxWidth to override the size. Images above the 4 MiB base64 limit are retried at lower quality or smaller dimensions; metadata reports adjustments. At most 30 captures per minute per client; a RATE_LIMITED result names the wait in retryAfterMs. |
| `screen_state` | `screen` | activado | Read whether the screen is on, its current width and height, orientation, rotation, and density. Does not request screen capture consent. |
| `files_list` | `files` | activado | Lists workspace files with metadata. Results are bounded and report truncation. |
| `files_stat` | `files` | activado | Returns existence, type, size, and modification time of a workspace path. |
| `files_read` | `files` | activado | Reads up to 1 MiB. Use encoding base64 for binary data; encoding, bytes, totalBytes, and truncated identify the representation and limit. |
| `files_write` | `files` | activado | Writes UTF-8 text and refreshes the host explorer. Content is limited to 1 MiB and the negotiated Binder request budget (normally 96 KiB including JSON escaping); oversized calls fail before writing. |
| `files_mkdir` | `files` | activado | Creates a workspace directory and missing parents, then refreshes the host explorer. |
| `files_rename` | `files` | activado | Moves a workspace file or directory to another workspace path and refreshes the host explorer. |
| `files_delete` | `files_delete` | desactivado | Deletes a workspace entry. The separate files_delete group is off by default. The workspace root cannot be deleted. |
| `editor_open` | `files` | activado | Opens a workspace file in the AutoJs6 editor at a one-based line and column. Lines outside the file are ignored by the editor. |
| `app_launch` | `device` | activado | Opens an installed Android application. Provide exactly one of packageName or appName. |
| `app_list` | `device` | activado | Lists up to 1000 Android applications visible to AutoJs6, optionally matching a package name or label. Android package visibility restrictions apply. |
| `clipboard_get` | `device` | activado | Reads clipboard text (up to 64 KiB). Android may restrict clipboard access while AutoJs6 is in the background. |
| `clipboard_set` | `device` | activado | Replaces clipboard text, including an empty string to clear it. |
| `device_ensure_accessibility` | `device` | activado | Asks AutoJs6 to enable its accessibility service using its configured secure-settings, root, or Shizuku strategy. Waits up to 10 s for an operational service; failure includes manual activation guidance. |
| `toast` | `device` | activado | Shows a short Android toast on the phone. |
| `shell_exec` | `shell` | desactivado | Runs an Android shell command in the host workspace. The shell group is off by default; root also requires the separate allow root switch and a shell.root host grant. Reports exit code, timeout, stdout, stderr, and truncation. maxOutputBytes bounds stdout and stderr together. |

******

### Uso

******

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) en un dispositivo con AutoJs6 build 5279 (6.8.0) o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `MCP Server` se reconoce y actívelo. Los paquetes oficiales superan la verificación de firma automáticamente.
3. Activa MCP Server en el panel de AutoJs6. Mantén pulsado su título o abre Ajustes desde el Centro de complementos. Copia la configuración del cliente de PC.
4. En el PC, ejecute `adb forward tcp:9637 tcp:9637` y apunte el cliente MCP a `http://127.0.0.1:9637/mcp` con el token como credencial bearer.
5. Confirma la primera solicitud de vinculación en el teléfono. Al terminar, detén el servidor desde el panel, los ajustes o la notificación.

> Interruptor MCP Server con guías de instalación, activación, autorización y compatibilidad; sincronización con Detener en la notificación; ajustes conservados al reconectar; acceso verificado a la misma página desde el panel y el Centro de complementos. Restaura el servidor al abrir AutoJs6 salvo que el usuario lo detuviera sin el anfitrión; no arranca con el dispositivo.

<p align="center">
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/drawer-en.png?raw=true" alt="Interruptor de MCP Server en el panel de AutoJs6" width="300" />
  <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/images/readme/settings-en.png?raw=true" alt="Página de ajustes de MCP Server" width="300" />
</p>

******

### Configuración del cliente

******

Claude Code registra el servidor con un solo comando; los demás clientes usan la misma URL y cabecera en su configuración MCP:

```shell
adb forward tcp:9637 tcp:9637
claude mcp add --transport http autojs6 http://127.0.0.1:9637/mcp --header "Authorization: Bearer <token>"
```

Activa MCP Server en el panel de AutoJs6. Mantén pulsado su título o abre Ajustes desde el Centro de complementos. Copia la configuración del cliente de PC. Confirma la primera solicitud de vinculación en el teléfono. Al terminar, detén el servidor desde el panel, los ajustes o la notificación.

******

### Rutas de conexión

******

USB: `adb forward tcp:9637 tcp:9637` asigna el puerto del teléfono al PC; con varios dispositivos añade `-s <serial>` (consúltalo con `adb devices`), y los emuladores funcionan igual. Si el puerto está ocupado en cualquiera de los lados, cámbialo en la página de ajustes y reenvía el nuevo. La tarjeta de conexión ofrece el comando de reenvío listo para copiar.

Red local: activa "Permitir conexiones de red local" en la página de ajustes. La página muestra entonces las direcciones actuales del teléfono (siguen los cambios de Wi-Fi) y recuerda que el cliente debe estar en la misma red; las redes de invitados, el aislamiento del punto de acceso y el cortafuegos del PC son los bloqueos habituales. Las solicitudes de emparejamiento desde la red local se marcan como tales y una notificación diaria lo recuerda mientras el servidor siga accesible desde la red; el recordatorio puede desactivarse.

Ambas rutas usan el mismo token y el mismo emparejamiento en el teléfono. Los clientes sin transporte HTTP usan el puente stdio descrito en Clientes.

******

### Clientes

******

La página de ajustes copia una configuración lista con el token real para cada cliente de abajo; los fragmentos de aquí usan `<token>` como marcador. Todos los clientes hablan Streamable HTTP con una cabecera Authorization, y la primera llamada de un cliente nuevo se confirma en el teléfono. Verificados: Claude Code, Codex CLI y MCP Inspector; los demás clientes usan la misma URL y cabecera, pero el mantenedor aún no los ha probado.

Claude Code: ejecuta el comando mostrado en "Configuración del cliente" (la página de ajustes lo copia con el token); después `claude mcp list` muestra `autojs6` como Connected.

Cursor: añade la entrada de abajo a `mcp.json`:

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

Codex CLI: pon el token en la variable de entorno `AUTOJS6_MCP_TOKEN` (la página de ajustes copia un comando de PowerShell para ello) y añade el servidor a `config.toml`, o ejecuta `codex mcp add autojs6 --url <url> --bearer-token-env-var AUTOJS6_MCP_TOKEN`:

```toml
[mcp_servers.autojs6]
url = "http://127.0.0.1:9637/mcp"
bearer_token_env_var = "AUTOJS6_MCP_TOKEN"
```

MCP Inspector: el modo CLI no necesita configuración adicional, y la interfaz web llega al teléfono a través de su propio proxy de Node. Activa el modo de desarrollador en la página de ajustes solo cuando una página del navegador se conecte directamente al endpoint:

```shell
npx @modelcontextprotocol/inspector --cli http://127.0.0.1:9637/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list
```

Cline, VS Code Copilot Chat, Gemini CLI y clientes similares: usa la misma URL y cabecera en su configuración MCP; la página de ajustes ofrece un fragmento JSON genérico con `"type": "http"`.

Claude Desktop y otros clientes solo stdio: instala el puente con `npm install -g autojs6-mcp-bridge`, registra `autojs6-mcp-bridge --serial <serial>` como servidor stdio y pon `AUTOJS6_MCP_TOKEN` en su bloque de entorno (los fragmentos para Claude Desktop y Claude Code están en el [README del puente](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge)). El puente 0.1.0 acompaña al plugin 1.0.0 y transmite la versión de protocolo del cliente sin cambios; se verificó con Claude Code 2.1.257 por stdio.

******

### Preguntas frecuentes

******

- 401 Unauthorized: el token falta, está mal escrito o fue rotado. Copia de nuevo la configuración desde la página de ajustes; tras Rotar token, cada cliente necesita el valor nuevo.
- Tiempo de emparejamiento agotado: la primera llamada de un cliente nuevo espera alrededor de un minuto a que pulses Permitir en el teléfono. Desbloquea el teléfono, acepta el diálogo o la acción de la notificación y repite la llamada. Denegar inicia un breve periodo de espera, tras el cual la siguiente llamada vuelve a preguntar.
- HOST_UNAVAILABLE: AutoJs6 no está en ejecución o su sesión de plugin está cerrada. Abre AutoJs6, mantén el interruptor del cajón activado y revisa el estado de conexión en la página de ajustes.
- Accesibilidad desactivada: las herramientas `ui_*` y la captura de pantalla necesitan el servicio de accesibilidad de AutoJs6. Llama a `device_ensure_accessibility` o activa el servicio en los ajustes de accesibilidad del sistema.
- Puerto en uso: el cajón informa `port_in_use`. Cambia el puerto en la página de ajustes y reenvía el nuevo puerto con adb.
- Red local inaccesible: activa el acceso desde la red local, usa una dirección de las listadas en la página de ajustes, mantén el PC y el teléfono en la misma red sin aislamiento de invitados y permite el puerto en el cortafuegos del PC. El ahorro de energía Wi-Fi del teléfono añade unos cientos de milisegundos por llamada.
- El servidor desaparece al apagarse la pantalla: los teléfonos que restringen el uso de batería de la aplicación (HyperOS y MIUI lo hacen por defecto con aplicaciones instaladas manualmente) detienen el servicio en primer plano alrededor de un minuto después de apagarse la pantalla con batería. La página de ajustes muestra entonces un aviso con el botón "Ajustes de batería"; elija allí "Sin restricciones" para MCP Server. El ajuste opcional "Detener automáticamente en reposo" (desactivado por defecto) también detiene el servidor tras los minutos elegidos sin peticiones y deja una notificación que lo indica.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder y la página de ajustes están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, por lo que solo AutoJs6 puede alcanzarlos; el diálogo de emparejamiento, su receptor y la página de historial de versiones no están exportados. Solo el servicio en primer plano que aloja el receptor acepta adb (`android.permission.DUMP`), que es el interruptor de inicio / parada del desarrollador.
- El permiso INTERNET se usa solo para el propio receptor HTTP del plugin; el plugin no realiza solicitudes salientes ni recopila datos. El HTTP en claro solo se permite hacia direcciones de bucle local mediante la configuración de seguridad de red.
- El servidor escucha en 127.0.0.1 por defecto. El acceso desde la red local permanece desactivado hasta que lo active; el token, la confirmación de emparejamiento, la lista de Host permitidos y los límites de frecuencia siguen aplicándose en la red local, y una notificación diaria le recuerda que está activo.
- El token de acceso procede de una fuente aleatoria segura, se envuelve con una clave AES-GCM del Android Keystore y vive en el almacenamiento privado del plugin, que nunca se respalda; las copias de seguridad y las transferencias entre dispositivos están desactivadas. La página de ajustes muestra solo sus últimos 4 caracteres, los diálogos con el token completo bloquean las capturas de pantalla y las copias se marcan como sensibles en el portapapeles.
- Los registros nunca contienen el token, los cuerpos de las solicitudes, el contenido de archivos ni las capturas de pantalla; el plugin registra solo nombres de herramientas, nombres de clientes y huellas del token. Se verificó con logcat en dos dispositivos durante llamadas reales de archivos y capturas (docs/dev/p6-security-audit.md).
- Las llamadas a herramientas pasan por el intermediario de capacidades de AutoJs6 y nunca exceden lo que el propio anfitrión tiene permitido; los comandos de shell, la eliminación de archivos y los gestos permanecen desactivados hasta que active sus grupos, y un shell con root necesita además su propio interruptor y una concesión del anfitrión.
- Los emparejamientos se pueden revocar uno a uno o todos a la vez en la página de ajustes; un cliente revocado debe confirmarse de nuevo en el teléfono antes de su siguiente llamada a herramienta. Rotar el token conserva los emparejamientos pero corta a todo cliente que siga usando el token antiguo.

Obtenga el plugin únicamente desde la página oficial de [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) o el centro de plugins de AutoJs6. Los paquetes de origen desconocido pueden fallar la verificación del anfitrión o conllevar riesgos aunque el número de versión parezca idéntico.

******

### Interfaz del plugin

******

La siguiente información está dirigida a desarrolladores del anfitrión AutoJs6 y de plugins; el anfitrión usa estos identificadores para descubrir el plugin y negociar la compatibilidad:

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

`McpServerPluginService` implementa el contrato mcp-server-api del host `org.autojs.plugin.mcp.server.api.IMcpServerPlugin` en el proceso `:mcp_server` y responde a `org.autojs.plugin.MCP_SERVER` (category `mcp-server`). `McpServerPluginInfoService` responde a `org.autojs.plugin.INFO` con PluginInfo. `WakeActivity` permite al host activar el plugin.

******

### Hoja de ruta

******

Los planes y el progreso del plugin se mantienen como una lista verificable en ROADMAP.md, organizada por fases con criterios de aceptación y niveles de evidencia. Los elementos sin marcar expresan intención y no capacidades actuales; la discusión mediante Issues es bienvenida.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.0.1

_2026/09/15_

- `Función` Recursos opcionales de documentación sin conexión: cuando el plugin AutoJs6 Offline Docs está instalado y el anfitrión lo retransmite mediante app.listDocs / app.readDoc, resources/list añade autojs6://docs/ (un índice con URI hijos) y un recurso autojs6://docs/{+path} por página de documentación, y resources/templates/list añade la plantilla docs; sin el plugin, o en un anfitrión sin estos métodos, no se lista nada y _meta.docsCatalogStatus indica el motivo
- `Mejora` compileSdk sube a 37 (Android 17); targetSdk se mantiene en 36 hasta verificar el comportamiento que depende del objetivo
- `Mejora` Conformidad MCP (P6): la suite oficial @modelcontextprotocol/conformance 0.1.16 se ejecutó en dos dispositivos contra la ruta /mcp con estado. 9 de sus 32 escenarios de servidor pasan (initialize, ping, tools/list, resultados de herramienta de texto y de error, resources/list, prompts/list, flujos SSE concurrentes, protección contra DNS rebinding); 18 llaman a los accesorios de referencia de la propia suite (herramientas test_*, prompts y recursos test://, que este servidor responde con un resultado de herramienta desconocida, -32602 o isError) y 5 necesitan capacidades que el servidor no declara (logging, completions, suscripciones a recursos). La cabecera Origin de bucle local ahora se acepta en todos los modos, como espera la suite; las cabeceras CORS y las respuestas preflight siguen limitadas al modo desarrollador. El modelo sin estado 2026-07-28 no tiene ruta (Roadmap D9). Detalles en docs/dev/p6-conformance.md.
- `Mejora` Auditoría de seguridad (P6): los siete puntos de la lista (almacenamiento del token, redacción de registros, componentes exportados, alcance del tráfico en claro, red local desactivada por defecto, revocación de emparejamientos, grupos de herramientas desactivados por defecto) se verificaron en el código y en dos dispositivos en docs/dev/p6-security-audit.md, y la sección de seguridad del README describe ahora esos límites. El HTTP en claro se limita a direcciones de bucle local mediante una configuración de seguridad de red en lugar del indicador usesCleartextTraffic de toda la aplicación; el plugin no abre conexiones de cliente y el receptor no necesita ese indicador.
- `Mejora` Línea base de rendimiento (P6): se midieron ui_dump con 50 / 200 / 400 nodos, screen_capture en tres tamaños, las idas y vueltas de script_run y cuatro solicitudes concurrentes en un emulador API 24, un teléfono Sony (API 33) y una Xiaomi Pad (API 35), y se registraron en docs/dev/p6-performance-baseline.md como referencia sin umbrales. Una llamada respondida solo por el plugin tarda unos 20 ms en el emulador y el teléfono, ui_dump crece unos 0,05 ms por nodo, la captura por accesibilidad responde en menos de 100 ms mientras que la vía MediaProjection en API 24 tarda unos 1,35 s por captura, y cuatro solicitudes concurrentes terminan en 1,0-1,8 veces una ida y vuelta dentro del límite del anfitrión de cuatro llamadas concurrentes.

#### v1.0.0

_2026/09/15_

- `Aviso` Vista previa P4: 37 herramientas, 33 activadas por defecto, con un interruptor en AutoJs6 y una página de ajustes del complemento. Requiere la compilación AutoJs6 de P4 correspondiente. ROADMAP.md.
- `Función` Ajustes del teléfono para estado del servidor, USB, puerto y red local, token, revocación de vínculos, grupos y root, modo de desarrollador, configuraciones copiables de Claude Code / Cursor / Codex / HTTP e historial de versiones, siguiendo el aspecto de AutoJs6. Los cambios de red reinician el servidor activo; tokens y permisos se aplican inmediatamente. Los diálogos secretos bloquean capturas.
- `Función` Los recursos MCP (P3.5) ofrecen archivos de trabajo de solo lectura, ejemplos del anfitrión, información del dispositivo y salida reciente de consola, respetando el emparejamiento y los grupos activos. Las lecturas de texto y datos binarios indican el truncamiento. Las plantillas write_autojs6_script, automate_task y debug_selector están disponibles en inglés y chino, con inglés para los demás idiomas del teléfono.
- `Función` Herramientas del directorio de trabajo (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open con fila y columna desde 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast y shell_exec. Lectura binaria en base64, hasta 1 MiB de datos originales. La escritura respeta además el límite del host (normalmente 96 KiB con escapes JSON). El borrado y Shell están desactivados inicialmente; root requiere allowShellRoot y el permiso shell.root del host. Se necesita el host P3.4 correspondiente.
- `Función` Identidad de plugin `mcp-server` con el servicio INFO, la Wake Activity y el esqueleto del servicio `org.autojs.plugin.MCP_SERVER` para el descubrimiento por el anfitrión
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
- `Función` Punto de conexión Streamable HTTP en `http://127.0.0.1:9637/mcp` con la herramienta `device_ping`, alojado por un servicio en primer plano que adb o el anfitrión pueden activar y desactivar (vista previa de desarrollo)
- `Función` Refuerzo del transporte del punto final `/mcp`: la direccion y el puerto de enlace provienen del almacen de configuracion del servidor, el cuerpo de la solicitud se limita a 1 MiB, las conexiones inactivas se cierran a los 60 s, y un puerto ocupado o un enlace rechazado termina en un estado `port_in_use` / `bind_failed` con una pista en lugar de un fallo
- `Función` Proteccion contra DNS rebinding delante del transporte del SDK: el modo loopback solo acepta `localhost` / `127.0.0.1` / `[::1]` como `Host`, el modo LAN agrega las direcciones IPv4 actuales del dispositivo y nombres de host adicionales opcionales y los actualiza cuando cambia la red; los origenes de navegador se rechazan salvo que el interruptor de "modo desarrollador" admita el origen loopback del Inspector mediante CORS
- `Función` Identidad del servidor `autojs6-mcp-server` con la version del plugin y las capacidades tools (`listChanged`), resources y prompts; `tools/list` conserva el orden de registro para que los clientes puedan almacenarlo en cache
- `Función` Autenticacion con token bearer para cada solicitud `/mcp`: un token de 32 bytes generado en el primer inicio, envuelto con una clave AES-GCM del Android Keystore y guardado en el almacen privado del plugin que nunca se respalda; un encabezado `Authorization` ausente o incorrecto se rechaza tras una comparacion en tiempo constante con `401` + `WWW-Authenticate: Bearer` y un error JSON-RPC `-32001`; el token nunca llega al registro
- `Función` Emparejamiento de primer uso delante del transporte: un cliente sin emparejar puede hacer `initialize` y listar tools, resources y prompts, pero su primer `tools/call`, `resources/read`, `resources/subscribe` o `prompts/get` responde `PAIRING_REQUIRED` (`-32002`) hasta que la solicitud se confirme en el telefono dentro de 60 s; una denegacion o un tiempo agotado responde `PAIRING_DENIED` (`-32003`) durante 30 s; el cliente se identifica por el nombre de `clientInfo` (o `User-Agent`) mas la clase de direccion (loopback / LAN), por lo que rotar el token conserva los emparejamientos, y se pueden emparejar hasta 32 clientes
- `Función` Confirmacion del emparejamiento en el telefono por dos canales: una notificacion de alta prioridad con acciones Permitir / Denegar, mas un dialogo mientras la pantalla esta desbloqueada; la configuracion del servidor, el token y los clientes emparejados viven en archivos reemplazados atomicamente que el proceso del servidor y la pagina de ajustes comparten sin caches obsoletas
- `Función` Catalogo de herramientas con interruptores por grupo (decision D6): `device_ping` (local al plugin), `device_info` (`device.info` de AutoJs6) y `script_run` (`engines.execScript` de AutoJs6: ejecuta JavaScript, espera hasta `timeoutMs` a que termine y devuelve el resultado junto con las lineas mas recientes de la consola, enviando notificaciones de progreso mientras corre); cada herramienta declara un JSON Schema cerrado (`additionalProperties: false`) y sus argumentos se validan antes de que nada llegue a AutoJs6; los interruptores de grupo `script` / `ui` / `ui_gesture` / `screen` / `files` / `files_delete` / `device` / `shell` se guardan en `tool_groups.json`, un grupo desactivado desaparece de `tools/list` desde la siguiente solicitud y sus herramientas responden `TOOL_DISABLED`
- `Función` Puente con el anfitrion: el servicio `org.autojs.plugin.MCP_SERVER` implementa el Binder real `IMcpServerPlugin` (`getInfo` / `getCapabilities` informan la version de contrato 1, los grupos de herramientas, las versiones del protocolo MCP y la version del SDK; `openServer` solo admite el AutoJs6 instalado con la misma firma y devuelve un `IMcpServerSession` con `getStatus` / `updateConfig` / `stop` / `close`); las llamadas a herramientas viajan por el broker de capacidades del anfitrion con ids de solicitud monotonos, tiempo limite por llamada, el tope de 4 llamadas concurrentes y las categorias de error del anfitrion asignadas a `HOST_UNAVAILABLE` / `A11Y_SERVICE_NOT_RUNNING` / `CAPABILITY_DENIED` / `LIMIT_EXCEEDED` / `RATE_LIMITED` / `TIMEOUT` / `HOST_ERROR`; cuando AutoJs6 muere el oyente sigue en marcha y las herramientas que dependen del anfitrion responden `HOST_UNAVAILABLE` hasta que vuelve a conectarse; el estado y los eventos (`pairing_requested`, `client_paired`, `tool_call`, `warning`) llegan al anfitrion por su callback
- `Función` La notificacion del servicio en primer plano muestra el punto de acceso, el estado de conexion de AutoJs6 y el numero de clientes emparejados, ademas de una accion Detener; un toast indica el punto de acceso cuando las notificaciones estan bloqueadas; `dumpsys activity service` imprime ademas la sesion del anfitrion, los interruptores de grupo y las herramientas registradas
- `Función` Grupo de scripts completado: `script_run_file` ejecuta un archivo de script del dispositivo, `script_stop` / `script_stop_all` detienen una o todas las ejecuciones de AutoJs6, `script_list` enumera las que estan en marcha y `console_tail` devuelve las lineas mas recientes de la consola con un cursor `nextSinceId` y un filtro por nivel; `script_run` y `script_run_file` ahora informan `executionId`, `status` (`finished` / `error` / `running`), `durationMs`, la excepcion con su linea y las lineas mas recientes de la consola, y mientras esperan envian cada 2 s una notificacion de progreso con la linea mas reciente de la consola
- `Función` Las respuestas del endpoint MCP se transmiten como eventos enviados por el servidor (no se usa el modo de respuesta JSON del SDK), de modo que una notificacion que pertenece a una solicitud, como el latido de progreso de un script en ejecucion, llega al cliente en la respuesta de esa solicitud
- `Función` Grupo UI anadido (roadmap P3.2): `ui_dump` devuelve la ventana activa como un arbol compacto de nodos con referencias `#n` (`format` text / json / xml, `maxNodes` hasta 400, `maxDepth`, `visibleOnly`, `window`), `ui_find` / `ui_wait_for` sondean un selector, `ui_current_window` y `ui_explain_selector` informan de la ventana y de por que falla un selector, `ui_click` / `ui_long_click` / `ui_set_text` / `ui_scroll` actuan sobre un `nodeRef` (relocalizado por su huella, `NODE_REF_STALE` si desaparecio) o un `selector`, `ui_press_key` pulsa back / home / recents / notifications / quick_settings / power_dialog / lock_screen, y el grupo `ui_gesture` (desactivado por defecto) anade `ui_swipe`, `ui_gesture` y la forma por coordenadas de las herramientas de clic (`TOOL_DISABLED` mientras el grupo esta desactivado); la instantanea del catalogo de herramientas crece a 20 herramientas; los gestos por coordenadas necesitan un host AutoJs6 compilado el 2026-09-11 o despues (un host anterior responde al azar "the system cancelled ...")
- `Función` Grupo de capturas (P3.3): screen_capture devuelve imágenes MCP con recorte, scale o maxWidth, JPEG / PNG / WebP y control de calidad. Valores predeterminados: JPEG, calidad 70 y lado mayor de 1280 px. Si base64 supera 4 MiB, se reintenta con menor calidad o tamaño y los metadatos indican el ajuste. screen_state informa del estado, tamaño, orientación y densidad. El catálogo incluye 22 herramientas. La alternativa MediaProjection requiere AutoJs6 compilado el 2026-09-13 o después y autorización en el teléfono, reutilizada por la sesión del host.
- `Función` Ruta por red local (P5.1): con el acceso desde la red local activado, la página de ajustes muestra las direcciones actuales del teléfono (se actualizan al cambiar la Wi-Fi) con avisos sobre la misma red y el cortafuegos; una solicitud de emparejamiento desde la red local se marca en el diálogo y en la notificación; un recordatorio diario indica que el servidor sigue accesible desde la red local y puede desactivarse sin reiniciar el oyente. El README documenta las rutas USB y de red local.
- `Función` Límites de frecuencia por cliente (P6): como máximo 20 peticiones por segundo y 30 llamadas a screen_capture por minuto por cliente. Una petición por encima del límite recibe HTTP 429 con cabecera Retry-After y un error JSON-RPC RATE_LIMITED con retryAfterMs; una captura por encima del límite se responde como resultado de herramienta RATE_LIMITED con retryAfterMs para que el modelo pueda esperar. La frecuencia de consultas de accesibilidad del grant del host sigue aplicándose además.
- `Función` Parada automática en reposo y batería (P6): la página de ajustes ofrece "Detener automáticamente en reposo" (desactivado por defecto; 5 / 15 / 30 / 60 / 120 minutos). El escuchador se detiene solo tras el tiempo elegido sin peticiones de clientes, registra el motivo interno idle_timeout, deja una notificación que se descarta sola y no cuenta como parada del usuario; una petición en curso (una llamada a herramienta en ejecución) nunca es reposo, mientras que un cliente que mantiene su flujo abierto sin enviar peticiones no mantiene el servidor en marcha. Cuando Android restringe el uso de batería de la aplicación (HyperOS y MIUI lo hacen por defecto con aplicaciones instaladas manualmente y detienen el servicio en primer plano alrededor de un minuto después de apagarse la pantalla con batería), la página de ajustes muestra un aviso con el botón "Ajustes de batería" y AutoJs6 recibe un evento warning. El tiempo de CPU en reposo y la estimación de batterystats de una hora en reposo quedan registrados en docs/dev/p6-battery-and-residency.md.
- `Corrección` El rebuild del IDE ya no busca un APK para las pruebas unitarias JVM. Las tareas de verificación de APK generan automáticamente sus entradas y funcionan después de un clean.
- `Corrección` Las proporciones del icono del centro de complementos variaban entre los modos claro y oscuro; el modo nocturno también usa el icono adaptable, con las capas ajustadas para conservar el dibujo completo y los márgenes de ic_launcher_round.png, cambiando solo el color de fondo
- `Corrección` La navegación con Tab en la página de ajustes omitía el botón atrás de la barra de herramientas; ahora el ciclo de Tab cubre el botón atrás y todos los controles, incluido Android 7. Las pruebas en dispositivo comprueban las etiquetas del lector de pantalla y el uso con teclado.
- `Corrección` El mapa arguments de script_run y script_run_file declaraba el tipo de sus valores como un arreglo JSON Schema que algunos clientes MCP rechazan o debilitan; el esquema ahora usa ramas anyOf de un solo tipo. El README gana las secciones Clientes y Preguntas frecuentes con la matriz de clientes probados.
- `Corrección` Los cuerpos de petición hostiles se rechazan antes de que un analizador entre en ellos: JSON anidado a más de 64 niveles, un cuerpo que repite un id de petición y un id todavía en curso en la misma sesión (que dejaba la primera petición sin respuesta) reciben 400 con un error JSON-RPC; un flujo GET sin sesión activa recibe 400 / 404 en vez de un flujo de eventos vacío. Las pruebas JVM y en dispositivo (API 28 / 31 / 33 / 35) cubren cuerpos demasiado largos o profundos, UTF-8 inválido, métodos desconocidos, combinaciones de cabeceras, base64 grande y 64 sesiones simultáneas.
- `Corrección` Matriz de ciclo de vida (P6): una petición que lleva el id de sesión de un proceso de escucha anterior (tras matar el escuchador o reiniciarlo por una rotación del token) se deja al 404 del transporte para que el cliente vuelva a hacer initialize, en vez de abrir una solicitud de emparejamiento bajo el User-Agent de la petición para un cliente ya emparejado; las notificaciones de emparejamiento que un proceso de escucha muerto dejó en el panel se borran al arrancar el siguiente escuchador. La muerte del host, la del escuchador, la de ambos a la vez, la detención forzosa desde los ajustes del sistema, la rotación del token con una sesión activa y un emparejamiento pendiente a través de la muerte del host y del escuchador quedan registrados con sus estados esperados y rutas de recuperación en docs/dev/lifecycle-matrix.md y verificados en dispositivos.
- `Mejora` La verificación de compilación rechaza dependencias nativas accidentales y genera un informe JSON
- `Dependencia` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) sobre el motor Ktor 3.5.1 CIO
- `Dependencia` Se agrega Ktor 3.5.1 `ktor-server-test-host` para las pruebas de transporte en JVM (solo ambito de pruebas)
- `Dependencia` Se agrega `mcp-server-api.aar` (modulo `plugin-api/mcp-server-api` de AutoJs6, compilacion del anfitrion 6.8.0 / 5279, MPL 2.0) como contrato Binder entre AutoJs6 y el plugin, con el hash fijado en `locks/host-api-aars.lock`
- `Dependencia` Actualizar los AAR common-plugin-api y mcp-server-api del anfitrión P4: extensión opcional de ajustes v1, orden AIDL sin cambios, hashes SHA-256 y compatibilidad con SDK 36.

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación y verificación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente; los usuarios normales pueden instalar simplemente el APK precompilado de la página Releases.

Compilar un APK de depuración:

```powershell
.\gradlew.bat :app:assembleDebug
```

Ejecutar las pruebas unitarias JVM y compilar el APK de pruebas de instrumentación:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compilar el APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Recopilar el artefacto de release y añadir la versión y el resumen CRC32 a su nombre de archivo:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verificar que las fuentes de documentación multilingüe y los artefactos generados están sincronizados (también lo exige la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilación requiere JDK 21 o posterior y Android SDK 37; las versiones de Gradle y de los plugins se gestionan de forma centralizada mediante `version.properties` e `io.github.supermonster003.autojs6-platform-versions`.

******

### Localización y generación de documentación

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

Los archivos JSON de idioma en `.readme/` y `.changelog/` son la única fuente del README, las instrucciones del centro de plugins y el registro de cambios. Edite siempre esas fuentes JSON y vuelva a ejecutar `py .python/generate_markdown.py`; los artefactos generados de README, `plugin_instruction.md` y registro de cambios nunca se editan a mano. Ejecute `py .python/generate_markdown.py --check` para verificar todos los artefactos generados.

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/LICENSE). Los componentes de terceros y sus licencias se listan en los [Avisos de terceros](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentación de AutoJs6: https://docs.autojs6.com
- Especificación del Model Context Protocol: https://modelcontextprotocol.io
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/THIRD_PARTY_NOTICES.md


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/docs/16kb.md)
