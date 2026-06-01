import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    id("org.owasp.dependencycheck") version "10.0.4"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=com.ureka.play4change")
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.firebase.messaging.ktx)
            // Phase 5 — background fetch
            implementation("androidx.work:work-runtime-ktx:2.9.1")
            implementation("io.insert-koin:koin-androidx-workmanager:3.5.3")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Architecture
            implementation(libs.decompose)
            implementation(libs.decompose.compose)

            // Utils
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            //koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            //ktor
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.json)

            //icons
            implementation(compose.materialIconsExtended)

            //dependencies
            implementation(project(":common"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.ureka.play4change"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ureka.play4change"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField(
                "String", "BASE_URL",
                "\"${project.findProperty("BASE_URL") ?: "http://10.0.2.2:8080"}\""
            )
            buildConfigField(
                "Boolean", "USE_MOCKS",
                "${project.findProperty("USE_MOCKS") ?: "false"}"
            )
            buildConfigField(
                "String", "FIREBASE_APP_ID",
                "\"${project.findProperty("FIREBASE_APP_ID") ?: ""}\""
            )
            buildConfigField(
                "String", "FIREBASE_PROJECT_ID",
                "\"${project.findProperty("FIREBASE_PROJECT_ID") ?: ""}\""
            )
            buildConfigField(
                "String", "FIREBASE_API_KEY",
                "\"${project.findProperty("FIREBASE_API_KEY") ?: ""}\""
            )
            buildConfigField(
                "String", "FIREBASE_SENDER_ID",
                "\"${project.findProperty("FIREBASE_SENDER_ID") ?: ""}\""
            )
        }
        release {
            isMinifyEnabled = false
            buildConfigField(
                "String", "BASE_URL",
                "\"${project.findProperty("BASE_URL") ?: "http://10.0.2.2:8080"}\""
            )
            buildConfigField("Boolean", "USE_MOCKS", "false")
            buildConfigField(
                "String", "FIREBASE_APP_ID",
                "\"${project.findProperty("FIREBASE_APP_ID") ?: ""}\""
            )
            buildConfigField(
                "String", "FIREBASE_PROJECT_ID",
                "\"${project.findProperty("FIREBASE_PROJECT_ID") ?: ""}\""
            )
            buildConfigField(
                "String", "FIREBASE_API_KEY",
                "\"${project.findProperty("FIREBASE_API_KEY") ?: ""}\""
            )
            buildConfigField(
                "String", "FIREBASE_SENDER_ID",
                "\"${project.findProperty("FIREBASE_SENDER_ID") ?: ""}\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(libs.compose.multiplatform.uiTooling)
}

// NOTE: dep-check 10.0.4 Gradle plugin has a Jackson version mismatch bug (see HACKS.md H04).
// ':composeApp:dependencyCheckAnalyze' fails locally. CI uses dep-check CLI via the
// copyRuntimeDependencies task + dependency-check/Dependency-Check_Action@main.
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "$projectDir/dependency-check-suppression.xml"
}

// Copies Android debug runtime JARs to build/dependencies for the CI dep-check CLI scan.
// Uses lenient artifact view to avoid KMP variant ambiguity with the :common AAR dependency.
tasks.register("copyRuntimeDependencies") {
    val outDir = layout.buildDirectory.dir("dependencies")
    outputs.dir(outDir)
    doLast {
        val jars = configurations.getByName("androidDebugRuntimeClasspath")
            .incoming
            .artifactView { lenient(true) }
            .artifacts
            .artifactFiles
            .filter { it.name.endsWith(".jar") }
        copy {
            from(jars)
            into(outDir)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
