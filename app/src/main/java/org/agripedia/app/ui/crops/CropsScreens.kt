package org.agripedia.app.ui.crops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.agripedia.app.R
import org.agripedia.app.data.db.UserCropEntity
import org.agripedia.app.data.db.UserGardenEntity
import org.agripedia.app.data.db.UserNoteEntity
import org.agripedia.app.data.repo.CropStage
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.components.ChipFlow
import org.agripedia.app.ui.components.ConfirmDialog
import org.agripedia.app.ui.components.ElevatedSection
import org.agripedia.app.ui.components.EmptyState
import org.agripedia.app.ui.components.InfoNote
import org.agripedia.app.ui.components.InfoRow
import org.agripedia.app.ui.components.Pill
import org.agripedia.app.ui.components.PlantRow
import org.agripedia.app.ui.components.ScreenScaffold
import org.agripedia.app.ui.components.SectionHeader
import org.agripedia.app.ui.contentLang
import org.agripedia.app.ui.label
import org.agripedia.app.ui.navigation.Nav
import org.agripedia.app.ui.profile.NoteEditorDialog
import org.agripedia.app.ui.tr
import org.agripedia.core.engine.FarmType
import org.agripedia.core.model.Activity
import java.time.LocalDate

private fun stageOf(c: UserCropEntity) = runCatching { CropStage.valueOf(c.stage) }.getOrDefault(CropStage.PLANNED)
private fun areaText(a: Double?) = a?.let { (if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()) + " m²" }

@Composable
fun MyCropsScreen(k: Knowledge, appVm: AppViewModel, nav: Nav) {
    val users = appVm.container.userRepository
    val scope = rememberCoroutineScope()
    val gardens by users.gardens.collectAsStateWithLifecycle(initialValue = null)
    val crops by users.crops.collectAsStateWithLifecycle(initialValue = emptyList())
    val profile by appVm.profile.collectAsStateWithLifecycle()
    var newGarden by remember { mutableStateOf(false) }
    var addCrop by remember { mutableStateOf(false) }
    val lang = contentLang()

    ScreenScaffold(
        title = stringResource(R.string.tab_crops),
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { addCrop = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.add_crop)) })
        },
    ) { padding ->
        val list = gardens
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SectionHeader(stringResource(R.string.my_gardens)) {
                    TextButton(onClick = { newGarden = true }) { Text(stringResource(R.string.new_garden)) }
                }
            }
            if (list != null && list.isEmpty()) item {
                EmptyState(stringResource(R.string.no_garden_title), stringResource(R.string.no_garden_text), icon = Icons.Outlined.Yard,
                    action = { Button(onClick = { newGarden = true }) { Text(stringResource(R.string.new_garden)) } })
            }
            items(list.orEmpty(), key = { it.id }) { g ->
                val count = crops.count { it.gardenId == g.id && stageOf(it) != CropStage.FINISHED }
                Card(onClick = { nav.garden(g.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(g.name, style = MaterialTheme.typography.titleMedium)
                        Text(listOfNotNull(runCatching { FarmType.valueOf(g.farmType) }.getOrNull()?.let { label(it) }, areaText(g.areaM2),
                            g.regionId?.let { k.catalog.regionsById[it] }?.let { tr(it.names) }).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        Text(pluralCrops(count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            val active = crops.filter { stageOf(it) != CropStage.FINISHED }
            if (active.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.ongoing_crops)) }
                items(active, key = { "c-" + it.id }) { c -> CropRow(k, c, lang) { nav.crop(c.id) } }
            }
            val finished = crops.filter { stageOf(it) == CropStage.FINISHED }
            if (finished.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.history_crops)) }
                items(finished, key = { "f-" + it.id }) { c -> CropRow(k, c, lang) { nav.crop(c.id) } }
            }
        }
    }
    if (newGarden) GardenEditorDialog(k, null, profile?.regionId, onDismiss = { newGarden = false }) { name, type, area, region ->
        scope.launch { users.saveGarden(null, name, type, area, region) }
    }
    if (addCrop) AddCropDialog(k, appVm.container, onDismiss = { addCrop = false }, onAdded = { addCrop = false; nav.crop(it) })
}

@Composable
private fun pluralCrops(n: Int) = androidx.compose.ui.res.pluralStringResource(R.plurals.crops_count, n, n)

