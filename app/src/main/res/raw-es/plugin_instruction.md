MCP Server convierte un dispositivo Android con AutoJs6 en un servidor [Model Context Protocol](https://modelcontextprotocol.io). Los agentes de IA del PC, como Claude Code, Cursor o MCP Inspector, se conectan al teléfono por USB o Wi-Fi y usan herramientas para ejecutar scripts, leer registros, inspeccionar el árbol de nodos de accesibilidad, tocar y escribir, tomar capturas de pantalla y trabajar con archivos y aplicaciones.

El proyecto está en la etapa de esqueleto: esta versión registra el plugin en el centro de plugins de AutoJs6 y prepara la infraestructura de compilación, documentación y pruebas. El punto de conexión MCP y sus herramientas aún no están disponibles. El progreso se sigue punto por punto en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Uso

1. Instale el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) en un dispositivo con AutoJs6 build 5278 (6.8.0) o posterior.
2. Abra el centro de plugins de AutoJs6, confirme que `MCP Server` se reconoce y actívelo. Los paquetes oficiales superan la verificación de firma automáticamente.
3. Encienda el servidor MCP desde el panel lateral de AutoJs6 o la página de ajustes del plugin; el teléfono muestra la dirección del punto de conexión y el token de emparejamiento.
4. En el PC, ejecute `adb forward tcp:9637 tcp:9637` y apunte el cliente MCP a `http://127.0.0.1:9637/mcp` con el token como credencial bearer.

Consulte el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) para la guía de conexión y el progreso actual.
