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
