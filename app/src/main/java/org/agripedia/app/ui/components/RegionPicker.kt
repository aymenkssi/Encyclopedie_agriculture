package org.agripedia.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import org.agripedia.app.R
import org.agripedia.app.ui.tr
import org.agripedia.core.engine.RegionLocator
import org.agripedia.core.model.Catalog
import org.agripedia.core.model.Region

/**
 * Two-step country -> region picker. Geolocation is optional: coarse location is
 * requested only when the user taps "use my location", used once to find the
 * nearest region, and never stored nor sent anywhere.
 */
@Composable
fun RegionPickerDialog(catalog: Catalog, currentRegionId: String?, onDismiss: () -> Unit, onPick: (Region) -> Unit) {
    val context = LocalContext.current
    var countryId by remember { mutableStateOf(currentRegionId?.let { catalog.regionsById[it]?.countryId }) }
    var message by remember { mutableStateOf<String?>(null) }
    val notFound = stringResource(R.string.location_not_found)
    val unavailable = stringResource(R.string.location_unavailable)

    var locating by remember { mutableStateOf(false) }
    val searching = stringResource(R.string.location_searching)

    fun useLocation(loc: Location?) {
        locating = false
        if (loc == null) { message = unavailable; return }
        val nearest = RegionLocator.nearest(catalog, loc.latitude, loc.longitude)
        if (nearest == null) message = notFound else { onPick(nearest.first); onDismiss() }
    }

    fun locate() {
        lastKnownLocation(context)?.let { useLocation(it); return }
        // No cached fix: ask once for a fresh coarse location.
        locating = true
        message = searching
        if (!requestCurrentLocation(context) { useLocation(it) }) useLocation(null)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else message = unavailable
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (countryId != null) IconButton(onClick = { countryId = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Text(stringResource(if (countryId == null) R.string.pick_country else R.string.pick_region))
            }
        },
        text = {
            Column {
                if (countryId == null) {
                    OutlinedButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
                        else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.MyLocation, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.use_my_location))
                    }
                    Text(stringResource(R.string.location_privacy), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                    message?.let { Text(it, color = if (locating) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                }
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (countryId == null) {
                        items(catalog.geography.countries, key = { it.id }) { c ->
                            val name = tr(c.names)
                            Text(name, Modifier.fillMaxWidth().clickable { countryId = c.id }.padding(vertical = 14.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        items(catalog.countriesById[countryId]?.regions.orEmpty(), key = { it.id }) { r ->
                            Row(Modifier.fillMaxWidth().clickable { onPick(r); onDismiss() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = r.id == currentRegionId, onClick = { onPick(r); onDismiss() })
                                Column {
                                    Text(tr(r.names), style = MaterialTheme.typography.bodyLarge)
                                    Text(tr(catalog.climateZonesById[r.climateZoneId]?.names), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** One-shot coarse location request; returns false when no provider is enabled. */
@SuppressLint("MissingPermission")
private fun requestCurrentLocation(context: Context, onResult: (Location?) -> Unit): Boolean {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
    val lm = context.getSystemService(LocationManager::class.java) ?: return false
    val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return false
    LocationManagerCompat.getCurrentLocation(lm, provider, android.os.CancellationSignal(), ContextCompat.getMainExecutor(context)) { onResult(it) }
    return true
}

@SuppressLint("MissingPermission")
private fun lastKnownLocation(context: Context): Location? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    return lm.getProviders(true).mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
}
