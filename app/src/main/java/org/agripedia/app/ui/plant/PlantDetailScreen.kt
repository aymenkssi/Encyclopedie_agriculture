package org.agripedia.app.ui.plant

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.agripedia.app.R
import org.agripedia.app.data.db.UserNoteEntity
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.components.ChipFlow
import org.agripedia.app.ui.components.DemoBanner
import org.agripedia.app.ui.components.ElevatedSection
import org.agripedia.app.ui.components.EmptyState
import org.agripedia.app.ui.components.InfoNote
import org.agripedia.app.ui.components.InfoRow
import org.agripedia.app.ui.components.MonthAxis
import org.agripedia.app.ui.components.MonthStrip
import org.agripedia.app.ui.components.Pill
import org.agripedia.app.ui.components.PlantAvatar
import org.agripedia.app.ui.components.ScreenScaffold
import org.agripedia.app.ui.components.SectionHeader
import org.agripedia.app.ui.components.StatusBadge
import org.agripedia.app.ui.contentLang
import org.agripedia.app.ui.crops.AddCropDialog
import org.agripedia.app.ui.label
import org.agripedia.app.ui.monthList
import org.agripedia.app.ui.navigation.Nav
import org.agripedia.app.ui.noteText
import org.agripedia.app.ui.profile.NoteEditorDialog
import org.agripedia.app.ui.suitabilityColors
import org.agripedia.app.ui.tr
import org.agripedia.core.engine.CalendarOrigin
import org.agripedia.core.model.CompanionRelation
import org.agripedia.core.model.Lang
import org.agripedia.core.model.Plant
import org.agripedia.core.model.Range
import org.agripedia.core.model.TreatmentType

private enum class PlantTab(val label: Int) {
    OVERVIEW(R.string.tab_overview), CONDITIONS(R.string.tab_conditions), CALENDAR(R.string.tab_plant_calendar),
    GUIDE(R.string.tab_guide), HEALTH(R.string.tab_health), COMPANIONS(R.string.tab_companions), SOURCES(R.string.tab_sources),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(k: Knowledge, appVm: AppViewModel, nav: Nav, plantId: String) {
    val plant = k.catalog.plantsById[plantId]
    if (plant == null) {
        ScreenScaffold(stringResource(R.string.app_name), onBack = nav::back) { p ->
            EmptyState(stringResource(R.string.plant_not_found), stringResource(R.string.plant_not_found_text), Modifier.padding(p))
        }
        return
    }
    val vm: PlantDetailViewModel = viewModel(key = "plant-$plantId") { PlantDetailViewModel(k, appVm.container, plant) }
    val st by vm.state.collectAsStateWithLifecycle()
    val lang = contentLang()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var addCrop by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<UserNoteEntity?>(null) }
    var newNote by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = plant.displayName(lang), onBack = nav::back,
        actions = {
            IconButton(onClick = vm::toggleFavorite) {
                Icon(if (st.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    stringResource(if (st.favorite) R.string.remove_favorite else R.string.add_favorite))
            }
            IconButton(onClick = { newNote = true }) { Icon(Icons.AutoMirrored.Outlined.NoteAdd, stringResource(R.string.add_note)) }
            IconButton(onClick = { addCrop = true }) { Icon(Icons.Outlined.AddCircleOutline, stringResource(R.string.add_to_crops)) }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                PlantTab.entries.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t.label)) },
                    modifier = Modifier.testTag("plant-tab-${t.name.lowercase()}")) }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (PlantTab.entries[tab]) {
                    PlantTab.OVERVIEW -> overview(k, plant, st, lang, nav, onEditNote = { editNote = it })
                    PlantTab.CONDITIONS -> conditions(k, plant)
                    PlantTab.CALENDAR -> calendar(k, plant, st, vm)
                    PlantTab.GUIDE -> guide(plant, st.methodId)
                    PlantTab.HEALTH -> health(k, plant)
                    PlantTab.COMPANIONS -> companions(k, plant, lang, nav)
                    PlantTab.SOURCES -> sources(k, plant)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (addCrop) AddCropDialog(k, appVm.container, fixedPlant = plant, onDismiss = { addCrop = false }, onAdded = { addCrop = false })
    if (newNote || editNote != null) NoteEditorDialog(
        initial = editNote, onDismiss = { newNote = false; editNote = null },
        onSave = { title, body -> vm.saveNote(editNote, title, body); newNote = false; editNote = null },
        onDelete = editNote?.let { n -> { vm.deleteNote(n); editNote = null } },
    )
}

