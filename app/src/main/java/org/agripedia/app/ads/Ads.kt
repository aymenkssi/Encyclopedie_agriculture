package org.agripedia.app.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.agripedia.app.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google AdMob banners (same publisher account as the other apps: pub-7488746561313974).
 *
 * - The App ID and the banner unit ID are set in gradle.properties (`agripedia.admob.appId`,
 *   `agripedia.admob.bannerId`), see play-store/README.md. Until then, and always in debug builds,
 *   Google's official test ads are shown. Never click your own real ads.
 * - The Google consent form (GDPR: EEA, UK, Switzerland) is shown first; ads are only requested
 *   once Google says they can be.
 * - Ads need the network: offline, the banner simply stays hidden and the app works normally.
 */
object Ads {
    /** Set to false to turn every ad off without touching the screens. */
    const val ENABLED = true

    /** Turned off by the store-screenshot test so that no test banner appears in the captures. */
    @Volatile var disabledForScreenshots = false

    private val _canShowAds = MutableStateFlow(false)
    val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True when the "Ad privacy options" entry must be offered in the settings. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val initialized = AtomicBoolean(false)

    private fun consent(context: Context): ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    /** Called from the activity on start: asks for consent if needed, then initializes AdMob. */
    fun gatherConsent(activity: Activity) {
        if (!ENABLED || disabledForScreenshots) return
        val info = consent(activity)
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                onConsentKnown(activity, info)
            }
        }, { _ ->
            // Offline or consent service unreachable: use the last known consent, if any.
            onConsentKnown(activity, info)
        })
        // Consent obtained in a previous session: ads can be requested right away.
        if (info.canRequestAds()) onConsentKnown(activity, info)
    }

    private fun onConsentKnown(context: Context, info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!info.canRequestAds()) return
        if (initialized.compareAndSet(false, true)) {
            MobileAds.initialize(context.applicationContext) { _canShowAds.value = true }
        }
    }

    /** Opens Google's form to change the ad consent (Profile › Ad privacy options). */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onConsentKnown(activity, consent(activity)) }
    }
}

/** Anchored adaptive banner, meant to sit just above the bottom navigation bar. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val canShow by Ads.canShowAds.collectAsStateWithLifecycle()
    if (!Ads.ENABLED || Ads.disabledForScreenshots || !canShow) return
    var failed by remember { mutableStateOf(false) }
    if (failed) return
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
        val width = maxWidth.value.toInt()
        val adView = remember(width) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) { failed = true }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}
