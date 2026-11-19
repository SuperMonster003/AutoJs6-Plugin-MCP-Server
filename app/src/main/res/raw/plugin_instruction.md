MCP Server lets AI clients on your computer, such as Claude Code and Cursor, operate this phone through the [Model Context Protocol](https://modelcontextprotocol.io). Once connected over USB or the local network, a client can use AutoJs6 to run scripts, inspect and operate the screen, take screenshots, and read and write files in the working directory. Every new client must be confirmed on the phone the first time it calls a tool.

### Getting started

1. Turn on the "MCP Server" switch in the AutoJs6 drawer, and enable the plugin when prompted the first time.
2. Long-press the item, or open the plugin's settings in the "Plugin center", then copy the configuration of your client from "Client configuration" and add it to the MCP client on your computer.
3. Connect the phone to the computer over USB and run the adb forward command shown under "Connection" on the computer (tap "Copy adb forward command" to copy it).
4. When the client calls a tool for the first time, tap "Allow" on the phone to confirm the pairing.

### Main features

- **Scripts and screen**: Clients can run scripts in AutoJs6 and read the console output, dump the accessibility node tree, find controls and tap, type or scroll, and take screenshots.
- **Files and device**: Read and write files in the AutoJs6 working directory, launch apps, and read or write the clipboard. With "Allow reading shared storage" on, clients can also read places such as the gallery and the Download folder.
- **Tool permissions**: Turn the tool groups available to clients on or off; a group that is off does not appear in the client's tool list. "File deletion", "Coordinate gestures" and "Shell commands" are off by default.
- **Plugin tools**: With plugins such as OCR, Barcode and OpenCV installed, clients can recognize text and barcodes and find images and colors. Tools that other plugins declare are turned on in "Plugin tools".
- **Registered scripts**: Scripts registered with an `@agent` comment are listed to clients, and the model can run them with the registered parameters; for scripts that need confirmation, the client asks for your consent before running them.
- **Token and pairing**: Clients connect with an access token, of which the settings page shows only the last 4 characters. Revoke pairings in "Paired clients"; after "Rotate token", the old token stops working at once.
- **Local network connections**: With "Allow local network connections" on, computers on the same network can connect without USB; the token and pairing confirmation still apply.
- **Stop automatically when idle**: Stop the server when no request arrives within the chosen time; off by default.

### Requirements

- Android 7.0 or later, AutoJs6 6.8.0 or later.
- An MCP client on the computer; USB connections also need adb. Clients that only support stdio, such as Claude Desktop, need the separate [bridge](https://github.com/SuperMonster003/AutoJs6-MCP-Bridge).
- Screen operations and screenshots need the AutoJs6 accessibility service; on some devices, screenshots also need permission granted on the phone.
- The notification permission lets you confirm pairing requests from any screen; Android 17 and later also require allowing this plugin to access nearby devices (for the local network).
- Plugin tools such as text and barcode recognition need the corresponding plugin installed and enabled in AutoJs6.

### FAQ

**Why the client reports 401 Unauthorized**

The token is missing, mistyped or rotated. Copy the configuration from the settings page again; after rotating the token, every client needs the new one.

**When the connection is refused**

Make sure the "MCP Server" switch in the drawer is on. Reconnecting the device or restarting adb clears the port forwarding; run the adb forward command on the computer again.

**Why the server stops when the screen is off**

When the system restricts the battery usage of this app, it may stop the server while the screen is off. The settings page then shows a warning; tap "Battery settings" and choose "Unrestricted" for MCP Server.

**Why a tool does not appear in the client**

Its group is off in "Tool permissions", or the plugin it needs is not installed or enabled in AutoJs6. The client's tool list updates automatically once it is enabled.

See the [project home page](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) for setting up each client, the tool list and the security notes.
