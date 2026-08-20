package com.jonecx.ibex

import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferences
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// Guards that keystore-wrapped data stays excluded from Auto Backup; restoring stale ciphertext
// after the non-exportable master key is gone would break every saved connection.
class BackupRulesConfigTest {

    private val keysetPref = "ibex_smb_keyset_prefs.xml"
    private val connectionsFile = "datastore/${NetworkConnectionsPreferences.STORE_NAME}.preferences_pb"

    @Test
    fun backupRules_excludesKeystoreWrappedData_api30() {
        val excludes = excludesIn("backup_rules.xml")
        assertTrue("Tink keyset must be excluded", excludes.contains("sharedpref" to keysetPref))
        assertTrue("connections store must be excluded", excludes.contains("file" to connectionsFile))
    }

    @Test
    fun dataExtractionRules_excludeKeystoreWrappedData_everySection() {
        val doc = parse("data_extraction_rules.xml")
        for (section in listOf("cloud-backup", "device-transfer")) {
            val excludes = excludesUnder(doc.getElementsByTagName(section).item(0) as Element)
            assertTrue("$section: keyset excluded", excludes.contains("sharedpref" to keysetPref))
            assertTrue("$section: connections store excluded", excludes.contains("file" to connectionsFile))
        }
    }

    private fun excludesIn(fileName: String): Set<Pair<String, String>> {
        val root = parse(fileName).documentElement
        return excludesUnder(root)
    }

    private fun excludesUnder(scope: Element): Set<Pair<String, String>> {
        val nodes = scope.getElementsByTagName("exclude")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .map { it.getAttribute("domain") to it.getAttribute("path") }
            .toSet()
    }

    private fun parse(fileName: String) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resolve(fileName))

    // Unit tests run with the module dir as CWD; fall back to the repo root for IDE runs.
    private fun resolve(fileName: String): File {
        val relative = "src/main/res/xml/$fileName"
        return File(relative).takeIf { it.exists() } ?: File("app/$relative")
    }
}
