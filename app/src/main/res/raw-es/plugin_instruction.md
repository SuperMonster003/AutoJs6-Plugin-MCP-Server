MCP Server convierte un dispositivo Android con AutoJs6 en un servidor [Model Context Protocol](https://modelcontextprotocol.io). Los agentes de IA del PC, como Claude Code, Cursor o MCP Inspector, se conectan al teléfono por USB o Wi-Fi y usan herramientas para ejecutar scripts, leer registros, inspeccionar el árbol de nodos de accesibilidad, tocar y escribir, tomar capturas de pantalla y trabajar con archivos y aplicaciones.

Versión 1.0.1: 37 herramientas (33 activadas por defecto), recursos y plantillas MCP, un interruptor en el panel de AutoJs6 y una página de ajustes del complemento. Requiere AutoJs6 6.8.0 (compilación 5279) o posterior; los recursos opcionales autojs6://docs/ necesitan además el plugin AutoJs6 Offline Docs y un anfitrión con sus métodos de retransmisión. El progreso y las evidencias se registran en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Uso

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) en un dispositivo con AutoJs6 build 5279 (6.8.0) o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `MCP Server` se reconoce y actívelo. Los paquetes oficiales superan la verificación de firma automáticamente.
3. Activa MCP Server en el panel de AutoJs6. Mantén pulsado su título o abre Ajustes desde el Centro de complementos. Copia la configuración del cliente de PC.
4. En el PC, ejecute `adb forward tcp:9637 tcp:9637` y apunte el cliente MCP a `http://127.0.0.1:9637/mcp` con el token como credencial bearer.
5. Confirma la primera solicitud de vinculación en el teléfono. Al terminar, detén el servidor desde el panel, los ajustes o la notificación.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
