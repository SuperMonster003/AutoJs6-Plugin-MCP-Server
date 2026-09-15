package io.github.supermonster003.autojs6.plugin.mcp.server.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toolbar
import io.github.supermonster003.autojs6.plugin.mcp.server.R

/** Native widgets keep API 24 support and inherit Android accessibility/keyboard behavior. */
abstract class SettingsPageActivity : Activity() {
    protected lateinit var appearance: HostAppearance
    protected lateinit var content: LinearLayout
    private var backButton: ImageButton? = null
    protected val textColor get() = if (appearance.dark) Color.WHITE else 0xff202522.toInt()
    protected val secondary get() = if (appearance.dark) 0xffbac7c1.toInt() else 0xff4e5d55.toInt()
    protected val surface get() = if (appearance.dark) 0xff202923.toInt() else Color.WHITE

    override fun attachBaseContext(newBase: Context) {
        appearance = HostAppearance.read(newBase)
        super.attachBaseContext(appearance.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (appearance.dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
        super.onCreate(savedInstanceState)
    }

    protected fun page(titleText: String) {
        title = titleText
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (appearance.dark) 0xff131a16.toInt() else 0xfff2f6f3.toInt())
        }
        val toolbar = Toolbar(this).apply {
            title = titleText
            val foreground = if (Color.luminance(appearance.primary) > .179) Color.BLACK else Color.WHITE
            setTitleTextColor(foreground)
            setBackgroundColor(appearance.primary)
            navigationIcon = getDrawable(R.drawable.ic_settings_back)?.mutate()?.apply { setTint(foreground) }
            navigationContentDescription = getString(R.string.settings_back)
            setNavigationOnClickListener { finish() }
            // The Material toolbar style blocks keyboard focus on touch screens and forms its own
            // navigation cluster; one plain Tab cycle should reach the back button and the page.
            touchscreenBlocksFocus = false
            if (Build.VERSION.SDK_INT >= 26) isKeyboardNavigationCluster = false
        }
        backButton = (0 until toolbar.childCount).map(toolbar::getChildAt).filterIsInstance<ImageButton>().firstOrNull()?.apply { id = View.generateViewId() }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(24)) }
        root.addView(ScrollView(this).apply { isFillViewport = true; addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        @Suppress("DEPRECATION")
        window.statusBarColor = appearance.primary
        @Suppress("DEPRECATION")
        window.navigationBarColor = if (appearance.dark || Build.VERSION.SDK_INT < 26) 0xff131a16.toInt() else 0xfff2f6f3.toInt()
        val lightStatus = if (Build.VERSION.SDK_INT >= 35) !appearance.dark else Color.luminance(appearance.primary) > .179
        if (Build.VERSION.SDK_INT >= 30) {
            val flags = (if (lightStatus) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0) or
                (if (!appearance.dark) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0)
            window.insetsController?.setSystemBarsAppearance(flags,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (if (lightStatus) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0) or
                (if (Build.VERSION.SDK_INT >= 26 && !appearance.dark) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Close the Tab cycle explicitly: Android 7 sorts focusables by screen position, so content
        // scrolled above the toolbar would otherwise win the wrap-around and hide the back button.
        val back = backButton ?: return
        val focusables = ArrayList<View>().also { content.addFocusables(it, View.FOCUS_FORWARD, View.FOCUSABLES_ALL) }
        val first = focusables.firstOrNull() ?: return
        val last = focusables.last()
        if (first.id == View.NO_ID) first.id = View.generateViewId()
        back.nextFocusForwardId = first.id
        last.nextFocusForwardId = back.id
    }

    protected fun card(titleId: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = GradientDrawable().apply { setColor(surface); cornerRadius = dp(16).toFloat() }
        content.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        label(getString(titleId), this, 19f).apply {
            setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        }
    }

    protected fun label(value: CharSequence, parent: LinearLayout, size: Float = 15f): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(textColor)
        gravity = Gravity.START
        setPadding(0, dp(6), 0, dp(6))
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    protected fun button(titleId: Int, parent: LinearLayout, action: () -> Unit): Button = Button(this).apply {
        setText(titleId); isAllCaps = false; minHeight = dp(48)
        setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()), intArrayOf(textColor, secondary)))
        backgroundTintList = ColorStateList.valueOf(if (appearance.dark) 0xff304238.toInt() else 0xffe7f0eb.toInt())
        setOnClickListener { action() }
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
    }

    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density + .5f).toInt()
}
