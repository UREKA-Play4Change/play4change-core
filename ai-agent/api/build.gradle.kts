plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(project(":common"))

    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
}