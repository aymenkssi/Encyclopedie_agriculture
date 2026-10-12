package org.agripedia.app.ui.crops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.agripedia.app.AppContainer
import org.agripedia.app.R
import org.agripedia.app.data.db.GardenPlotEntity
import org.agripedia.app.data.db.UserGardenEntity
import org.agripedia.app.data.repo.CropStage
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.ui.components.DateField
import org.agripedia.app.ui.components.Dropdown
import org.agripedia.app.ui.components.InfoNote
import org.agripedia.app.ui.components.parsePositiveDecimal
import org.agripedia.app.ui.contentLang
import org.agripedia.app.ui.label
import org.agripedia.app.ui.tr
import org.agripedia.core.engine.FarmType
import org.agripedia.core.model.Activity
import org.agripedia.core.model.Plant
import org.agripedia.core.model.SowingMode
import java.time.LocalDate

/** Start activities allowed for a plant according to its sowing mode. */
fun startActivitiesFor(p: Plant): List<Activity> = when (p.sowingMode) {
    SowingMode.DIRECT -> listOf(Activity.DIRECT_SOWING)
    SowingMode.TRANSPLANT -> listOf(Activity.NURSERY_SOWING, Activity.TRANSPLANTING)
    SowingMode.BOTH -> listOf(Activity.NURSERY_SOWING, Activity.DIRECT_SOWING, Activity.TRANSPLANTING)
    SowingMode.VEGETATIVE -> listOf(Activity.PLANTING)
}

/**
 * Adds a plant to one of the user's gardens (a default garden is created if none
 * exists). Optional milestone tasks are derived from the date entered here.
 */
