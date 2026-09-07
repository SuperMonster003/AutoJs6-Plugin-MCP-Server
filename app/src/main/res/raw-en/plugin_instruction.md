MCP Server turns an Android device running AutoJs6 into a [Model Context Protocol](https://modelcontextprotocol.io) server. AI agents on a PC, such as Claude Code, Cursor, or the MCP Inspector, connect to the phone over USB or Wi-Fi and use tools to run scripts, read logs, inspect the accessibility node tree, tap and type, take screenshots, and work with files and apps.

The project is in the skeleton stage: this release registers the plugin with the AutoJs6 plugin center and prepares the build, documentation, and test infrastructure. The MCP endpoint and its tools are not available yet. Progress is tracked item by item in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/releases) on a device with AutoJs6 build 5278 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `MCP Server` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Turn on the MCP server from the AutoJs6 drawer or the plugin settings page; the phone shows the endpoint address and the pairing token.
4. On the PC, run `adb forward tcp:9637 tcp:9637` and point the MCP client at `http://127.0.0.1:9637/mcp` with the token as a bearer credential.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server/blob/master/ROADMAP.md) for the connection guide and the current progress.