private fun LazyListScope.overview(k: Knowledge, plant: Plant, st: PlantDetailState, lang: String, nav: Nav, onEditNote: (UserNoteEntity) -> Unit) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlantAvatar(plant, 64)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(plant.displayName(lang), style = MaterialTheme.typography.headlineSmall)
                Text(listOfNotNull(plant.scientificName, plant.scientificAuthority).joinToString(" "), fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium)
                Text(plant.family, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    item {
        ChipFlow {
            StatusBadge(plant.status)
            Pill(tr(k.catalog.categoriesById[plant.categoryId]?.names), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            Pill(label(plant.difficulty), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            Pill(label(plant.lifecycle), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (plant.isDemo) item { DemoBanner(stringResource(R.string.demo_sheet)) }
    st.recommendation?.let { r ->
        item {
            ElevatedSection {
                val (bg, fg) = suitabilityColors(r.level)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.in_your_region, tr(st.region?.names)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Pill(label(r.level), bg, fg)
                }
                r.notes.filter { it.code.name != "INDICATIVE_ONLY" }.take(4).forEach { InfoNote(noteText(it)) }
                InfoNote(stringResource(R.string.no_guarantee))
            }
        }
    }
    item {
        SectionHeader(stringResource(R.string.common_names))
        Lang.SUPPORTED.forEach { l ->
            plant.names[l]?.takeIf { it.isNotEmpty() }?.let { InfoRow(l.uppercase(), it.joinToString(", ")) }
        }
        if (plant.synonyms.isNotEmpty()) InfoRow(stringResource(R.string.synonyms), plant.synonyms.joinToString(", "))
    }
    item {
        SectionHeader(stringResource(R.string.description))
        Text(tr(plant.description).ifEmpty { stringResource(R.string.no_data) }, style = MaterialTheme.typography.bodyLarge)
    }
    if (plant.varieties.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.varieties)) }
        items(plant.varieties, key = { "var-" + it.id }) { v ->
            ElevatedSection {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(v.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusBadge(v.status)
                }
                tr(v.description).takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                v.cycleDays?.let { Text(stringResource(R.string.cycle_days_value, fmt(it)), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
    item {
        SectionHeader(stringResource(R.string.yield))
        Text(tr(plant.yieldInfo).ifEmpty { stringResource(R.string.no_data) }, style = MaterialTheme.typography.bodyMedium)
        tr(plant.yieldFactors).takeIf { it.isNotEmpty() }?.let { InfoNote(it) }
        SectionHeader(stringResource(R.string.storage))
        Text(tr(plant.storage).ifEmpty { stringResource(R.string.no_data) }, style = MaterialTheme.typography.bodyMedium)
    }
    item { SectionHeader(stringResource(R.string.my_notes)) }
    if (st.notes.isEmpty()) item { Text(stringResource(R.string.no_notes_plant), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    items(st.notes, key = { "note-" + it.id }) { n ->
        ElevatedSection(Modifier.clickable { onEditNote(n) }) {
            Text(n.title.ifBlank { stringResource(R.string.untitled) }, style = MaterialTheme.typography.titleSmall)
            Text(n.body, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
        }
    }
}

private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", d)
private fun fmt(r: Range): String = if (r.min == r.max) fmt(r.min) else "${fmt(r.min)}–${fmt(r.max)}"

private fun LazyListScope.conditions(k: Knowledge, plant: Plant) {
    item {
        ElevatedSection {
            Text(stringResource(R.string.climate_needs), style = MaterialTheme.typography.titleSmall)
            val t = plant.temperature
            InfoRow(stringResource(R.string.temp_min), t.min?.let { "${fmt(it)} °C" })
            val optMin = t.optimalMin; val optMax = t.optimalMax; val germ = t.germinationMin; val hardy = t.hardinessMin
            InfoRow(stringResource(R.string.temp_optimal), if (optMin != null && optMax != null) "${fmt(optMin)}–${fmt(optMax)} °C" else null)
            InfoRow(stringResource(R.string.temp_max), t.max?.let { "${fmt(it)} °C" })
            if (germ != null) InfoRow(stringResource(R.string.temp_germination), "${fmt(germ)} °C")
            if (hardy != null) InfoRow(stringResource(R.string.temp_hardiness), "${fmt(hardy)} °C")
            InfoRow(stringResource(R.string.frost_tolerance), label(plant.frostTolerance))
            InfoRow(stringResource(R.string.sun_exposure), label(plant.sun))
            InfoRow(stringResource(R.string.water_needs), label(plant.water))
            InfoNote(stringResource(R.string.temp_explanation))
        }
    }
    item {
        ElevatedSection {
            Text(stringResource(R.string.soil), style = MaterialTheme.typography.titleSmall)
            InfoRow(stringResource(R.string.soil_types), plant.soilIds.mapNotNull { k.catalog.soilTypesById[it] }.map { tr(it.names) }.joinToString(", ").ifEmpty { null })
            InfoRow(stringResource(R.string.soil_ph), plant.ph?.let { fmt(it) })
        }
    }
    item {
        ElevatedSection {
            Text(stringResource(R.string.cultivation), style = MaterialTheme.typography.titleSmall)
            InfoRow(stringResource(R.string.cycle_length), plant.cycleDays?.let { stringResource(R.string.cycle_days_value, fmt(it)) })
            InfoRow(stringResource(R.string.sowing_mode), label(plant.sowingMode))
            InfoRow(stringResource(R.string.sowing_depth), plant.sowingDepthCm?.let { "${fmt(it)} cm" })
            InfoRow(stringResource(R.string.nursery_duration), plant.nurseryWeeks?.let { stringResource(R.string.weeks_value, fmt(it)) })
            InfoRow(stringResource(R.string.spacing_in_row), plant.spacing?.inRowCm?.let { "${fmt(it)} cm" })
            InfoRow(stringResource(R.string.spacing_between_rows), plant.spacing?.betweenRowsCm?.let { "${fmt(it)} cm" })
            InfoRow(stringResource(R.string.cultivation_method), plant.methods.mapNotNull { k.catalog.methodsById[it] }.map { tr(it.names) }.joinToString(", "))
            InfoRow(stringResource(R.string.climate), plant.climateZoneIds.mapNotNull { k.catalog.climateZonesById[it] }.map { tr(it.names) }.joinToString(", ").ifEmpty { null })
        }
    }
}

private fun LazyListScope.calendar(k: Knowledge, plant: Plant, st: PlantDetailState, vm: PlantDetailViewModel) {
    item {
        Text(stringResource(R.string.cultivation_method), style = MaterialTheme.typography.titleSmall)
        ChipFlow { plant.methods.forEach { m -> FilterChip(st.methodId == m, { vm.setMethod(m) }, { Text(tr(k.catalog.methodsById[m]?.names)) }) } }
        if (plant.varieties.isNotEmpty() && plant.calendar.any { it.varietyId != null }) {
            Text(stringResource(R.string.varieties), style = MaterialTheme.typography.titleSmall)
            ChipFlow {
                FilterChip(st.varietyId == null, { vm.setVariety(null) }, { Text(stringResource(R.string.all)) })
                plant.varieties.forEach { v -> FilterChip(st.varietyId == v.id, { vm.setVariety(v.id) }, { Text(v.name) }) }
            }
        }
    }
    val cal = st.calendar
    if (st.region == null || cal == null) {
        item { EmptyState(stringResource(R.string.no_region_title), stringResource(R.string.calendar_needs_region)) }
        return
    }
    item {
        Text(stringResource(R.string.calendar_for, tr(st.region.names)), style = MaterialTheme.typography.titleMedium)
        if (st.recommendation?.level == org.agripedia.core.engine.SuitabilityLevel.NOT_RECOMMENDED) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.not_adapted_warning), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (!cal.available) {
        item { EmptyState(stringResource(R.string.no_calendar_title), stringResource(R.string.no_calendar_text)) }
    } else {
        item { MonthAxis() }
        items(cal.windows, key = { "w-" + it.activity.name + it.origin.name }) { w ->
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label(w.activity), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(stringResource(originLabel(w.origin)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MonthStrip(w.months, w.preferredMonths, current = java.time.LocalDate.now().monthValue,
                    color = if (w.activity == org.agripedia.core.model.Activity.HARVEST) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                Text(monthList(w.months), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val assumptions = plant.calendar.filter { (it.regionId == st.region.id || it.climateZoneId == st.region.climateZoneId) && it.assumptions.isNotEmpty() }
        if (assumptions.isNotEmpty()) item {
            SectionHeader(stringResource(R.string.assumptions))
            assumptions.forEach { InfoNote("${label(it.activity)} : ${tr(it.assumptions)}") }
        }
    }
    item {
        SectionHeader(stringResource(R.string.calendar_notes))
        cal.notes.forEach { InfoNote(noteText(it)) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        InfoNote(stringResource(R.string.climate_data_status, label(st.region.climateStatus)))
    }
}

private fun originLabel(o: CalendarOrigin): Int = when (o) {
    CalendarOrigin.EXPLICIT_REGION -> R.string.origin_region
    CalendarOrigin.EXPLICIT_ZONE -> R.string.origin_zone
    CalendarOrigin.COMPUTED -> R.string.origin_computed
    CalendarOrigin.PHENOLOGY -> R.string.origin_phenology
    CalendarOrigin.CONTROLLED -> R.string.origin_controlled
}

private fun LazyListScope.guide(plant: Plant, methodId: String) {
    if (plant.guides.isEmpty()) {
        item { EmptyState(stringResource(R.string.no_guide_title), stringResource(R.string.no_guide_text)) }
        return
    }
    plant.guides.forEach { g ->
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(g.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusBadge(g.status)
            }
        }
        items(g.stages.filter { it.methods.isEmpty() || methodId in it.methods }, key = { "st-${g.id}-${it.order}" }) { s ->
            ElevatedSection {
                Row(verticalAlignment = Alignment.Top) {
                    Pill(s.order.toString(), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(tr(s.title), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(tr(s.body), style = MaterialTheme.typography.bodyMedium)
                        tr(s.tips).takeIf { it.isNotEmpty() }?.let {
                            Spacer(Modifier.height(6.dp)); Text(stringResource(R.string.tip_prefix, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        tr(s.mistakes).takeIf { it.isNotEmpty() }?.let {
                            Spacer(Modifier.height(6.dp)); Text(stringResource(R.string.mistake_prefix, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.health(k: Knowledge, plant: Plant) {
    val diseases = plant.diseaseIds.mapNotNull { k.catalog.diseasesById[it] }
    val pests = plant.pestIds.mapNotNull { k.catalog.pestsById[it] }
    item { InfoNote(stringResource(R.string.health_disclaimer)) }
    if (diseases.isEmpty() && pests.isEmpty()) item { EmptyState(stringResource(R.string.no_health_title), stringResource(R.string.no_health_text)) }
    if (diseases.isNotEmpty()) item { SectionHeader(stringResource(R.string.diseases)) }
    items(diseases, key = { "d-" + it.id }) { d ->
        ElevatedSection {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(d.names), style = MaterialTheme.typography.titleSmall)
                    d.causalAgent?.let { Text(it, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall) }
                }
                Pill(label(d.type), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Labeled(R.string.symptoms, tr(d.symptoms))
            Labeled(R.string.favorable_conditions, tr(d.favorableConditions))
            Labeled(R.string.prevention, tr(d.prevention))
            Treatments(k, d.treatmentIds)
        }
    }
    if (pests.isNotEmpty()) item { SectionHeader(stringResource(R.string.pests)) }
    items(pests, key = { "p-" + it.id }) { p ->
        ElevatedSection {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(p.names), style = MaterialTheme.typography.titleSmall)
                    p.scientificName?.let { Text(it, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall) }
                }
                Pill(label(p.type), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Labeled(R.string.description, tr(p.description))
            Labeled(R.string.damage, tr(p.damage))
            Labeled(R.string.prevention, tr(p.prevention))
            Treatments(k, p.treatmentIds)
        }
    }
}

@Composable
private fun Labeled(title: Int, text: String) {
    if (text.isEmpty()) return
    Spacer(Modifier.height(6.dp))
    Text(stringResource(title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Treatments(k: Knowledge, ids: List<String>) {
    val list = ids.mapNotNull { k.catalog.treatmentsById[it] }.sortedBy { it.type.ordinal }
    if (list.isEmpty()) return
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.control_methods), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    list.forEach { t ->
        Column(Modifier.padding(vertical = 4.dp)) {
            Text("• ${tr(t.names)} (${label(t.type)})", style = MaterialTheme.typography.bodyMedium)
            tr(t.description).takeIf { it.isNotEmpty() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (t.type == TreatmentType.CHEMICAL || t.caution.isNotEmpty()) tr(t.caution).takeIf { it.isNotEmpty() }?.let {
                Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun LazyListScope.companions(k: Knowledge, plant: Plant, lang: String, nav: Nav) {
    val good = plant.companions.filter { it.relation == CompanionRelation.GOOD }
    val bad = plant.companions.filter { it.relation == CompanionRelation.BAD }
    // Also show relations declared from the other side.
    val reverse = k.catalog.plants.flatMap { other -> other.companions.filter { it.otherPlantId == plant.id && plant.companions.none { c -> c.otherPlantId == other.id } }.map { other to it } }
    item { SectionHeader(stringResource(R.string.good_companions)) }
    if (good.isEmpty() && reverse.none { it.second.relation == CompanionRelation.GOOD }) item { Text(stringResource(R.string.no_data), fontStyle = FontStyle.Italic) }
    items(good, key = { "g-" + it.otherPlantId }) { c -> CompanionRow(k, c.otherPlantId, tr(c.reason), c.status, lang, nav) }
    items(reverse.filter { it.second.relation == CompanionRelation.GOOD }, key = { "rg-" + it.first.id }) { (o, c) -> CompanionRow(k, o.id, tr(c.reason), c.status, lang, nav) }
    item { SectionHeader(stringResource(R.string.bad_companions)) }
    if (bad.isEmpty() && reverse.none { it.second.relation == CompanionRelation.BAD }) item { Text(stringResource(R.string.no_data), fontStyle = FontStyle.Italic) }
    items(bad, key = { "b-" + it.otherPlantId }) { c -> CompanionRow(k, c.otherPlantId, tr(c.reason), c.status, lang, nav) }
    items(reverse.filter { it.second.relation == CompanionRelation.BAD }, key = { "rb-" + it.first.id }) { (o, c) -> CompanionRow(k, o.id, tr(c.reason), c.status, lang, nav) }
    plant.rotation?.let { r ->
        item {
            SectionHeader(stringResource(R.string.rotation))
            ElevatedSection {
                Row { Text(stringResource(R.string.rotation_group, r.group), modifier = Modifier.weight(1f)); StatusBadge(r.status) }
                InfoRow(stringResource(R.string.rotation_years), r.minYearsBeforeReturn?.toString())
                InfoRow(stringResource(R.string.rotation_good), r.goodPredecessorFamilies.joinToString(", ").ifEmpty { null })
                InfoRow(stringResource(R.string.rotation_avoid), r.avoidPredecessorFamilies.joinToString(", ").ifEmpty { null })
                tr(r.notes).takeIf { it.isNotEmpty() }?.let { InfoNote(it) }
            }
        }
    }
}

@Composable
private fun CompanionRow(k: Knowledge, otherId: String, reason: String, status: org.agripedia.core.model.ValidationStatus, lang: String, nav: Nav) {
    val other = k.catalog.plantsById[otherId]
    ElevatedSection(if (other != null) Modifier.clickable { nav.plant(otherId) } else Modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(other?.displayName(lang) ?: otherId, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            StatusBadge(status)
        }
        if (reason.isNotEmpty()) Text(reason, style = MaterialTheme.typography.bodySmall)
    }
}

private fun LazyListScope.sources(k: Knowledge, plant: Plant) {
    item {
        ElevatedSection {
            InfoRow(stringResource(R.string.validation_status), label(plant.status))
            InfoRow(stringResource(R.string.last_reviewed), plant.lastReviewed)
            InfoRow(stringResource(R.string.dataset_version), k.catalog.manifest.version.toString())
        }
    }
    items(plant.sourceIds.mapNotNull { k.catalog.sourcesById[it] }, key = { "src-" + it.id }) { s -> SourceCard(s) }
}

@Composable
fun SourceCard(s: org.agripedia.core.model.DataSource) {
    ElevatedSection {
        Text(s.title, style = MaterialTheme.typography.titleSmall)
        s.publisher?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        s.url?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        s.license?.let { Text(stringResource(R.string.license, it), style = MaterialTheme.typography.bodySmall) }
        Text(stringResource(R.string.accessed_on, s.accessedOn ?: stringResource(R.string.not_consulted)), style = MaterialTheme.typography.bodySmall)
        tr(s.notes).takeIf { it.isNotEmpty() }?.let { InfoNote(it) }
    }
}
