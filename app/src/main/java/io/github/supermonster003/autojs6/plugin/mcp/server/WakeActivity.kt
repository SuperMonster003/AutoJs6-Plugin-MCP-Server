package io.github.supermonster003.autojs6.plugin.mcp.server

import android.app.Activity
import android.os.Bundle

/**
 * Invisible activity started by the AutoJs6 plugin center to move the plugin out of the stopped
 * state on devices that keep newly installed apps stopped (`org.autojs.plugin.action.WAKE`).
 */
class WakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
