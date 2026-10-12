package org.agripedia.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Checks that French (default), English and Arabic resources stay in sync. */
class ResourcesTest {
    private fun keys(folder: String): Set<String> {
        val f = File("src/main/res/$folder/strings.xml")
        assertTrue("missing ${f.path}", f.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)
        val names = mutableSetOf<String>()
        for (tag in listOf("string", "plurals")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) names += tag + ":" + nodes.item(i).attributes.getNamedItem("name").nodeValue
        }
        return names
    }

    @Test
    fun allLanguagesHaveTheSameKeys() {
        val fr = keys("values")
        assertEquals(fr, keys("values-en"))
        assertEquals(fr, keys("values-ar"))
    }

    @Test
    fun placeholdersMatchAcrossLanguages() {
        fun placeholders(folder: String): Map<String, List<String>> {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$folder/strings.xml"))
            val nodes = doc.getElementsByTagName("string")
            return (0 until nodes.length).associate { i ->
                val n = nodes.item(i)
                n.attributes.getNamedItem("name").nodeValue to Regex("%\\d\\$[sd]").findAll(n.textContent).map { it.value }.sorted().toList()
            }
        }
        val fr = placeholders("values")
        for (lang in listOf("values-en", "values-ar")) {
            val other = placeholders(lang)
            fr.forEach { (k, v) -> assertEquals("$lang/$k", v, other[k]) }
        }
    }
}
