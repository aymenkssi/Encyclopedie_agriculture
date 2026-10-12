package org.agripedia.app.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.agripedia.app.BuildConfig
import org.agripedia.app.Links
import org.agripedia.app.ads.Ads
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Tune
import org.agripedia.app.R
import org.agripedia.app.data.db.UserNoteEntity
import org.agripedia.app.data.prefs.Settings
import org.agripedia.app.data.prefs.ThemeMode
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.notifications.Reminders
import org.agripedia.app.ui.AppLanguage
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.appContainer
import org.agripedia.app.ui.components.ChipFlow
import org.agripedia.app.ui.components.ConfirmDialog
import org.agripedia.app.ui.components.DemoBanner
import org.agripedia.app.ui.components.Dropdown
import org.agripedia.app.ui.components.ElevatedSection
import org.agripedia.app.ui.components.EmptyState
import org.agripedia.app.ui.components.InfoNote
import org.agripedia.app.ui.components.InfoRow
import org.agripedia.app.ui.components.PlantRow
import org.agripedia.app.ui.components.RegionPickerDialog
import org.agripedia.app.ui.components.ScreenScaffold
import org.agripedia.app.ui.components.SectionHeader
import org.agripedia.app.ui.contentLang
import org.agripedia.app.ui.label
import org.agripedia.app.ui.navigation.Nav
import org.agripedia.app.ui.plant.SourceCard
import org.agripedia.app.ui.tr
import org.agripedia.core.engine.ExperienceLevel
import org.agripedia.core.engine.FarmType

@Composable
fun ProfileScreen(k: Knowledge, appVm: AppViewModel, nav: Nav, settings: Settings) {
    val container = appVm.container
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profile by appVm.profile.collectAsStateWithLifecycle()
    val adPrivacyRequired by Ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showPicker by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val p = profile ?: return
    val c = k.catalog
    val exported = stringResource(R.string.backup_exported)
    val failed = stringResource(R.string.backup_failed)
    val restoredFmt = stringResource(R.string.backup_restored)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { container.backupManager.export(uri) }.isSuccess
            snackbar.showSnackbar(if (ok) exported else failed)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> confirmImport = uri }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch { container.settings.setNotifications(granted) }
        if (granted) Reminders.schedule(context)
    }

    ScreenScaffold(title = stringResource(R.string.tab_profile), snackbarHostState = snackbar) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SectionHeader(stringResource(R.string.my_profile))
                InfoNote(stringResource(R.string.no_account_needed))
                ElevatedSection {
                    val region = p.regionId?.let { c.regionsById[it] }
                    Row(Modifier.fillMaxWidth().clickable { showPicker = true }, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.reference_region), style = MaterialTheme.typography.labelSmall)
                            Text(region?.let { "${tr(c.countriesById[it.countryId]?.names)} · ${tr(it.names)}" } ?: stringResource(R.string.choose_region),
                                style = MaterialTheme.typography.bodyLarge)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Dropdown(stringResource(R.string.experience), ExperienceLevel.entries, p.experience, { label(it) }, { appVm.saveProfile(p.copy(experience = it)) })
                    Dropdown(stringResource(R.string.farm_type), FarmType.entries, p.farmType, { label(it) },
                        { appVm.saveProfile(p.copy(farmType = it, preferredMethod = it.defaultMethod)) })
                    Dropdown(stringResource(R.string.preferred_method), c.reference.methods.map { it.id }, p.preferredMethod, { tr(c.methodsById[it]?.names) },
                        { appVm.saveProfile(p.copy(preferredMethod = it)) })
                }
            }
            item {
                SectionHeader(stringResource(R.string.settings))
                ElevatedSection {
                    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
                    ChipFlow {
                        AppLanguage.OPTIONS.forEach { (tag, name) ->
                            FilterChip(AppLanguage.current() == tag, { AppLanguage.set(tag) }, { Text(name ?: stringResource(R.string.language_system)) })
                        }
                    }
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
                    ChipFlow {
                        ThemeMode.entries.forEach { m ->
                            FilterChip(settings.theme == m, { scope.launch { container.settings.setTheme(m) } }, {
                                Text(stringResource(when (m) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }))
                            })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.notifications), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.notifications_hint), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(settings.notificationsEnabled, { on ->
                            if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canPost(context)) {
                                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                scope.launch { container.settings.setNotifications(on) }
                                if (on) Reminders.schedule(context) else Reminders.cancel(context)
                            }
                        })
                    }
                }
            }
            item {
                SectionHeader(stringResource(R.string.my_space))
                LinkRow(Icons.AutoMirrored.Outlined.StickyNote2, stringResource(R.string.my_notes)) { nav.notes() }
                LinkRow(Icons.Outlined.History, stringResource(R.string.history)) { nav.history() }
                LinkRow(Icons.AutoMirrored.Outlined.LibraryBooks, stringResource(R.string.sources_and_data)) { nav.sources() }
                LinkRow(Icons.Outlined.FavoriteBorder, stringResource(R.string.support_title)) { nav.donate() }
            }
            item {
                SectionHeader(stringResource(R.string.backup))
                ElevatedSection {
                    Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { exportLauncher.launch("agripedia-backup-${java.time.LocalDate.now()}.json") }) { Text(stringResource(R.string.export)) }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text(stringResource(R.string.import_)) }
                    }
                }
            }
            item {
                SectionHeader(stringResource(R.string.privacy))
                InfoNote(stringResource(R.string.privacy_text))
                LinkRow(Icons.Outlined.Policy, stringResource(R.string.privacy_policy)) { openUrl(context, Links.PRIVACY_POLICY_URL) }
                if (adPrivacyRequired) LinkRow(Icons.Outlined.Tune, stringResource(R.string.ad_privacy_options)) {
                    context.findActivity()?.let { Ads.showPrivacyOptions(it) }
                }
                InfoRow(stringResource(R.string.app_version), BuildConfig.VERSION_NAME)
                InfoRow(stringResource(R.string.dataset_version), c.manifest.version.toString())
            }
        }
    }
    if (showPicker) RegionPickerDialog(c, p.regionId, onDismiss = { showPicker = false }, onPick = { appVm.saveProfile(p.copy(countryId = it.countryId, regionId = it.id)) })
    confirmImport?.let { uri ->
        ConfirmDialog(stringResource(R.string.import_), stringResource(R.string.import_confirm), stringResource(R.string.import_), onConfirm = {
            scope.launch {
                val r = runCatching { container.backupManager.import(uri) }
                snackbar.showSnackbar(r.fold({ restoredFmt.format(it.applied) }, { failed }))
            }
        }, onDismiss = { confirmImport = null })
    }
}

