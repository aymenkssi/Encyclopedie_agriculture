package org.agripedia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.agripedia.app.R
import org.agripedia.app.ui.categoryIcon
import org.agripedia.app.ui.label
import org.agripedia.app.ui.monthName
import org.agripedia.core.model.Plant
import org.agripedia.core.model.ValidationStatus

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun StatusBadge(status: ValidationStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        ValidationStatus.VERIFIED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ValidationStatus.INDICATIVE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ValidationStatus.TO_VERIFY -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Pill(label(status), bg, fg, modifier)
}

@Composable
fun Pill(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}

/** Banner reminding that the dataset is a demonstration, not validated agronomic data. */
@Composable
fun DemoBanner(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Science, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.offline_banner), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Outlined.Eco, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) { Spacer(Modifier.height(16.dp)); action() }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    EmptyState(stringResource(R.string.error_title), message, modifier, Icons.Filled.ErrorOutline,
        action = onRetry?.let { { Button(onClick = it) { Text(stringResource(R.string.retry)) } } })
}

/** Placeholder illustration: category icon on a tinted circle (photos can be added per plant later). */
@Composable
fun PlantAvatar(plant: Plant, size: Int = 48) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(categoryIcon(plant.categoryId), null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size((size * 0.55).dp))
    }
}

@Composable
fun PlantRow(
    plant: Plant, name: String, subtitle: String?, onClick: () -> Unit, modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null, badges: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick, modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PlantAvatar(plant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(plant.scientificName, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (badges != null) { Spacer(Modifier.height(6.dp)); badges() }
            }
            trailing?.invoke() ?: Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Compact plant card for horizontal carousels. */
@Composable
fun PlantTile(plant: Plant, name: String, caption: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.width(150.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp)) {
            PlantAvatar(plant, 40)
            Spacer(Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(value ?: stringResource(R.string.no_data), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f),
            fontStyle = if (value == null) FontStyle.Italic else FontStyle.Normal)
    }
}

@Composable
fun ElevatedSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/** 12-cell month strip; highlighted months are filled, preferred months are emphasized. */
@Composable
fun MonthStrip(months: Set<Int>, preferred: Set<Int> = emptySet(), current: Int? = null, color: Color = MaterialTheme.colorScheme.primary) {
    val described = months.sorted().joinToString { monthName(it) }
    Row(Modifier.fillMaxWidth().semantics { contentDescription = described }, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (m in 1..12) {
            val on = m in months
            Box(
                Modifier.weight(1f).height(if (m in preferred) 18.dp else 14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (on) color.copy(alpha = if (m in preferred || preferred.isEmpty()) 1f else 0.55f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (m == current) Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.takeIf { on } ?: MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
    }
}

@Composable
fun MonthAxis() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (m in 1..12) Text(monthName(m, short = true).take(1), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
