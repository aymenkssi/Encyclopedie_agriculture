package org.agripedia.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import org.agripedia.app.backup.BackupManager
import org.agripedia.app.data.db.AppDatabase
import org.agripedia.app.data.prefs.SettingsStore
import org.agripedia.app.data.repo.CatalogRepository
import org.agripedia.app.data.repo.UserRepository
import org.agripedia.app.data.seed.AssetSeedReader
import org.agripedia.app.data.seed.ContentImporter

/**
 * Manual dependency injection (keeps the build simple; can be replaced by Hilt).
 * Also the place where future features (sync backend, premium gate) are plugged in.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    val settings = SettingsStore(appContext)
    val catalogRepository by lazy { CatalogRepository(ContentImporter(database, AssetSeedReader(appContext.assets)), appScope) }
    val userRepository by lazy { UserRepository(database, settings) }
    val backupManager by lazy { BackupManager(database, appContext.contentResolver) }
    val features = FeatureGate()
}

/**
 * Feature flags for a future free / premium / professional split. Everything is
 * free and enabled in this version; no payment system is implemented.
 */
class FeatureGate {
    enum class Feature { ENCYCLOPEDIA, CALENDAR, GARDENS, ADVANCED_PLANNING, EXPORTS, MULTI_DEVICE_SYNC }
    fun isEnabled(feature: Feature): Boolean = feature != Feature.MULTI_DEVICE_SYNC // no backend yet
}
