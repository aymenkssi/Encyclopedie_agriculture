package org.agripedia.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** End-to-end smoke test: onboarding, main tabs, search and plant sheet, on a real device/emulator. */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun s(id: Int) = rule.activity.getString(id)

    private fun waitFor(text: String, timeoutMs: Long = 20_000) =
        rule.waitUntil(timeoutMs) { rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun launchNavigateSearchAndOpenSheet() {
        // First launch shows onboarding (or home if already done on this device).
        rule.waitUntil(30_000) {
            rule.onAllNodesWithText(s(R.string.skip)).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText(s(R.string.tab_home)).fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodesWithText(s(R.string.skip)).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText(s(R.string.skip)).performClick()
        }
        waitFor(s(R.string.tab_encyclopedia))

        // Every tab opens.
        rule.onNodeWithText(s(R.string.tab_calendar)).performClick()
        waitFor(s(R.string.my_tasks))
        rule.onNodeWithText(s(R.string.tab_crops)).performClick()
        waitFor(s(R.string.my_gardens))
        rule.onNodeWithText(s(R.string.tab_profile)).performClick()
        waitFor(s(R.string.my_profile))

        // Search (typo-tolerant) and open the sheet.
        rule.onNodeWithText(s(R.string.tab_encyclopedia)).performClick()
        rule.waitUntil(10_000) { rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("tomtae")
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Solanum lycopersicum")).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasText("Solanum lycopersicum")).onFirst().performClick()
        waitFor(s(R.string.common_names))
        rule.onNodeWithText(s(R.string.tab_guide)).performClick()
        waitFor("1")
        rule.onNodeWithText(s(R.string.tab_health)).performClick()
        waitFor(s(R.string.diseases))
        rule.onAllNodesWithText(s(R.string.diseases)).onFirst().assertIsDisplayed()
    }
}
