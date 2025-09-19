import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.20"
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.filekit.coil)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.desktop.currentOs)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation(libs.colorpicker.compose)
            implementation(libs.composable.table)
            implementation(libs.data.table.material3)
            implementation(libs.filekit.coil)
            implementation(libs.filekit.coil)
            implementation(libs.filekit.core)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs)
            implementation(libs.kgit)
            implementation(libs.kgit)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kscript.tools)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.semver)
        }
        desktopMain.dependencies {

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.desktop.currentOs)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.filekit.coil)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.kotlinx.coroutines.swing)

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.desktop.currentOs)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation(libs.colorpicker.compose)
            implementation(libs.composable.table)
            implementation(libs.data.table.material3)
            implementation(libs.filekit.coil)
            implementation(libs.filekit.coil)
            implementation(libs.filekit.core)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs)
            implementation(libs.kgit)
            implementation(libs.kgit)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kscript.tools)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.semver)
        }
    }
}

// Application/version info generation
val appVersion = "1.6.0"

val generatedVersionDir = layout.buildDirectory.dir("generated/version/kotlin")

tasks.register("generateVersionInfo") {
    val outputDir = generatedVersionDir.get().asFile
    outputs.dir(outputDir)
    doLast {
        val pkg = "org.example.project"
        val dir = File(outputDir, pkg.replace('.', '/'))
        dir.mkdirs()
        File(dir, "VersionInfo.kt").writeText(
            """
            package $pkg
            object VersionInfo {
                const val PACKAGE_VERSION: String = "$appVersion"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    sourceSets {
        val desktopMain by getting
        desktopMain.kotlin.srcDir(generatedVersionDir)
    }
}

tasks.named("compileKotlinDesktop").configure {
    dependsOn("generateVersionInfo")
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "simplemoni"
            packageVersion = appVersion
            linux {
                modules(
                    "jdk.security.auth",
                    "java.instrument",
                    "java.management",
                    "java.security.jgss",
                    "java.sql",
                    "jdk.security.auth",
                    "jdk.unsupported"
                )
            }
        }
        buildTypes.release.proguard {
            version.set("7.4.0")
            obfuscate.set(false)
            isEnabled = false
        }
    }
}
