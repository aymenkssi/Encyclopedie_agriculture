package org.agripedia.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Keeps the Google Play listing and the website consistent with the app. */
class PlayStoreTest {
    private val root = File("..")

    @Test
    fun storeTextsRespectGooglePlayLimits() {
        val p = ProcessBuilder("python3", "play-store/scripts/check_texts.py").directory(root).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        assertEquals(out, 0, p.waitFor())
    }

    @Test
    fun appAdsTxtMatchesPublisherAccount() {
        assertEquals("google.com, pub-7488746561313974, DIRECT, f08c47fec0942fa0", File(root, "site/app-ads.txt").readText().trim())
    }

    @Test
    fun privacyPolicyIsPublishedInThreeLanguages() {
        val html = File(root, "site/privacy.html").readText()
        assertTrue(html.contains("<h2>English</h2>") && html.contains("<h2>Français</h2>") && html.contains("العربية"))
        assertTrue(html.contains(Links.PACKAGE))
    }
}
