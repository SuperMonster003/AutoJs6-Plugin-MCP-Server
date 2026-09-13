package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import io.github.supermonster003.autojs6.plugin.mcp.server.R

class ReleaseHistoryActivity : SettingsPageActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page(getString(R.string.settings_release_history))
        val text = ReleaseHistory.load(resources.configuration.locales[0]) { assets.open(it).bufferedReader().use { reader -> reader.readText() } }
        val display = SpannableStringBuilder()
        // The generated history contains headings, bullets, bold labels and links; no HTML or network content.
        text.lineSequence().forEach { raw ->
            if (raw.startsWith("<!--") || raw.startsWith("[//]:") || raw.trim() == "---") return@forEach
            val heading = raw.startsWith('#')
            val line = raw.trimStart('#', ' ').replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
                .replace("**", "").replace("`", "")
            val start = display.length
            display.append(line).append('\n')
            if (heading) {
                display.setSpan(StyleSpan(Typeface.BOLD), start, display.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                display.setSpan(RelativeSizeSpan(1.25f), start, display.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        label(display, content, 16f).setTextIsSelectable(true)
    }
}
