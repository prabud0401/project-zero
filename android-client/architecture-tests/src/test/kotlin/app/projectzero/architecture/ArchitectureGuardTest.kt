package app.projectzero.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ArchitectureGuardTest {
    private val productionClasses = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("app.projectzero")

    @Test
    fun domainMustNotDependOnAndroid() {
        noClasses()
            .that().resideInAPackage("app.projectzero.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "android..",
                "androidx..",
                "com.google.android..",
                "dalvik..",
            )
            .check(productionClasses)
    }

    @Test
    fun domainMustNotDependOnContracts() {
        noClasses()
            .that().resideInAPackage("app.projectzero.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("app.projectzero.contracts..")
            .check(productionClasses)
    }

    @Test
    fun projectPackagesMustBeFreeOfCycles() {
        slices()
            .matching("app.projectzero.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }

    @Test
    fun domainSourcesMustNotImportAndroid() {
        val root = File(System.getProperty("domainMainSource"))
        assertTrue(root.isDirectory, "domain sources missing at $root")
        val hits = root.walkTopDown()
            .filter { it.extension == "kt" || it.extension == "java" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import android") ||
                        trimmed.startsWith("import androidx") ||
                        trimmed.startsWith("import com.google.android")
                    ) {
                        "${file.relativeTo(root)}:${index + 1}: $trimmed"
                    } else {
                        null
                    }
                }
            }
            .toList()
        assertTrue(hits.isEmpty(), hits.joinToString("\n"))
    }

    @Test
    fun appManifestMustRemainHomeWithoutForbiddenCapabilities() {
        val manifest = File(System.getProperty("appManifest")).readText()
        assertTrue(manifest.contains("android.intent.category.HOME"))
        assertTrue(manifest.contains("android.intent.category.DEFAULT"))
        val forbidden = listOf(
            "BIND_ACCESSIBILITY_SERVICE",
            "SYSTEM_ALERT_WINDOW",
            "QUERY_ALL_PACKAGES",
            "AccessibilityService",
            "CALL_PHONE",
            "SEND_SMS",
            "READ_CONTACTS",
            "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE",
            "BIND_NOTIFICATION_LISTENER_SERVICE",
        )
        forbidden.forEach { token ->
            assertFalse(manifest.contains(token), "forbidden token $token in app source manifest")
        }
        assertFalse(manifest.contains("<uses-permission"), "app source manifest must not declare uses-permission")
    }

    @Test
    fun notificationListenerIsServicePermissionNotUsesPermission() {
        val ingest = File(System.getProperty("androidClientRoot"), "notification-ingest/src/main/AndroidManifest.xml")
        assertTrue(ingest.isFile, "notification-ingest manifest missing")
        val text = ingest.readText()
        assertTrue(text.contains("android.service.notification.NotificationListenerService"))
        assertTrue(text.contains("android:permission=\"android.permission.BIND_NOTIFICATION_LISTENER_SERVICE\""))
        assertFalse(text.contains("<uses-permission"), "listener permission must not be a uses-permission")
        assertFalse(text.contains("BIND_ACCESSIBILITY_SERVICE"))
        assertFalse(text.contains("SYSTEM_ALERT_WINDOW"))
        assertFalse(text.contains("QUERY_ALL_PACKAGES"))
        assertFalse(text.contains("AccessibilityService"))
    }

    @Test
    fun allModuleManifestsForbidAccessibilityOverlayAndBroadQueries() {
        val root = File(System.getProperty("androidClientRoot"))
        val manifests = root.walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && !it.path.contains("${File.separator}build${File.separator}") }
            .toList()
        assertTrue(manifests.isNotEmpty())
        val forbidden = listOf(
            "BIND_ACCESSIBILITY_SERVICE",
            "SYSTEM_ALERT_WINDOW",
            "QUERY_ALL_PACKAGES",
            "AccessibilityService",
        )
        manifests.forEach { file ->
            val text = file.readText()
            forbidden.forEach { token ->
                assertFalse(text.contains(token), "forbidden $token in ${file.relativeTo(root)}")
            }
            assertFalse(
                text.contains("<uses-permission") && text.contains("BIND_NOTIFICATION_LISTENER_SERVICE"),
                "uses-permission BIND_NOTIFICATION_LISTENER_SERVICE in ${file.relativeTo(root)}",
            )
            assertFalse(text.contains("ACTION_CALL"), "ACTION_CALL in ${file.relativeTo(root)}")
            assertFalse(text.contains("CALL_PHONE"), "CALL_PHONE in ${file.relativeTo(root)}")
        }
    }

    @Test
    fun sourcesMustNotUseActionCallOrHiddenApis() {
        val root = File(System.getProperty("androidClientRoot"))
        val hits = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.extension == "kt" && !it.path.contains("${File.separator}build${File.separator}") }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//")) return@forEachIndexed
                    if (trimmed.contains("Intent.ACTION_CALL") || trimmed.contains("\"android.intent.action.CALL\"")) {
                        if (!trimmed.contains("forbidden") && !trimmed.contains("assert") &&
                            !trimmed.contains("require(") && !trimmed.contains("!=") &&
                            !trimmed.contains("==") && !trimmed.contains("trimmed.contains")
                        ) {
                            hits += "${file.relativeTo(root)}:${index + 1}: $trimmed"
                        }
                    }
                    if ((trimmed.contains("@hide") || trimmed.contains("dalvik.system.VMRuntime")) && !trimmed.contains("trimmed.contains")) {
                        hits += "${file.relativeTo(root)}:${index + 1}: $trimmed"
                    }
                }
            }
        assertTrue(hits.isEmpty(), hits.joinToString("\n"))
    }

    @Test
    fun ingestSourcesMustNotLogNotificationBodies() {
        val roots = listOf("notification-ingest", "local-ai", "data-local", "app").map {
            File(System.getProperty("androidClientRoot"), "$it/src/main")
        }
        val hits = mutableListOf<String>()
        roots.filter { it.isDirectory }.forEach { root ->
            root.walkTopDown()
                .filter { it.extension == "kt" }
                .forEach { file ->
                    file.readLines().forEachIndexed { index, line ->
                        val trimmed = line.trim()
                        if ((trimmed.contains("Log.d") || trimmed.contains("Log.i") || trimmed.contains("Log.w") ||
                                trimmed.contains("Log.e") || trimmed.contains("println(")) &&
                            (trimmed.contains(".body") || trimmed.contains(".title") ||
                                trimmed.contains("EXTRA_TEXT") || trimmed.contains("EXTRA_TITLE") ||
                                trimmed.contains("redactedBody") || trimmed.contains("notification.extras"))
                        ) {
                            hits += "${file.relativeTo(root)}:${index + 1}: $trimmed"
                        }
                    }
                }
        }
        assertTrue(hits.isEmpty(), hits.joinToString("\n"))
    }
}
