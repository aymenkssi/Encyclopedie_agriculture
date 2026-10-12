package org.agripedia.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.agripedia.app.data.db.AppDatabase
import org.agripedia.app.data.prefs.SettingsStore
import org.agripedia.app.data.repo.UserRepository
import org.agripedia.app.data.seed.AssetSeedReader
import org.agripedia.app.data.seed.ContentImporter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    @Test fun importsBundledContentAndRebuildsCatalog() = runBlocking {
        val importer = ContentImporter(db, AssetSeedReader(context.assets))
        val version = importer.ensureImported()
        val catalog = importer.loadFromDatabase()
        assertTrue(version > 0)
        assertTrue(catalog.plants.isNotEmpty())
        assertTrue(catalog.regionsById.isNotEmpty())
        // Second call is a no-op (same version).
        assertEquals(version, importer.ensureImported())
    }

    @Test fun favoritesAndTasksArePersisted() = runBlocking {
        val repo = UserRepository(db, SettingsStore(context))
        repo.toggleFavorite("solanum-lycopersicum")
        assertEquals(listOf("solanum-lycopersicum"), repo.favoritePlantIds.first())
        repo.toggleFavorite("solanum-lycopersicum")
        assertTrue(repo.favoritePlantIds.first().isEmpty())

        val today = LocalDate.now()
        repo.saveTask(null, "Arroser", null, org.agripedia.core.model.Activity.WATERING, today, null, null, true, 3)
        val task = repo.tasksBetween(today, today).first().single()
        repo.setTaskDone(task, true)
        val all = repo.tasksBetween(today, today.plusDays(10)).first()
        assertEquals(2, all.size) // done task + next occurrence
        assertTrue(all.any { !it.done && it.dueDate == today.plusDays(3).toEpochDay() })
    }
}
