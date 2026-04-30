import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// spring-boot-gradle-plugin 3.2.x transitively loads spring-boot-buildpack-platform which
// bundles jackson-databind 2.14.2 into the buildscript classloader. Due to JVM parent-first
// classloading, the OWASP dependency-check plugin's BlackbirdModule (compiled against Jackson
// 2.16.0) finds the older NativeImageUtil from 2.14.2, which lacks isInNativeImage(), causing
// a NoSuchMethodError. Force all com.fasterxml.jackson.* in the buildscript classpath to 2.16.1.
buildscript {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group.startsWith("com.fasterxml.jackson")) {
                useVersion("2.16.1")
                because("OWASP dependency-check requires Jackson 2.16.0+ for NativeImageUtil.isInNativeImage()")
            }
        }
    }
}

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.springBoot) apply false
    alias(libs.plugins.springDependencyManagement) apply false
    alias(libs.plugins.kotlinSpring) apply false
}

allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
