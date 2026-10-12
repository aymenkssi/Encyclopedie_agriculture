package org.agripedia.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIcon(categoryId: String): ImageVector = when (categoryId) {
    "vegetable" -> Icons.Outlined.Eco
    "fruit" -> Icons.Outlined.Park
    "legume" -> Icons.Outlined.Spa
    "cereal" -> Icons.Outlined.Grass
    "aromatic" -> Icons.Outlined.LocalFlorist
    "industrial" -> Icons.Outlined.Factory
    else -> Icons.Outlined.Agriculture
}

@androidx.compose.runtime.Composable
fun suitabilityColors(level: org.agripedia.core.engine.SuitabilityLevel): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    val cs = androidx.compose.material3.MaterialTheme.colorScheme
    return when (level) {
        org.agripedia.core.engine.SuitabilityLevel.RECOMMENDED -> cs.primaryContainer to cs.onPrimaryContainer
        org.agripedia.core.engine.SuitabilityLevel.POSSIBLE -> cs.secondaryContainer to cs.onSecondaryContainer
        org.agripedia.core.engine.SuitabilityLevel.NOT_RECOMMENDED -> cs.errorContainer to cs.onErrorContainer
        org.agripedia.core.engine.SuitabilityLevel.UNKNOWN -> cs.surfaceVariant to cs.onSurfaceVariant
    }
}
