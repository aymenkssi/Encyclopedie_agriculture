package org.agripedia.app.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.agripedia.app.R
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.categoryIcon
import org.agripedia.app.ui.components.DemoBanner
import org.agripedia.app.ui.components.ElevatedSection
import org.agripedia.app.ui.components.EmptyState
import org.agripedia.app.ui.components.OfflineBanner
import org.agripedia.app.ui.components.Pill
import org.agripedia.app.ui.components.PlantRow
import org.agripedia.app.ui.components.PlantTile
import org.agripedia.app.ui.components.RegionPickerDialog
import org.agripedia.app.ui.components.SectionHeader
import org.agripedia.app.ui.contentLang
import org.agripedia.app.ui.label
import org.agripedia.app.ui.monthList
import org.agripedia.app.ui.monthName
import org.agripedia.app.ui.navigation.Nav
import org.agripedia.app.ui.suitabilityColors
import org.agripedia.app.ui.tr
import java.time.LocalDate

@Composable
fun HomeScreen(k: Knowledge, appVm: AppViewModel, nav: Nav) {
    val vm: HomeViewModel = viewModel { HomeViewModel(k, appVm.container) }
    val state by vm.state.collectAsStateWithLifecycle()
    val online by appVm.online.collectAsStateWithLifecycle()
    val lang = contentLang()
    var showPicker by remember { mutableStateOf(false) }
    val c = k.catalog
    val region = state.region

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!online) item { OfflineBanner() }
        item {
            // Global search entry point.
            Surface(
                shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().clickable { nav.encyclopedia() },
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            // Location and period selectors.
            ElevatedSection {
                Row(Modifier.fillMaxWidth().clickable { showPicker = true }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(region?.let { tr(c.countriesById[it.countryId]?.names) } ?: stringResource(R.string.no_region),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(region?.let { tr(it.names) } ?: stringResource(R.string.choose_region), style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { showPicker = true }) { Text(stringResource(R.string.change)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shiftMonth(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous)) }
                    Text(
                        state.period.season?.let { label(it) + " · " + monthList(state.period.months) } ?: monthName(state.period.month),
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 2,
                    )
                    IconButton(onClick = { vm.shiftMonth(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next)) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = state.period.season == null && state.period.month == LocalDate.now().monthValue,
                            onClick = { vm.selectMonth(LocalDate.now().monthValue) }, label = { Text(stringResource(R.string.this_month)) })
                    }
                    items(state.seasons) { s ->
                        FilterChip(selected = state.period.season == s, onClick = { vm.selectSeason(s) }, label = { Text(label(s)) })
                    }
                }
            }
        }
        item { DemoBanner(tr(c.manifest.disclaimer), onClick = { nav.sources() }) }
        item {
            SectionHeader(stringResource(R.string.categories))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(c.reference.categories.sortedBy { it.order }, key = { it.id }) { cat ->
                    AssistChip(onClick = { nav.encyclopedia(category = cat.id) }, label = { Text(tr(cat.names)) },
                        leadingIcon = { Icon(categoryIcon(cat.id), null) })
                }
            }
        }

        if (region == null) {
            item {
                EmptyState(stringResource(R.string.no_region_title), stringResource(R.string.no_region_text),
                    action = { Button(onClick = { showPicker = true }) { Text(stringResource(R.string.choose_region)) } })
            }
        } else {
            item {
                SectionHeader(stringResource(R.string.recommended_for, tr(region.names))) {
                    TextButton(onClick = { nav.encyclopedia(adapted = true) }) { Text(stringResource(R.string.see_all)) }
                }
                Text(stringResource(R.string.recommended_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.recommendations.isEmpty()) item {
                EmptyState(stringResource(R.string.no_reco_title), stringResource(R.string.no_reco_text))
            }
            items(state.recommendations, key = { "reco-" + it.plant.id }) { r ->
                val (bg, fg) = suitabilityColors(r.level)
                PlantRow(
                    r.plant, r.plant.displayName(lang),
                    subtitle = r.activities.filter { it in org.agripedia.core.engine.PlantCalendar.START_ACTIVITIES }
                        .ifEmpty { r.activities }.take(3).map { label(it) }.joinToString(" · ").ifEmpty { null },
                    onClick = { nav.plant(r.plant.id) },
                    badges = { Pill(label(r.level), bg, fg) },
                )
            }
        }

        item {
            SectionHeader(stringResource(R.string.tasks_now)) {
                TextButton(onClick = { nav.calendar() }) { Text(stringResource(R.string.open_calendar)) }
            }
        }
        if (state.tasks.isEmpty() && state.cropActivities.isEmpty()) item {
            Text(stringResource(R.string.no_tasks_now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(state.tasks, key = { "task-" + it.id }) { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = t.done, onCheckedChange = { vm.setDone(t, it) })
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val due = LocalDate.ofEpochDay(t.dueDate)
                    Text(
                        if (due.isBefore(LocalDate.now())) stringResource(R.string.overdue_since, due.toString()) else due.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (due.isBefore(LocalDate.now())) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.cropActivities.isNotEmpty()) {
            item { Text(stringResource(R.string.month_calendar_for_you, monthName(state.period.month)), style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)) }
            items(state.cropActivities, key = { "act-" + it.first.id }) { (plant, acts) ->
                Row(Modifier.fillMaxWidth().clickable { nav.plant(plant.id) }.padding(vertical = 6.dp)) {
                    Text(plant.displayName(lang), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(0.4f))
                    Text(acts.joinToString(" · ") { label(it) }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
                }
            }
        }

        if (state.tips.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.seasonal_tips)) }
            items(state.tips) { tip ->
                ElevatedSection {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(tipText(tip), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (state.favorites.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.favorites))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.favorites, key = { "fav-" + it.id }) { p ->
                        PlantTile(p, p.displayName(lang), tr(c.categoriesById[p.categoryId]?.names), onClick = { nav.plant(p.id) })
                    }
                }
            }
        }
        if (state.recent.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.recently_viewed)) {
                    TextButton(onClick = { nav.history() }) { Text(stringResource(R.string.see_all)) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recent, key = { "recent-" + it.id }) { p ->
                        PlantTile(p, p.displayName(lang), p.scientificName, onClick = { nav.plant(p.id) })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showPicker) RegionPickerDialog(c, region?.id, onDismiss = { showPicker = false }, onPick = { vm.setRegion(it) })
}

@Composable
private fun tipText(tip: Tip): String = when (tip) {
    is Tip.Frost -> stringResource(R.string.tip_frost, monthList(tip.months))
    is Tip.Dry -> stringResource(R.string.tip_dry, monthList(tip.months))
    is Tip.Heat -> stringResource(R.string.tip_heat, monthList(tip.months))
    is Tip.RainyStart -> stringResource(R.string.tip_rainy_start, monthList(tip.months))
    is Tip.Wet -> stringResource(R.string.tip_wet, monthList(tip.months))
}
