package org.agripedia.app.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.agripedia.app.R
import org.agripedia.app.data.db.AgriculturalTaskEntity
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.components.ConfirmDialog
import org.agripedia.app.ui.components.DateField
import org.agripedia.app.ui.components.Dropdown
import org.agripedia.app.ui.components.ElevatedSection
import org.agripedia.app.ui.components.EmptyState
import org.agripedia.app.ui.components.InfoNote
import org.agripedia.app.ui.components.ScreenScaffold
import org.agripedia.app.ui.components.SectionHeader
import org.agripedia.app.ui.contentLang
import org.agripedia.app.ui.label
import org.agripedia.app.ui.monthName
import org.agripedia.app.ui.navigation.Nav
import org.agripedia.app.ui.tr
import org.agripedia.core.model.Activity
import java.time.LocalDate
import java.time.YearMonth

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(k: Knowledge, appVm: AppViewModel, nav: Nav) {
    val users = appVm.container.userRepository
    val scope = rememberCoroutineScope()
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<AgriculturalTaskEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    val ym = YearMonth.now().plusMonths(monthOffset.toLong())
    val tasks by remember(ym) { users.tasksBetween(ym.atDay(1), ym.atEndOfMonth()) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val overdue by remember { users.openTasksUntil(LocalDate.now().minusDays(1)) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val profile by appVm.profile.collectAsStateWithLifecycle()
    val favorites by appVm.favorites.collectAsStateWithLifecycle()
    val crops by users.crops.collectAsStateWithLifecycle(initialValue = emptyList())
    val lang = contentLang()
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    ScreenScaffold(
        title = stringResource(R.string.tab_calendar),
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, stringResource(R.string.new_task)) } },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthOffset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous)) }
                Text("${monthName(ym.monthValue)} ${ym.year}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).clickable { monthOffset = 0 })
                IconButton(onClick = { monthOffset++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next)) }
            }
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text(stringResource(R.string.my_tasks)) })
                Tab(tab == 1, { tab = 1 }, text = { Text(stringResource(R.string.crop_calendar)) })
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (tab == 0) {
                    if (monthOffset == 0 && overdue.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.overdue_tasks)) }
                        items(overdue, key = { "o-" + it.id }) { t -> TaskRow(t, onToggle = { scope.launch { users.setTaskDone(t, it) } }, onClick = { editing = t }) }
                        item { SectionHeader(stringResource(R.string.tasks_of_month)) }
                    }
                    if (tasks.isEmpty()) item {
                        EmptyState(stringResource(R.string.no_tasks_title), stringResource(R.string.no_tasks_text), icon = Icons.Outlined.TaskAlt)
                    }
                    items(tasks, key = { it.id }) { t -> TaskRow(t, onToggle = { scope.launch { users.setTaskDone(t, it) } }, onClick = { editing = t }) }
                } else {
                    val region = profile?.regionId?.let { k.catalog.regionsById[it] }
                    if (region == null) {
                        item { EmptyState(stringResource(R.string.no_region_title), stringResource(R.string.calendar_needs_region)) }
                    } else {
                        val ids = (crops.map { it.plantId } + favorites).distinct()
                        item { InfoNote(stringResource(R.string.crop_calendar_hint, tr(region.names))) }
                        if (ids.isEmpty()) item { EmptyState(stringResource(R.string.no_followed_plants_title), stringResource(R.string.no_followed_plants_text)) }
                        val rows = ids.mapNotNull { id ->
                            val p = k.catalog.plantsById[id] ?: return@mapNotNull null
                            val method = crops.firstOrNull { it.plantId == id }?.methodId ?: p.methods.first()
                            p to k.calendar.compute(p, region, method).activitiesIn(ym.monthValue)
                        }
                        items(rows, key = { "r-" + it.first.id }) { (p, acts) ->
                            ElevatedSection(Modifier.clickable { nav.plant(p.id) }) {
                                Text(p.displayName(lang), style = MaterialTheme.typography.titleSmall)
                                Text(if (acts.isEmpty()) stringResource(R.string.nothing_this_month) else acts.map { label(it) }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
    if (creating || editing != null) TaskEditorDialog(
        k, initial = editing, defaultDate = if (monthOffset == 0) LocalDate.now() else ym.atDay(1),
        onDismiss = { creating = false; editing = null },
        onSave = { title, desc, activity, date, plantId, reminder, repeat ->
            scope.launch { users.saveTask(editing, title, desc, activity, date, plantId, null, reminder, repeat) }
            if (reminder && org.agripedia.app.notifications.Reminders.needsPermission(context)) {
                notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            creating = false; editing = null
        },
        onDelete = editing?.let { t -> { scope.launch { users.deleteTask(t) }; editing = null } },
    )
}

@Composable
private fun TaskRow(t: AgriculturalTaskEntity, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val due = LocalDate.ofEpochDay(t.dueDate)
    val late = !t.done && due.isBefore(LocalDate.now())
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(t.done, onToggle)
        Column(Modifier.weight(1f)) {
            Text(t.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (t.done) TextDecoration.LineThrough else null)
            Text(
                listOfNotNull(due.toString(), runCatching { Activity.valueOf(t.activity) }.getOrNull()?.let { label(it) }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (t.reminder) Icon(Icons.Outlined.Notifications, stringResource(R.string.reminder), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (t.repeatDays != null) Icon(Icons.Outlined.Repeat, stringResource(R.string.repeat), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val REPEAT_OPTIONS = listOf<Int?>(null, 1, 2, 3, 7, 14, 30)

@Composable
fun TaskEditorDialog(
    k: Knowledge, initial: AgriculturalTaskEntity?, defaultDate: LocalDate, onDismiss: () -> Unit,
    onSave: (String, String?, Activity, LocalDate, String?, Boolean, Int?) -> Unit, onDelete: (() -> Unit)?,
) {
    val lang = contentLang()
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var desc by remember { mutableStateOf(initial?.description.orEmpty()) }
    var activity by remember { mutableStateOf(initial?.activity?.let { runCatching { Activity.valueOf(it) }.getOrNull() } ?: Activity.WATERING) }
    var date by remember { mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.dueDate) } ?: defaultDate) }
    var plantId by remember { mutableStateOf(initial?.plantId) }
    var reminder by remember { mutableStateOf(initial?.reminder ?: true) }
    var repeat by remember { mutableStateOf(initial?.repeatDays) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_task else R.string.edit_task)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.title)) }, singleLine = true,
                    isError = title.isBlank(), modifier = Modifier.fillMaxWidth())
                Dropdown(stringResource(R.string.activity), Activity.entries.filter { it != Activity.FLOWERING }, activity, { label(it) }, { activity = it })
                DateField(stringResource(R.string.due_date), date, { date = it })
                Dropdown(stringResource(R.string.plant_optional), listOf<String?>(null) + k.catalog.plants.sortedBy { it.displayName(lang) }.map { it.id }, plantId,
                    { id -> id?.let { k.catalog.plantsById[it]?.displayName(lang) } ?: stringResource(R.string.none) }, { plantId = it })
                OutlinedTextField(desc, { desc = it.take(500) }, label = { Text(stringResource(R.string.description)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.reminder), modifier = Modifier.weight(1f))
                    Switch(reminder, { reminder = it })
                }
                Dropdown(stringResource(R.string.repeat), REPEAT_OPTIONS, repeat,
                    { it?.let { d -> androidx.compose.ui.res.pluralStringResource(R.plurals.every_n_days, d, d) } ?: stringResource(R.string.no_repeat) }, { repeat = it })
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, desc, activity, date, plantId, reminder, repeat) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (confirmDelete && onDelete != null) ConfirmDialog(stringResource(R.string.delete_task), stringResource(R.string.delete_task_text), stringResource(R.string.delete),
        onConfirm = onDelete, onDismiss = { confirmDelete = false })
}
