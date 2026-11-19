package io.github.supermonster003.autojs6.plugin.mcp.server.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.github.supermonster003.autojs6.plugin.mcp.server.R
import io.github.supermonster003.autojs6.plugin.mcp.server.ui.ReleaseHistoryEntry

/** The versions shown expanded; saved as a list so the choice survives rotation and process death. */
private val ExpandedVersionsSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() },
)

/**
 * Release history (`AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md`, model: 3-Maple Image): the title "Release history"
 * with the plugin name as the subtitle and one group per version. A collapsed version shows its date and how many
 * items each category has; an expanded one lists its items under each category, the category named once. The newest
 * version starts expanded; the top bar expands or collapses all of them.
 */
@Composable
internal fun ReleaseHistoryScreen(entries: List<ReleaseHistoryEntry>, onBack: () -> Unit) {
    var expanded by rememberSaveable(stateSaver = ExpandedVersionsSaver) {
        mutableStateOf(entries.firstOrNull()?.let { setOf(it.version) }.orEmpty())
    }
    val allExpanded = entries.isNotEmpty() && entries.all { it.version in expanded }
    McpScreen(
        title = stringResource(R.string.settings_release_history),
        subtitle = stringResource(R.string.app_name),
        onBack = onBack,
        actions = {
            if (entries.isNotEmpty()) {
                IconButton(
                    onClick = { expanded = if (allExpanded) emptySet() else entries.mapTo(mutableSetOf()) { it.version } },
                    modifier = Modifier.testTag("mcp.releaseHistory.toggleAll"),
                ) {
                    Icon(
                        painter = painterResource(if (allExpanded) R.drawable.ic_unfold_less_24 else R.drawable.ic_unfold_more_24),
                        contentDescription = stringResource(
                            if (allExpanded) R.string.settings_release_history_collapse_all else R.string.settings_release_history_expand_all,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                ReleaseHistoryEmptyState()
            }
            return@McpScreen
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("mcp.releaseHistory.list")) {
            itemsIndexed(entries, key = { _, entry -> entry.version }) { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = ReleaseHistoryDefaults.horizontalPadding),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                val isExpanded = entry.version in expanded
                ReleaseEntry(
                    entry = entry,
                    expanded = isExpanded,
                    onToggle = { expanded = if (isExpanded) expanded - entry.version else expanded + entry.version },
                )
            }
        }
    }
}

/** Dimensions of the release history page (`AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md`, layout). */
internal object ReleaseHistoryDefaults {
    val horizontalPadding = 24.dp
    val headerEndPadding = 16.dp
    val verticalPadding = 16.dp
    val bulletSlot = 20.dp
    val bulletSize = 5.dp
    val bulletStart = 6.dp
    val groupSpacing = 14.dp
    val itemSpacing = 6.dp
}

/** Shown when no bundled changelog holds a release: a warning icon (decorative) and one line of text. */
@Composable
private fun ReleaseHistoryEmptyState() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp).testTag("mcp.releaseHistory.empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_release_history_load_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseEntry(entry: ReleaseHistoryEntry, expanded: Boolean, onToggle: () -> Unit) {
    val state = stringResource(if (expanded) R.string.settings_release_history_expanded else R.string.settings_release_history_collapsed)
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(modifier = Modifier.fillMaxWidth()) {
        // The header is the one focus stop of a release (Tab walks the history, Enter expands or collapses it) and
        // reads version, date, counts and state in one go.
        Column(
            modifier = Modifier
                .testTag("mcp.releaseHistory.entry.${entry.version}")
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = state }
                .padding(
                    start = ReleaseHistoryDefaults.horizontalPadding,
                    end = ReleaseHistoryDefaults.headerEndPadding,
                    top = ReleaseHistoryDefaults.verticalPadding,
                    bottom = if (expanded) 0.dp else ReleaseHistoryDefaults.verticalPadding,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.version,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (entry.date != null) {
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).rotate(chevronRotation),
                )
            }
            val counts = entry.categoryCounts
            if (!expanded && counts.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp).testTag("mcp.releaseHistory.counts.${entry.version}"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    counts.forEach { (tag, count) -> CategoryChip(tag, count) }
                }
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mcp.releaseHistory.body.${entry.version}")
                    // A screen reader reads the items of a release in one go after its header.
                    .semantics(mergeDescendants = true) {}
                    .padding(
                        start = ReleaseHistoryDefaults.horizontalPadding,
                        end = ReleaseHistoryDefaults.horizontalPadding,
                        bottom = ReleaseHistoryDefaults.verticalPadding,
                    ),
            ) {
                entry.groups.forEachIndexed { index, group ->
                    val top = if (index == 0) 10.dp else ReleaseHistoryDefaults.groupSpacing
                    if (group.tag != null) CategoryChip(group.tag, count = null, modifier = Modifier.padding(top = top))
                    group.items.forEachIndexed { itemIndex, text ->
                        val itemTop = if (group.tag == null && itemIndex == 0) top else ReleaseHistoryDefaults.itemSpacing
                        BulletItem(text, Modifier.padding(top = itemTop))
                    }
                }
            }
        }
    }
}

/** A category label (primary container, 10 dp corners), with the item count in the collapsed summary. */
@Composable
private fun CategoryChip(tag: String, count: Int?, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = tag, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = color)
        if (count != null) {
            Spacer(Modifier.width(4.dp))
            Text(text = count.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
        }
    }
}

/**
 * One item: a drawn dot (not a font glyph, so it looks the same with every font) centered on the first line, the text
 * beside it; wrapped lines align with the text, not with the dot.
 */
@Composable
private fun BulletItem(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyMedium
    val firstLine: Dp = with(LocalDensity.current) { if (style.lineHeight.isSpecified) style.lineHeight.toDp() else 20.dp }
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.width(ReleaseHistoryDefaults.bulletSlot).height(firstLine), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .padding(start = ReleaseHistoryDefaults.bulletStart)
                    .size(ReleaseHistoryDefaults.bulletSize)
                    .background(dotColor, CircleShape),
            )
        }
        Text(text = text, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}
