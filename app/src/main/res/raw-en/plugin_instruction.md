MCP Server turns an Android device running AutoJs6 into a [Model Context Protocol](https://modelcontextprotocol.io) server. AI agents on a PC, such as Claude Code, Cursor, or the MCP Inspector, connect to the phone over USB or Wi-Fi and use tools to run scripts, read logs, inspect the accessibility node tree, tap and type, take screenshots, and work with files and apps.

Version 1.0.2: 37 tools (33 enabled by default), MCP resources and prompts, an AutoJs6 drawer switch and a plugin settings page. Requires AutoJs6 6.8.0 (build 5279) or later; the optional autojs6://docs/ resources also need the AutoJs6 Offline Docs plugin and a host with its relay methods. Progress and evidence are tracked in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) on a device with AutoJs6 build 5279 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `MCP Server` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Turn on MCP Server in the AutoJs6 drawer. Long-press its title to open settings, or use Settings in its Plugin Center entry. Copy the configuration for your PC client.
4. On the PC, run `adb forward tcp:9637 tcp:9637` and point the MCP client at `http://127.0.0.1:9637/mcp` with the token as a bearer credential.
5. Confirm the first pairing request on the phone. Stop the server from the drawer, settings, or the notification when finished.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) for the connection guide and the current progress.