@Composable
private fun LinkRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) }, leadingContent = { Icon(icon, null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun NoteEditorDialog(initial: UserNoteEntity?, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onDelete: (() -> Unit)?) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var body by remember { mutableStateOf(initial?.body.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_note else R.string.edit_note)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.title)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it.take(5000) }, label = { Text(stringResource(R.string.note)) }, minLines = 4, modifier = Modifier.fillMaxWidth())
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() || body.isNotBlank(), onClick = { onSave(title, body) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (confirmDelete && onDelete != null) ConfirmDialog(stringResource(R.string.delete_note), stringResource(R.string.delete_note_text), stringResource(R.string.delete),
        onConfirm = onDelete, onDismiss = { confirmDelete = false })
}

@Composable
fun NotesScreen(k: Knowledge, nav: Nav) {
    val users = appContainer().userRepository
    val scope = rememberCoroutineScope()
    val notes by users.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    var editing by remember { mutableStateOf<UserNoteEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    val lang = contentLang()
    ScreenScaffold(stringResource(R.string.my_notes), onBack = nav::back, actions = {
        TextButton(onClick = { creating = true }) { Text(stringResource(R.string.new_note)) }
    }) { padding ->
        if (notes.isEmpty()) EmptyState(stringResource(R.string.no_notes_title), stringResource(R.string.no_notes_text), Modifier.padding(padding), icon = Icons.Outlined.Description)
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id }) { n ->
                ElevatedSection(Modifier.clickable { editing = n }) {
                    Text(n.title.ifBlank { stringResource(R.string.untitled) }, style = MaterialTheme.typography.titleSmall)
                    n.plantId?.let { k.catalog.plantsById[it] }?.let {
                        Text(it.displayName(lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { nav.plant(it.id) })
                    }
                    Text(n.body, style = MaterialTheme.typography.bodyMedium, maxLines = 5)
                }
            }
        }
    }
    if (creating || editing != null) NoteEditorDialog(editing, onDismiss = { creating = false; editing = null },
        onSave = { t, b -> scope.launch { users.saveNote(editing, t, b, editing?.plantId, editing?.cropId) }; creating = false; editing = null },
        onDelete = editing?.let { n -> { scope.launch { users.deleteNote(n) }; editing = null } })
}

@Composable
fun HistoryScreen(k: Knowledge, nav: Nav) {
    val users = appContainer().userRepository
    val scope = rememberCoroutineScope()
    val ids by remember { users.recentPlantIds(100) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val lang = contentLang()
    ScreenScaffold(stringResource(R.string.history), onBack = nav::back, actions = {
        if (ids.isNotEmpty()) IconButton(onClick = { scope.launch { users.clearHistory() } }) { Icon(Icons.Outlined.Delete, stringResource(R.string.clear_history)) }
    }) { padding ->
        val plants = ids.mapNotNull { k.catalog.plantsById[it] }
        if (plants.isEmpty()) EmptyState(stringResource(R.string.no_history_title), stringResource(R.string.no_history_text), Modifier.padding(padding))
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plants, key = { it.id }) { p -> PlantRow(p, p.displayName(lang), tr(k.catalog.categoriesById[p.categoryId]?.names), onClick = { nav.plant(p.id) }) }
        }
    }
}

@Composable
fun SourcesScreen(k: Knowledge, nav: Nav) {
    val c = k.catalog
    ScreenScaffold(stringResource(R.string.sources_and_data), onBack = nav::back) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { DemoBanner(tr(c.manifest.disclaimer)) }
            item {
                ElevatedSection {
                    InfoRow(stringResource(R.string.dataset_version), c.manifest.version.toString())
                    InfoRow(stringResource(R.string.generated_on), c.manifest.generatedOn)
                    InfoRow(stringResource(R.string.plants_count), c.plants.size.toString())
                    InfoRow(stringResource(R.string.regions_count), c.regionsById.size.toString())
                }
            }
            item {
                SectionHeader(stringResource(R.string.validation_levels))
                InfoNote(stringResource(R.string.status_verified_desc))
                InfoNote(stringResource(R.string.status_indicative_desc))
                InfoNote(stringResource(R.string.status_to_verify_desc))
            }
            item { SectionHeader(stringResource(R.string.sources)) }
            items(c.reference.sources, key = { it.id }) { s -> SourceCard(s) }
            item {
                SectionHeader(stringResource(R.string.how_data_is_used))
                InfoNote(stringResource(R.string.how_data_is_used_text))
            }
        }
    }
}

/** Finds the hosting Activity behind Compose's context wrappers. */
fun android.content.Context.findActivity(): android.app.Activity? {
    var c: android.content.Context? = this
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}
