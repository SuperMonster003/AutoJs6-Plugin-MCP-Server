package io.github.supermonster003.autojs6.plugin.mcp.server.tools

/**
 * The switchable tool groups (roadmap P2.3, decision D6): the user enables or disables a whole
 * group on the phone; disabled groups vanish from `tools/list` and answer `TOOL_DISABLED`.
 *
 * Groups with a [family] are backed by an AutoJs6 plugin (roadmap P11, decision D27): their rows
 * are listed only while the open host session grants their methods and the host reports that
 * family as available (`app.pluginCapabilities`); see [PluginToolAvailability]. They are read-only
 * tools and default to on, like the `screen` group.
 */
enum class ToolGroup(val id: String, val defaultEnabled: Boolean, val family: String? = null) {

    SCRIPT("script", true),
    UI("ui", true),
    UI_GESTURE("ui_gesture", false),
    SCREEN("screen", true),
    FILES("files", true),
    FILES_DELETE("files_delete", false),
    DEVICE("device", true),
    SHELL("shell", false),
    OCR("ocr", true, family = "ocr"),
    BARCODE("barcode", true, family = "barcode"),
    OPENCV("opencv", true, family = "opencv"),
    PINYIN("pinyin", true, family = "pinyin"),
    PINYIN4J("pinyin4j", true, family = "pinyin4j"),
    MEDIAINFO("mediainfo", true, family = "mediainfo");

    /** True for the groups whose tools exist only while the matching AutoJs6 plugin is installed and enabled. */
    val isPluginFamily: Boolean
        get() = family != null

    companion object {

        val IDS: List<String> = entries.map { it.id }

        /** The plugin-family groups, in the order the settings page and `device_info` list them. */
        val PLUGIN_FAMILIES: List<ToolGroup> = entries.filter { it.isPluginFamily }

        fun fromId(id: String?): ToolGroup? = entries.firstOrNull { it.id == id }
    }
}
