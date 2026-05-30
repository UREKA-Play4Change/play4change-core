import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("org.owasp.dependencycheck") version "10.0.4"
}
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvmToolchain(21)

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Core"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.uuid)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.ureka.play4change.common"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
}

// NOTE: dep-check 10.0.4 Gradle plugin has a Jackson version mismatch bug (see HACKS.md H04).
// ':common:dependencyCheckAnalyze' fails locally. CI uses dep-check CLI via the
// copyRuntimeDependencies task + dependency-check/Dependency-Check_Action@main.
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "$projectDir/dependency-check-suppression.xml"
}

// Copies JVM runtime JARs to build/dependencies for the CI dep-check CLI scan.
tasks.register<Copy>("copyRuntimeDependencies") {
    from(configurations.getByName("jvmRuntimeClasspath"))
    into(layout.buildDirectory.dir("dependencies"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}