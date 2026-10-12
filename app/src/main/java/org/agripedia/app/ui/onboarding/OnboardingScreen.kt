package org.agripedia.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.agripedia.app.R
import org.agripedia.app.data.repo.Knowledge
import org.agripedia.app.data.repo.UserProfile
import org.agripedia.app.ui.AppLanguage
import org.agripedia.app.ui.AppViewModel
import org.agripedia.app.ui.components.ChipFlow
import org.agripedia.app.ui.components.DemoBanner
import org.agripedia.app.ui.components.RegionPickerDialog
import org.agripedia.app.ui.label
import org.agripedia.app.ui.tr
import org.agripedia.core.engine.ExperienceLevel
import org.agripedia.core.engine.FarmType

/** First launch: language, region (optional), experience and type of growing space. No account required. */
@Composable
fun OnboardingScreen(k: Knowledge, vm: AppViewModel, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var regionId by rememberSaveable { mutableStateOf<String?>(null) }
    var experience by rememberSaveable { mutableStateOf(ExperienceLevel.BEGINNER) }
    var farm by rememberSaveable { mutableStateOf(FarmType.GARDEN) }
    var showPicker by remember { mutableStateOf(false) }
    val steps = 4

    fun finish() = scope.launch {
        val region = regionId?.let { k.catalog.regionsById[it] }
        vm.container.userRepository.saveProfile(UserProfile(region?.countryId, region?.id, experience, farm, farm.defaultMethod))
        vm.container.settings.setOnboardingDone(true)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { onDone() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        LinearProgressIndicator(progress = { (step + 1) / steps.toFloat() }, modifier = Modifier.fillMaxWidth())
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                0 -> {
                    Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                    Text(stringResource(R.string.onb_welcome_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.onb_welcome_text), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                    ChipFlow {
                        AppLanguage.OPTIONS.forEach { (tag, name) ->
                            FilterChip(selected = AppLanguage.current() == tag, onClick = { AppLanguage.set(tag) },
                                label = { Text(name ?: stringResource(R.string.language_system)) })
                        }
                    }
                    DemoBanner(tr(k.catalog.manifest.disclaimer))
                }
                1 -> {
                    Icon(Icons.Outlined.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.onb_region_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.onb_region_text), style = MaterialTheme.typography.bodyLarge)
                    val region = regionId?.let { k.catalog.regionsById[it] }
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(region?.let { "${tr(k.catalog.countriesById[it.countryId]?.names)} · ${tr(it.names)}" } ?: stringResource(R.string.choose_region))
                    }
                    Text(stringResource(R.string.onb_region_skip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                2 -> {
                    Text(stringResource(R.string.onb_experience_title), style = MaterialTheme.typography.headlineSmall)
                    ExperienceLevel.entries.forEach { e ->
                        FilterChip(selected = experience == e, onClick = { experience = e }, label = { Text(label(e)) }, modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> {
                    Text(stringResource(R.string.onb_farm_title), style = MaterialTheme.typography.headlineSmall)
                    FarmType.entries.forEach { f ->
                        FilterChip(selected = farm == f, onClick = { farm = f }, label = { Text(label(f)) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.previous)) }
            Spacer(Modifier.weight(1f))
            if (step < steps - 1) {
                TextButton(onClick = { finish() }) { Text(stringResource(R.string.skip)) }
                Button(onClick = { step++ }) { Text(stringResource(R.string.next)) }
            } else Button(onClick = { finish() }) { Text(stringResource(R.string.start)) }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (showPicker) RegionPickerDialog(k.catalog, regionId, onDismiss = { showPicker = false }, onPick = { regionId = it.id })
}