@Composable
private fun CropRow(k: Knowledge, c: UserCropEntity, lang: String, onClick: () -> Unit) {
    val plant = k.catalog.plantsById[c.plantId]
    if (plant == null) {
        ElevatedSection(Modifier.clickable(onClick = onClick)) { Text(stringResource(R.string.plant_not_found)) }
        return
    }
    PlantRow(plant, plant.displayName(lang),
        subtitle = "${label(runCatching { Activity.valueOf(c.startActivity) }.getOrDefault(Activity.PLANTING))} : ${LocalDate.ofEpochDay(c.startDate)}",
        onClick = onClick,
        badges = { Pill(label(stageOf(c)), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) })
}

@Composable
fun GardenDetailScreen(k: Knowledge, appVm: AppViewModel, nav: Nav, gardenId: String) {
    val users = appVm.container.userRepository
    val scope = rememberCoroutineScope()
    val garden by remember(gardenId) { users.garden(gardenId) }.collectAsStateWithLifecycle(initialValue = null)
    val plots by remember(gardenId) { users.plots(gardenId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val crops by remember(gardenId) { users.cropsOfGarden(gardenId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var edit by remember { mutableStateOf(false) }
    var newPlot by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addCrop by remember { mutableStateOf(false) }
    val lang = contentLang()
    val g: UserGardenEntity? = garden

    ScreenScaffold(
        title = g?.name ?: "", onBack = nav::back,
        actions = {
            if (g != null) {
                IconButton(onClick = { edit = true }) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
            }
        },
        floatingActionButton = {
            if (g != null && !g.deleted) ExtendedFloatingActionButton(onClick = { addCrop = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.add_crop)) })
        },
    ) { padding ->
        if (g == null || g.deleted) {
            EmptyState(stringResource(R.string.garden_not_found), "", Modifier.padding(padding))
            return@ScreenScaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ElevatedSection {
                    InfoRow(stringResource(R.string.garden_type), runCatching { FarmType.valueOf(g.farmType) }.getOrNull()?.let { label(it) })
                    InfoRow(stringResource(R.string.area_m2), areaText(g.areaM2))
                    InfoRow(stringResource(R.string.region), g.regionId?.let { k.catalog.regionsById[it] }?.let { tr(it.names) })
                    val used = plots.mapNotNull { it.areaM2 }.sum()
                    if (g.areaM2 != null && used > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.plots_area_used, areaText(used) ?: "", areaText(g.areaM2) ?: ""), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { (used / g.areaM2).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        if (used > g.areaM2) Text(stringResource(R.string.plots_area_exceeded), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                SectionHeader(stringResource(R.string.plots)) { TextButton(onClick = { newPlot = true }) { Text(stringResource(R.string.new_plot)) } }
                if (plots.isEmpty()) Text(stringResource(R.string.no_plots), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(plots, key = { "p-" + it.id }) { p ->
                val current = crops.filter { it.plotId == p.id && stageOf(it) != CropStage.FINISHED }
                ElevatedSection {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            Text(listOfNotNull(areaText(p.areaM2), p.soilId?.let { tr(k.catalog.soilTypesById[it]?.names) }, tr(k.catalog.methodsById[p.methodId]?.names))
                                .joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            Text(current.mapNotNull { k.catalog.plantsById[it.plantId]?.displayName(lang) }.joinToString(", ").ifEmpty { stringResource(R.string.plot_free) },
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { scope.launch { users.deletePlot(p) } }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.crops_in_garden)) }
            if (crops.isEmpty()) item { Text(stringResource(R.string.no_crops_in_garden), style = MaterialTheme.typography.bodySmall) }
            items(crops, key = { "c-" + it.id }) { c -> CropRow(k, c, lang) { nav.crop(c.id) } }
            item { InfoNote(stringResource(R.string.large_farm_future)) }
        }
    }
    if (edit && g != null) GardenEditorDialog(k, g, g.regionId, onDismiss = { edit = false }) { name, type, area, region ->
        scope.launch { users.saveGarden(g.id, name, type, area, region) }
    }
    if (newPlot) PlotEditorDialog(k, onDismiss = { newPlot = false }) { name, area, soil, method -> scope.launch { users.savePlot(gardenId, name, area, soil, method) } }
    if (confirmDelete && g != null) ConfirmDialog(stringResource(R.string.delete_garden), stringResource(R.string.delete_garden_text), stringResource(R.string.delete),
        onConfirm = { scope.launch { users.deleteGarden(g); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { nav.back() } } }, onDismiss = { confirmDelete = false })
    if (addCrop) AddCropDialog(k, appVm.container, fixedGardenId = gardenId, onDismiss = { addCrop = false }, onAdded = { addCrop = false })
}

@Composable
fun CropDetailScreen(k: Knowledge, appVm: AppViewModel, nav: Nav, cropId: String) {
    val users = appVm.container.userRepository
    val scope = rememberCoroutineScope()
    val crop by remember(cropId) { users.crop(cropId) }.collectAsStateWithLifecycle(initialValue = null)
    val tasks by remember(cropId) { users.tasksOfCrop(cropId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by remember(cropId) { users.notesOfCrop(cropId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<UserNoteEntity?>(null) }
    var newNote by remember { mutableStateOf(false) }
    val lang = contentLang()
    val c = crop
    val plant = c?.let { k.catalog.plantsById[it.plantId] }

    ScreenScaffold(
        title = plant?.displayName(lang) ?: "", onBack = nav::back,
        actions = {
            if (c != null) {
                IconButton(onClick = { newNote = true }) { Icon(Icons.AutoMirrored.Outlined.NoteAdd, stringResource(R.string.add_note)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
            }
        },
    ) { padding ->
        if (c == null || c.deleted) { EmptyState(stringResource(R.string.crop_not_found), "", Modifier.padding(padding)); return@ScreenScaffold }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ElevatedSection {
                    InfoRow(stringResource(R.string.start_activity), label(runCatching { Activity.valueOf(c.startActivity) }.getOrDefault(Activity.PLANTING)))
                    InfoRow(stringResource(R.string.start_date), LocalDate.ofEpochDay(c.startDate).toString())
                    InfoRow(stringResource(R.string.cultivation_method), tr(k.catalog.methodsById[c.methodId]?.names))
                    InfoRow(stringResource(R.string.quantity_optional), c.quantity)
                    plant?.cycleDays?.let { cy ->
                        val days = LocalDate.now().toEpochDay() - c.startDate
                        InfoRow(stringResource(R.string.days_since_start), days.toString())
                        val progress = (days / cy.min).toFloat().coerceIn(0f, 1f)
                        Text(stringResource(R.string.progress_to_harvest), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        InfoNote(stringResource(R.string.progress_estimate))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (plant != null) OutlinedButton(onClick = { nav.plant(plant.id) }) { Text(stringResource(R.string.open_sheet)) }
                }
            }
            item {
                SectionHeader(stringResource(R.string.stage))
                ChipFlow {
                    CropStage.entries.forEach { s -> FilterChip(stageOf(c) == s, { scope.launch { users.updateCropStage(c, s) } }, { Text(label(s)) }) }
                }
            }
            item { SectionHeader(stringResource(R.string.crop_tasks)) }
            if (tasks.isEmpty()) item { Text(stringResource(R.string.no_crop_tasks), style = MaterialTheme.typography.bodySmall) }
            items(tasks, key = { "t-" + it.id }) { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(t.done, { scope.launch { users.setTaskDone(t, it) } })
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyMedium)
                        Text(LocalDate.ofEpochDay(t.dueDate).toString() + if (t.origin == org.agripedia.app.data.repo.TaskOrigin.PLANNED) " · " + stringResource(R.string.estimated) else "",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.my_notes)) }
            if (notes.isEmpty()) item { Text(stringResource(R.string.no_notes_crop), style = MaterialTheme.typography.bodySmall) }
            items(notes, key = { "n-" + it.id }) { n ->
                ElevatedSection(Modifier.clickable { editNote = n }) {
                    Text(n.title.ifBlank { stringResource(R.string.untitled) }, style = MaterialTheme.typography.titleSmall)
                    Text(n.body, style = MaterialTheme.typography.bodyMedium)
                    Text(java.time.Instant.ofEpochMilli(n.updatedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Spacer(Modifier.width(1.dp)) }
        }
    }
    if (confirmDelete && c != null) ConfirmDialog(stringResource(R.string.delete_crop), stringResource(R.string.delete_crop_text), stringResource(R.string.delete),
        onConfirm = { scope.launch { users.deleteCrop(c); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { nav.back() } } }, onDismiss = { confirmDelete = false })
    if ((newNote || editNote != null) && c != null) NoteEditorDialog(
        initial = editNote, onDismiss = { newNote = false; editNote = null },
        onSave = { title, body -> scope.launch { users.saveNote(editNote, title, body, c.plantId, c.id) }; newNote = false; editNote = null },
        onDelete = editNote?.let { n -> { scope.launch { users.deleteNote(n) }; editNote = null } },
    )
}
