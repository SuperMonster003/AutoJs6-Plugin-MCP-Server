package io.github.supermonster003.autojs6.plugin.mcp.server.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.mcp.server.R
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.PageColors

/** The app bar colors of the plugin's pages: the host's primary color, with black or white text and icons. */
internal data class McpBarColors(val container: Color, val content: Color)

private val LocalMcpBarColors = staticCompositionLocalOf { McpBarColors(Color(0xff00695c), Color.White) }

/**
 * The Material 3 theme of the plugin's Compose pages, mapped from [PageColors] so they look like the View settings
 * pages: the accent (adjusted for contrast) is `primary`, the button background is `primaryContainer`, and the
 * night mode is the host's (not the system's).
 */
@Composable
internal fun McpPageTheme(colors: PageColors, content: @Composable () -> Unit) {
    val base = if (colors.dark) darkColorScheme() else lightColorScheme()
    val text = Color(colors.text)
    val scheme = base.copy(
        primary = Color(colors.control),
        primaryContainer = Color(colors.controlContainer),
        onPrimaryContainer = Color(colors.control),
        background = Color(colors.pageBackground),
        onBackground = text,
        surface = Color(colors.surface),
        onSurface = text,
        onSurfaceVariant = Color(colors.secondary),
        outlineVariant = Color(colors.divider),
    )
    val typography = Typography().let { type ->
        type.copy(
            titleLarge = type.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            titleMedium = type.titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
            bodyMedium = type.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = type.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            labelMedium = type.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
        )
    }
    CompositionLocalProvider(LocalMcpBarColors provides McpBarColors(Color(colors.primary), Color(colors.onPrimary))) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/**
 * A page with the plugin's standard app bar (the host's primary color, like the View settings pages): the back
 * button, the title with an optional subtitle below it, and optional actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun McpScreen(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val bar = LocalMcpBarColors.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("mcp.topBar"),
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                                color = bar.content.copy(alpha = .8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("mcp.topBar.navigation")) {
                        Icon(painterResource(R.drawable.ic_settings_back), contentDescription = stringResource(R.string.settings_back))
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bar.container,
                    scrolledContainerColor = bar.container,
                    navigationIconContentColor = bar.content,
                    titleContentColor = bar.content,
                    actionIconContentColor = bar.content,
                ),
            )
        },
        content = content,
    )
}
