plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.ureka.play4change"
version = "0.0.0"

dependencies {
    implementation(project(":common"))
    implementation(project(":ai-agent:api"))
    implementation(project(":ai-agent:langchain"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    runtimeOnly(libs.postgresql)


    // LangChain4j — core + Mistral provider
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.mistral)
    implementation(libs.langchain4j.pgvector)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)

    implementation(libs.spring.boot.starter.security)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    implementation(libs.spring.security.crypto)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

}

kotlin {
    jvmToolchain(21)
}