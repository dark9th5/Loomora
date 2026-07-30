package com.loomora

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalizationResourcesTest {
    private val root: File = File(System.getProperty("user.dir")).let {
        if (it.name == "app") it.parentFile else it
    }

    @Test
    fun englishAndVietnameseResourceKeysMatch() {
        resourcePairs().forEach { (english, vietnamese) ->
            assertEquals(
                "Resource key mismatch: ${english.parentFile.parentFile}",
                resourceKeys(english),
                resourceKeys(vietnamese)
            )
        }
    }

    @Test
    fun vietnameseResourcesDoNotContainKnownUnaccentedCopy() {
        val denied = listOf(
            "Yeu thich", "Thung rac", "Khoi phuc", "Xoa vinh vien",
            "Ban ghi moi", "San sang", "Dang chuan bi"
        )
        resourcePairs().forEach { (_, vietnamese) ->
            val text = vietnamese.readText()
            denied.forEach { phrase ->
                assertFalse("Unaccented Vietnamese '$phrase' in $vietnamese", text.contains(phrase))
            }
        }
    }

    private fun resourcePairs(): List<Pair<File, File>> = root.walkTopDown()
        .filter { it.isFile && it.name == "strings.xml" && it.path.contains("src${File.separator}main${File.separator}res${File.separator}values${File.separator}") }
        .mapNotNull { english ->
            val vietnamese = File(english.parentFile.parentFile, "values-vi/strings.xml")
            if (vietnamese.isFile) english to vietnamese else null
        }
        .toList()

    private fun resourceKeys(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val resources = document.documentElement.childNodes
        return buildSet {
            for (index in 0 until resources.length) {
                val node = resources.item(index)
                val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                add("${node.nodeName}:$name")
            }
        }
    }
}
