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

Vista previa P4: 37 herramientas, 33 activadas por defecto, con un interruptor en AutoJs6 y una página de ajustes del complemento. Requiere la compilación AutoJs6 de P4 correspondiente. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

******

### Funciones previstas

******

La hoja de ruta entrega las siguientes capacidades por etapas:

- Ajustes del teléfono para estado del servidor, USB, puerto y red local, token, revocación de vínculos, grupos y root, modo de desarrollador, configuraciones copiables de Claude Code / Cursor / Codex / HTTP e historial de versiones, siguiendo el aspecto de AutoJs6. Los cambios de red reinician el servidor activo; tokens y permisos se aplican inmediatamente. Los diálogos secretos bloquean capturas.
- Ejecución de scripts: ejecutar JavaScript desde texto o desde un archivo dentro de AutoJs6, listar y detener motores, y leer la salida reciente de la consola.
- Interfaz de accesibilidad: volcar el árbol de nodos en un formato de texto compacto, buscar nodos con la sintaxis de selectores de AutoJs6, hacer clic, mantener pulsado, desplazar, establecer texto y pulsar teclas globales como Atrás e Inicio.
- Grupo de capturas (P3.3): screen_capture devuelve imágenes MCP con recorte, scale o maxWidth, JPEG / PNG / WebP y control de calidad. Valores predeterminados: JPEG, calidad 70 y lado mayor de 1280 px. Si base64 supera 4 MiB, se reintenta con menor calidad o tamaño y los metadatos indican el ajuste. screen_state informa del estado, tamaño, orientación y densidad. El catálogo incluye 37 herramientas. La alternativa MediaProjection requiere AutoJs6 compilado el 2026-09-13 o después y autorización en el teléfono, reutilizada por la sesión del host.
- Herramientas del directorio de trabajo (P3.4): files_list / stat / read / write / mkdir / rename / delete, editor_open con fila y columna desde 1, app_launch / list, clipboard_get / set, device_ensure_accessibility, toast y shell_exec. Lectura binaria en base64, hasta 1 MiB de datos originales. La escritura respeta además el límite del host (normalmente 96 KiB con escapes JSON). El borrado y Shell están desactivados inicialmente; root requiere allowShellRoot y el permiso shell.root del host. Se necesita el host P3.4 correspondiente.
- Los recursos MCP (P3.5) ofrecen archivos de trabajo de solo lectura, ejemplos del anfitrión, información del dispositivo y salida reciente de consola, respetando el emparejamiento y los grupos activos. Las lecturas de texto y datos binarios indican el truncamiento. Las plantillas write_autojs6_script, automate_task y debug_selector están disponibles en inglés y chino, con inglés para los demás idiomas del teléfono.
- Rutas de conexión: USB mediante `adb forward`, red local con activación explícita, un puente stdio en el PC y un túnel público opcional con OAuth 2.1.
- Seguridad: un token bearer rotatorio, confirmación de emparejamiento en el teléfono en el primer uso e interruptores de herramientas por grupo; el servidor solo escucha en la interfaz de bucle local de forma predeterminada.

******

### Uso

******

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) en un dispositivo con AutoJs6 build 5279 (6.8.0) o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `MCP Server` se reconoce y actívelo. Los paquetes oficiales superan la verificación de firma automáticamente.
3. Activa MCP Server en el panel de AutoJs6. Mantén pulsado su título o abre Ajustes desde el Centro de complementos. Copia la configuración del cliente de PC.
4. En el PC, ejecute `adb forward tcp:9637 tcp:9637` y apunte el cliente MCP a `http://127.0.0.1:9637/mcp` con el token como credencial bearer.
5. Confirma la primera solicitud de vinculación en el teléfono. Al terminar, detén el servidor desde el panel, los ajustes o la notificación.

> Interruptor MCP Server con guías de instalación, activación, autorización y compatibilidad; sincronización con Detener en la notificación; ajustes conservados al reconectar; acceso verificado a la misma página desde el panel y el Centro de complementos. Restaura el servidor al abrir AutoJs6 salvo que el usuario lo detuviera sin el anfitrión; no arranca con el dispositivo.

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

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, por lo que solo AutoJs6 puede enlazarse a ellos.
- El permiso INTERNET se usa solo para el propio receptor HTTP del plugin; el plugin no realiza solicitudes salientes ni recopila datos.
- Las llamadas a herramientas pasan por el intermediario de capacidades de AutoJs6 y nunca exceden lo que el propio anfitrión tiene permitido; los grupos peligrosos, como comandos de shell y eliminación de archivos, permanecen desactivados hasta que el usuario los habilite.
- Las copias de seguridad están desactivadas y los tokens se guardan solo en el almacenamiento privado del plugin.

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

La compilación requiere JDK 21 o posterior y Android SDK 36; las versiones de Gradle y de los plugins se gestionan de forma centralizada mediante `version.properties` e `io.github.supermonster003.autojs6-platform-versions`.

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
