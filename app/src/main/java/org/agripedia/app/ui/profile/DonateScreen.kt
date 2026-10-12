package org.agripedia.app.ui.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.agripedia.app.Links
import org.agripedia.app.R
import org.agripedia.app.ui.components.ElevatedSection
import org.agripedia.app.ui.components.ScreenScaffold
import org.agripedia.app.ui.navigation.Nav

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) { /* no browser */ }
}

/** Donation page (PayPal) and other ways to support the app. Deliberately ad-free. */
@Composable
fun DonateScreen(nav: Nav) {
    val context = LocalContext.current
    val shareText = stringResource(R.string.share_app_message, Links.PLAY_STORE_URL)
    val shareTitle = stringResource(R.string.share_app)
    ScreenScaffold(stringResource(R.string.support_title), onBack = nav::back) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.donate_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.donate_message), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                ElevatedSection {
                    Reason(Icons.Outlined.CardGiftcard, stringResource(R.string.donate_reason_free))
                    Reason(Icons.Outlined.Eco, stringResource(R.string.donate_reason_data))
                    Reason(Icons.Outlined.Language, stringResource(R.string.donate_reason_languages))
                }
            }
            item {
                Button(onClick = { openUrl(context, Links.DONATION_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Favorite, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.donate_paypal))
                }
                Text(stringResource(R.string.donate_paypal_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            item { Text(stringResource(R.string.other_ways), style = MaterialTheme.typography.titleMedium) }
            item {
                OutlinedButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Links.PLAY_STORE_MARKET)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: ActivityNotFoundException) { openUrl(context, Links.PLAY_STORE_URL) }
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.StarRate, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.rate_app)) }
            }
            item {
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                    context.startActivity(Intent.createChooser(send, shareTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(8.dp)); Text(shareTitle) }
            }
            item {
                OutlinedButton(onClick = {
                    openUrl(context, "mailto:${Links.CONTACT_EMAIL}?subject=" + Uri.encode("AgriPedia"))
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Email, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.contribute_data)) }
                Text(stringResource(R.string.contribute_data_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Reason(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
