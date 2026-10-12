package org.agripedia.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.agripedia.app.ads.Ads
import org.agripedia.app.data.prefs.ThemeMode
import org.agripedia.app.data.repo.UserProfile
import org.agripedia.core.engine.ExperienceLevel
import org.agripedia.core.engine.FarmType
import org.agripedia.core.model.Methods
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Captures the Google Play screenshots (see .github/workflows/store-screenshots.yml).
 * Skipped unless run with `-e storeScreenshots true -e lang fr|en|ar`.
 * Images are written to /sdcard/Android/data/<package>/files/store/<lang>/.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshots {
    @get:Rule val rule = createEmptyComposeRule()

    private val args = InstrumentationRegistry.getArguments()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun shell(cmd: String) = instrumentation.uiAutomation.executeShellCommand(cmd).close()

    private fun waitTag(tag: String) = rule.waitUntil(30_000) { rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(text: String) = rule.waitUntil(30_000) { rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun capture(dir: File, name: String) {
        rule.waitForIdle()
        Thread.sleep(800) // let animations and ripples settle
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun capture() {
        assumeTrue(args.getString("storeScreenshots") == "true")
        val lang = args.getString("lang") ?: "fr"
        val (country, region) = when (lang) { "ar" -> "MA" to "ma-rsk"; "en" -> "FR" to "fr-paca"; else -> "FR" to "fr-idf" }
        val query = when (lang) { "ar" -> "طماطم"; "en" -> "tomatoe"; else -> "tomtae" }

        val app = instrumentation.targetContext.applicationContext as AgriPediaApp
        val c = app.container
        Ads.disabledForScreenshots = true
        shell("cmd locale set-app-locales ${app.packageName} --user 0 --locales $lang")
        instrumentation.runOnMainSync {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(lang))
        }
        runBlocking {
            c.settings.setOnboardingDone(true)
            c.settings.setTheme(ThemeMode.LIGHT)
            c.userRepository.saveProfile(UserProfile(country, region, ExperienceLevel.BEGINNER, FarmType.GARDEN, Methods.OPEN_FIELD))
            val favs = c.userRepository.favoritePlantIds.first()
            for (id in listOf("solanum-lycopersicum", "ocimum-basilicum", "lactuca-sativa")) if (id !in favs) c.userRepository.toggleFavorite(id)
        }
        val dir = File(app.getExternalFilesDir(null), "store/$lang").apply { deleteRecursively(); mkdirs() }

        ActivityScenario.launch(MainActivity::class.java).use {
            waitTag("tab-home")
            // Make sure the requested language is really applied before capturing. On a freshly
            // booted device the first launch may keep the system language: recreate the activity then.
            fun languageApplied(): Boolean {
                var current = ""
                it.onActivity { a -> current = a.resources.configuration.locales[0].language }
                return current == lang
            }
            repeat(3) { attempt ->
                if (!languageApplied()) {
                    if (attempt > 0) it.recreate()
                    runCatching { rule.waitUntil(10_000) { languageApplied() } }
                }
            }
            check(languageApplied()) { "App language is not '$lang'" }
            waitTag("tab-home")
            capture(dir, "01-home")

            rule.onAllNodes(hasTestTag("tab-encyclopedia")).onFirst().performClick()
            rule.waitUntil(15_000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput(query)
            waitText("Solanum lycopersicum")
            capture(dir, "02-search")

            rule.onAllNodes(hasText("Solanum lycopersicum")).onFirst().performClick()
            waitTag("plant-tab-overview")
            capture(dir, "03-sheet")

            rule.onAllNodes(hasTestTag("plant-tab-calendar")).onFirst().performClick()
            rule.waitForIdle()
            capture(dir, "04-calendar")

            runBlocking { c.settings.setTheme(ThemeMode.DARK) }
            rule.onAllNodes(hasTestTag("tab-home")).onFirst().performClick()
            waitTag("tab-home")
            capture(dir, "05-dark")
        }
        runBlocking { c.settings.setTheme(ThemeMode.SYSTEM) }
    }
}
