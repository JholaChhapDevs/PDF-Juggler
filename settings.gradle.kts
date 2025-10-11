rootProject.name = "PDF-Juggler"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Plugin management repositories
pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Additional repositories for MaryTTS and old dependencies
        jcenter() // for fast-md5, Jampack
        maven("https://oss.sonatype.org/content/repositories/releases/")
        maven("https://marytts.github.io/marytts/maven/")
        maven("https://jitpack.io")
        maven("https://nrgxnat.jfrog.io/artifactory/libs-release/")
        maven("https://nexus.terrestris.de/repository/public/")
        // GitHub repo for jtok-core
        maven {
            url = uri("https://raw.githubusercontent.com/DFKI-MLT/Maven-Repository/main")
        }
    }
}

// Dependency resolution repositories for all projects
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // disallow subproject repositories
    repositories {
        // Modern libraries
        google()
        mavenCentral()

        // Old MaryTTS dependencies
        jcenter()
        maven("https://oss.sonatype.org/content/repositories/releases/")
        maven("https://marytts.github.io/marytts/maven/")
        maven("https://jitpack.io")
        maven("https://nrgxnat.jfrog.io/artifactory/libs-release/")
        maven("https://nexus.terrestris.de/repository/public/")

        // For jtok-core specifically
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://raw.githubusercontent.com/DFKI-MLT/Maven-Repository/main")
                }
            }
            filter {
                includeGroup("de.dfki.lt.jtok")
            }
        }
    }
}

// Plugins used for toolchain management
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Include subprojects
include(":composeApp")
