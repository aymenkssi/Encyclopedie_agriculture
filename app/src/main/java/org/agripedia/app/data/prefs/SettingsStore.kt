package org.agripedia.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Small key/value preferences (no personal data). */
class SettingsStore(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            onboardingDone = p[Keys.ONBOARDING] ?: false,
        )
    }

    suspend fun setTheme(mode: ThemeMode) { context.dataStore.edit { it[Keys.THEME] = mode.name } }
    suspend fun setNotifications(enabled: Boolean) { context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled } }
    suspend fun setOnboardingDone(done: Boolean) { context.dataStore.edit { it[Keys.ONBOARDING] = done } }

    /** Random installation id used as sync tie-breaker. Not linked to the user's identity. */
    suspend fun deviceId(): String {
        context.dataStore.data.first()[Keys.DEVICE_ID]?.let { return it }
        val id = UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.DEVICE_ID] = id }
        return id
    }
}
