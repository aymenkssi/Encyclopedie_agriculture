package org.agripedia.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.agripedia.app.data.prefs.ThemeMode
import org.agripedia.app.ui.navigation.AgriApp
import org.agripedia.app.ui.theme.AgriTheme

/** AppCompatActivity so that the per-app language (AppCompatDelegate) applies on all API levels. */
class MainActivity : AppCompatActivity() {
    private var openCalendar by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openCalendar = intent?.getBooleanExtra(EXTRA_OPEN_CALENDAR, false) == true
        val container = (application as AgriPediaApp).container
        // Google consent form (GDPR) then AdMob initialization; the app works without it.
        org.agripedia.app.ads.Ads.gatherConsent(this)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            AgriTheme(settings?.theme ?: ThemeMode.SYSTEM) {
                AgriApp(settings = settings, openCalendar = openCalendar, onCalendarOpened = { openCalendar = false })
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_CALENDAR, false)) openCalendar = true
    }

    companion object { const val EXTRA_OPEN_CALENDAR = "open_calendar" }
}
