package org.agripedia.app.ui.encyclopedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.agripedia.app.R
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.components.ChipFlow
import org.agripedia.app.ui.components.EmptyState
import org.agripedia.app.ui.components.PlantRow
import org.agripedia.app.ui.components.StatusBadge
import org.agripedia.app.ui.contentLang
import org.agripedia.app.ui.label
import org.agripedia.app.ui.monthName
import org.agripedia.app.ui.navigation.Nav
import org.agripedia.app.ui.tr
import org.agripedia.core.model.Difficulty
import org.agripedia.core.model.WaterNeed
import org.agripedia.core.search.CycleLength

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaScreen(k: Knowledge, appVm: AppViewModel, nav: Nav, initialQuery: String, initialCategory: String, initialAdapted: Boolean) {
    val vm: EncyclopediaViewModel = viewModel(key = "enc-$initialQuery-$initialCategory-$initialAdapted") {
        EncyclopediaViewModel(k, appVm.container, initialQuery, initialCategory, initialAdapted)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val favorites by appVm.favorites.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val lang = contentLang()
    val c = k.catalog

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ui.query, onValueChange = vm::setQuery, modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (ui.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Clear, stringResource(R.string.clear)) } },
            )
            IconButton(onClick = { showFilters = true }) {
                BadgedBox(badge = { if (ui.activeFilters > 0) Badge { Text(ui.activeFilters.toString()) } }) {
                    Icon(Icons.Filled.FilterList, stringResource(R.string.filters))
                }
            }
        }
        Text(
            pluralCount(results.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        if (results.isEmpty()) {
            EmptyState(stringResource(R.string.no_results_title), stringResource(R.string.no_results_text), icon = Icons.Outlined.SearchOff,
                action = { if (ui.activeFilters > 0) TextButton(onClick = vm::clearFilters) { Text(stringResource(R.string.clear_filters)) } })
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.plant.id }) { hit ->
                    val p = hit.plant
                    val name = p.displayName(lang)
                    val matched = hit.matchedName?.takeIf { it != name && it != p.scientificName }
                    PlantRow(
                        p, name,
                        subtitle = listOfNotNull(tr(c.categoriesById[p.categoryId]?.names), matched?.let { "« $it »" }).joinToString(" · "),
                        onClick = { nav.plant(p.id) },
                        badges = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(p.status)
                                if (p.id in favorites) Text("★", color = MaterialTheme.colorScheme.tertiary)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showFilters) ModalBottomSheet(onDismissRequest = { showFilters = false }) {
        Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::clearFilters) { Text(stringResource(R.string.clear_filters)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.filter_adapted), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        ui.regionId?.let { c.regionsById[it] }?.let { tr(it.names) }?.let { stringResource(R.string.filter_adapted_detail, it, monthName(ui.month)) } ?: stringResource(R.string.filter_adapted_no_region),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ui.adapted, onCheckedChange = vm::setAdapted, enabled = ui.regionId != null)
            }
            FilterGroup(stringResource(R.string.category)) {
                c.reference.categories.sortedBy { it.order }.forEach { cat ->
                    FilterChip(cat.id in ui.filter.categoryIds, { vm.toggleCategory(cat.id) }, { Text(tr(cat.names)) })
                }
            }
            FilterGroup(stringResource(R.string.difficulty)) {
                Difficulty.entries.forEach { d -> FilterChip(d in ui.filter.difficulties, { vm.toggleDifficulty(d) }, { Text(label(d)) }) }
            }
            FilterGroup(stringResource(R.string.water_needs)) {
                WaterNeed.entries.forEach { w -> FilterChip(w in ui.filter.waterNeeds, { vm.toggleWater(w) }, { Text(label(w)) }) }
            }
            FilterGroup(stringResource(R.string.cultivation_method)) {
                c.reference.methods.forEach { m -> FilterChip(m.id in ui.filter.methodIds, { vm.toggleMethod(m.id) }, { Text(tr(m.names)) }) }
            }
            FilterGroup(stringResource(R.string.soil)) {
                c.reference.soilTypes.forEach { s -> FilterChip(s.id in ui.filter.soilIds, { vm.toggleSoil(s.id) }, { Text(tr(s.names)) }) }
            }
            FilterGroup(stringResource(R.string.climate)) {
                c.reference.climateZones.forEach { z -> FilterChip(z.id in ui.filter.climateZoneIds, { vm.toggleZone(z.id) }, { Text(tr(z.names)) }) }
            }
            FilterGroup(stringResource(R.string.cycle_length)) {
                CycleLength.entries.forEach { cl ->
                    FilterChip(ui.filter.cycle == cl, { vm.setCycle(if (ui.filter.cycle == cl) null else cl) }, {
                        Text(stringResource(when (cl) { CycleLength.SHORT -> R.string.cycle_short; CycleLength.MEDIUM -> R.string.cycle_medium; CycleLength.LONG -> R.string.cycle_long }))
                    })
                }
            }
            FilterGroup(stringResource(R.string.country)) {
                c.geography.countries.forEach { co -> FilterChip(ui.filter.countryId == co.id, { vm.setCountry(if (ui.filter.countryId == co.id) null else co.id) }, { Text(tr(co.names)) }) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun pluralCount(n: Int): String = androidx.compose.ui.res.pluralStringResource(R.plurals.results_count, n, n)

@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
    ChipFlow(content)
}
