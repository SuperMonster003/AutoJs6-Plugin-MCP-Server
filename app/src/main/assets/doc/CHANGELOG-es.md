******

### Historial de versiones

******

# v1.0.0

###### 2026/09/10

* `Aviso` Vista previa de desarrollo: el plugin se registra en el centro de plugins de AutoJs6, pero el punto de conexión MCP y sus herramientas aún no están disponibles
* `Función` Identidad de plugin `mcp-server` con el servicio INFO, la Wake Activity y el esqueleto del servicio `org.autojs.plugin.MCP_SERVER` para el descubrimiento por el anfitrión
* `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
* `Función` Punto de conexión Streamable HTTP en `http://127.0.0.1:9637/mcp` con la herramienta `device_ping`, alojado por un servicio en primer plano que adb o el anfitrión pueden activar y desactivar (vista previa de desarrollo)
* `Función` Refuerzo del transporte del punto final `/mcp`: la direccion y el puerto de enlace provienen del almacen de configuracion del servidor, el cuerpo de la solicitud se limita a 1 MiB, las conexiones inactivas se cierran a los 60 s, y un puerto ocupado o un enlace rechazado termina en un estado `port_in_use` / `bind_failed` con una pista en lugar de un fallo
* `Función` Proteccion contra DNS rebinding delante del transporte del SDK: el modo loopback solo acepta `localhost` / `127.0.0.1` / `[::1]` como `Host`, el modo LAN agrega las direcciones IPv4 actuales del dispositivo y nombres de host adicionales opcionales y los actualiza cuando cambia la red; los origenes de navegador se rechazan salvo que el interruptor de "modo desarrollador" admita el origen loopback del Inspector mediante CORS
* `Función` Identidad del servidor `autojs6-mcp-server` con la version del plugin y las capacidades tools (`listChanged`), resources y prompts; `tools/list` conserva el orden de registro para que los clientes puedan almacenarlo en cache
* `Dependencia` MCP Kotlin SDK 0.15.0 (`kotlin-sdk-server`) sobre el motor Ktor 3.5.1 CIO
* `Dependencia` Se agrega Ktor 3.5.1 `ktor-server-test-host` para las pruebas de transporte en JVM (solo ambito de pruebas)
