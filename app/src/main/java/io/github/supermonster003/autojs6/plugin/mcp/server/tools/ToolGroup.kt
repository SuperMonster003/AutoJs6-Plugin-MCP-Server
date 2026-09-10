package io.github.supermonster003.autojs6.plugin.mcp.server.tools

/**
 * The switchable tool groups (roadmap P2.3, decision D6): the user enables or disables a whole
 * group on the phone; disabled groups vanish from `tools/list` and answer `TOOL_DISABLED`.
 */
enum class ToolGroup(val id: String, val defaultEnabled: Boolean) {

    SCRIPT("script", true),
    UI("ui", true),
    UI_GESTURE("ui_gesture", false),
    SCREEN("screen", true),
    FILES("files", true),
    FILES_DELETE("files_delete", false),
    DEVICE("device", true),
    SHELL("shell", false);

    companion object {

        val IDS: List<String> = entries.map { it.id }

        fun fromId(id: String?): ToolGroup? = entries.firstOrNull { it.id == id }
    }
}
