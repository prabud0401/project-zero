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
    fun appManifestMustRemainHomeOnlyWithoutForbiddenCapabilities() {
        val manifest = File(System.getProperty("appManifest")).readText()
        assertTrue(manifest.contains("android.intent.category.HOME"))
        assertTrue(manifest.contains("android.intent.category.DEFAULT"))
        val forbidden = listOf(
            "BIND_ACCESSIBILITY_SERVICE",
            "SYSTEM_ALERT_WINDOW",
            "QUERY_ALL_PACKAGES",
            "BIND_NOTIFICATION_LISTENER_SERVICE",
            "AccessibilityService",
            "NotificationListenerService",
            "CALL_PHONE",
            "SEND_SMS",
            "READ_CONTACTS",
            "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE",
        )
        forbidden.forEach { token ->
            assertFalse(manifest.contains(token), "forbidden token $token in source manifest")
        }
    }
}
