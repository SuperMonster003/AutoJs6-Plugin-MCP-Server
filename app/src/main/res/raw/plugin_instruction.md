MCP Server turns an Android device running AutoJs6 into a [Model Context Protocol](https://modelcontextprotocol.io) server. AI agents on a PC, such as Claude Code, Cursor, or the MCP Inspector, connect to the phone over USB or Wi-Fi and use tools to run scripts, read logs, inspect the accessibility node tree, tap and type, take screenshots, and work with files and apps.

Development preview through P3.3: the authenticated MCP endpoint, pairing, script and UI tools, and screenshots are implemented. screen_capture returns JPEG, PNG or WebP images; screen_state reports screen dimensions and orientation. The drawer switch and settings page are still planned in P4. Progress and device evidence are in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) on a device with AutoJs6 build 5279 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `MCP Server` is recognized, and enable it. Official release packages pass signature verification automatically.
3. For this development preview, use the adb control plane and a host test session described in the developer notes; the drawer switch and plugin settings page are planned in P4.
4. On the PC, run `adb forward tcp:9637 tcp:9637` and point the MCP client at `http://127.0.0.1:9637/mcp` with the token as a bearer credential.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) for the connection guide and the current progress.
