package org.agripedia.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Per-app language (persisted by AppCompat). Empty tag = follow the system. */
object AppLanguage {
    val OPTIONS = listOf("" to null, "fr" to "Français", "ar" to "العربية", "en" to "English")

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }

    fun current(): String = AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()
}