@Composable
fun AddCropDialog(
    k: Knowledge, container: AppContainer, fixedPlant: Plant? = null, fixedGardenId: String? = null,
    onDismiss: () -> Unit, onAdded: (String) -> Unit,
) {
    val users = container.userRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = contentLang()
    val gardens by users.gardens.collectAsStateWithLifecycle(initialValue = emptyList())
    val profile by users.profile.collectAsStateWithLifecycle(initialValue = null)
    var plant by remember { mutableStateOf(fixedPlant) }
    var gardenId by remember { mutableStateOf(fixedGardenId) }
    var plotId by remember { mutableStateOf<String?>(null) }
    var start by remember { mutableStateOf(fixedPlant?.let { startActivitiesFor(it).first() }) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var stage by remember { mutableStateOf(CropStage.PLANNED) }
    var quantity by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf(true) }
    var method by remember { mutableStateOf(fixedPlant?.methods?.first()) }
    var rotationWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gardens) { if (gardenId == null) gardenId = gardens.firstOrNull()?.id }
    val plots by remember(gardenId) { gardenId?.let { users.plots(it) } ?: flowOf(emptyList()) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val defaultGardenName = stringResource(R.string.default_garden_name)
    val rotationMsg = stringResource(R.string.rotation_warning)
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    // Rotation advice: warn when the same family was grown recently on the chosen plot.
    LaunchedEffect(plotId, plant) {
        rotationWarning = null
        val p = plant ?: return@LaunchedEffect
        val pid = plotId ?: return@LaunchedEffect
        val years = p.rotation?.minYearsBeforeReturn ?: 0
        val limit = LocalDate.now().minusYears(years.toLong().coerceAtLeast(1)).toEpochDay()
        val recentSameFamily = users.cropHistoryOfPlot(pid).filter { it.startDate >= limit }
            .mapNotNull { k.catalog.plantsById[it.plantId] }
            .filter { it.family == p.family || it.family in p.rotation?.avoidPredecessorFamilies.orEmpty() }
        if (recentSameFamily.isNotEmpty()) rotationWarning = rotationMsg.format(recentSameFamily.joinToString { it.displayName(lang) })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_crops)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fixedPlant == null) {
                    Dropdown(stringResource(R.string.plant), k.catalog.plants.sortedBy { it.displayName(lang) }, plant, { it.displayName(lang) }, {
                        plant = it; start = startActivitiesFor(it).first(); method = it.methods.first()
                    })
                }
                if (fixedGardenId == null) {
                    if (gardens.isEmpty()) InfoNote(stringResource(R.string.default_garden_will_be_created))
                    else Dropdown(stringResource(R.string.garden), gardens, gardens.firstOrNull { it.id == gardenId }, { it.name }, { gardenId = it.id; plotId = null })
                }
                if (plots.isNotEmpty()) {
                    Dropdown(stringResource(R.string.plot), listOf<GardenPlotEntity?>(null) + plots, plots.firstOrNull { it.id == plotId },
                        { it?.name ?: stringResource(R.string.no_plot) }, { plotId = it?.id })
                }
                rotationWarning?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                plant?.let { p ->
                    Dropdown(stringResource(R.string.cultivation_method), p.methods, method, { tr(k.catalog.methodsById[it]?.names) }, { method = it })
                    Dropdown(stringResource(R.string.start_activity), startActivitiesFor(p), start, { label(it) }, { start = it })
                }
                DateField(stringResource(R.string.start_date), date, { date = it })
                Dropdown(stringResource(R.string.stage), CropStage.entries, stage, { label(it) }, { stage = it })
                OutlinedTextField(quantity, { quantity = it.take(60) }, label = { Text(stringResource(R.string.quantity_optional)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(plan, { plan = it })
                    Text(stringResource(R.string.plan_tasks), style = MaterialTheme.typography.bodyMedium)
                }
                if (plan) InfoNote(stringResource(R.string.plan_tasks_hint))
            }
        },
        confirmButton = {
            TextButton(enabled = plant != null && start != null, onClick = {
                val p = plant ?: return@TextButton
                scope.launch {
                    val gid = gardenId ?: users.saveGarden(null, defaultGardenName, profile?.farmType ?: FarmType.GARDEN, null, profile?.regionId)
                    val id = users.addCrop(gid, plotId, p, null, method ?: p.methods.first(), start!!, date, stage, quantity, plan) { a ->
                        context.getString(R.string.task_title_auto, activityLabel(context, a), p.displayName(lang))
                    }
                    if (plan && org.agripedia.app.notifications.Reminders.needsPermission(context)) {
                        notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onAdded(id)
                }
            }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Non-composable activity label (for texts stored in tasks). */
fun activityLabel(context: android.content.Context, a: Activity): String = context.getString(
    when (a) {
        Activity.SOIL_PREPARATION -> R.string.act_soil_preparation
        Activity.NURSERY_SOWING -> R.string.act_nursery_sowing
        Activity.DIRECT_SOWING -> R.string.act_direct_sowing
        Activity.TRANSPLANTING -> R.string.act_transplanting
        Activity.PLANTING -> R.string.act_planting
        Activity.WATERING -> R.string.act_watering
        Activity.FERTILIZING -> R.string.act_fertilizing
        Activity.PRUNING -> R.string.act_pruning
        Activity.FLOWERING -> R.string.act_flowering
        Activity.HEALTH_MONITORING -> R.string.act_health_monitoring
        Activity.TREATMENT -> R.string.act_treatment
        Activity.HARVEST -> R.string.act_harvest
        Activity.ROTATION_PLANNING -> R.string.act_rotation_planning
    }
)

@Composable
fun GardenEditorDialog(k: Knowledge, initial: UserGardenEntity?, defaultRegionId: String?, onDismiss: () -> Unit, onSave: (String, FarmType, Double?, String?) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var type by remember { mutableStateOf(initial?.farmType?.let { runCatching { FarmType.valueOf(it) }.getOrNull() } ?: FarmType.GARDEN) }
    var area by remember { mutableStateOf(initial?.areaM2?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()) }
    val regionId = initial?.regionId ?: defaultRegionId
    val areaInvalid = area.isNotBlank() && parsePositiveDecimal(area) == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_garden else R.string.edit_garden)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank())
                Dropdown(stringResource(R.string.garden_type), FarmType.entries, type, { label(it) }, { type = it })
                OutlinedTextField(area, { area = it.take(12) }, label = { Text(stringResource(R.string.area_m2)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = areaInvalid, supportingText = { if (areaInvalid) Text(stringResource(R.string.invalid_number)) })
                regionId?.let { k.catalog.regionsById[it] }?.let { InfoNote(stringResource(R.string.garden_region, tr(it.names))) }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && !areaInvalid, onClick = { onSave(name, type, parsePositiveDecimal(area), regionId); onDismiss() }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun PlotEditorDialog(k: Knowledge, onDismiss: () -> Unit, onSave: (String, Double?, String?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var soil by remember { mutableStateOf<String?>(null) }
    var method by remember { mutableStateOf(org.agripedia.core.model.Methods.OPEN_FIELD) }
    val areaInvalid = area.isNotBlank() && parsePositiveDecimal(area) == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_plot)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(area, { area = it.take(12) }, label = { Text(stringResource(R.string.area_m2)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = areaInvalid, supportingText = { if (areaInvalid) Text(stringResource(R.string.invalid_number)) })
                Dropdown(stringResource(R.string.soil), listOf<String?>(null) + k.catalog.reference.soilTypes.map { it.id }, soil,
                    { id -> id?.let { tr(k.catalog.soilTypesById[it]?.names) } ?: stringResource(R.string.unknown) }, { soil = it })
                Dropdown(stringResource(R.string.cultivation_method), k.catalog.reference.methods.map { it.id }, method, { tr(k.catalog.methodsById[it]?.names) }, { method = it })
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && !areaInvalid, onClick = { onSave(name, parsePositiveDecimal(area), soil, method); onDismiss() }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
