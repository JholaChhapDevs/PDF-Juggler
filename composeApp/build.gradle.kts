import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.bundles.basics)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            // MaryTTS dependencies
            implementation("de.dfki.mary:marytts:5.2.1")
            implementation("de.dfki.mary:voice-cmu-slt-hsmm:5.2.1")

            // Missing annotation dependencies needed by ProGuard
            implementation("javax.annotation:javax.annotation-api:1.3.2")
            implementation("com.google.code.findbugs:jsr305:3.0.2")

            // Optional dependencies for better compatibility
            implementation("org.apache.commons:commons-lang3:3.12.0")
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.jholachhapdevs.pdfjuggler.MainKt"

        buildTypes {
            release {
                proguard {
                    isEnabled.set(false)
                }
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PDF-Juggler"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icons/app_icon.ico"))
                menuGroup = "PDF Juggler"
                shortcut = true
                menu = true
            }

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icons/app_icon.icns"))
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icons/app_icon.png"))
            }

            // Include additional JVM modules
            modules("java.sql", "java.management", "java.logging", "java.desktop")
        }
    }
}
