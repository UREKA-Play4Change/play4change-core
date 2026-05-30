plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.kotlinSerialization)
    id("dev.detekt") version "2.0.0-alpha.2"
    id("org.owasp.dependencycheck") version "10.0.4"
    id("com.github.spotbugs") version "6.0.26"
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
    implementation("org.flywaydb:flyway-core")



    // LangChain4j — core + Mistral provider
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.mistral)
    implementation(libs.langchain4j.pgvector)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation(libs.mockk)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

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

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Rate limiting
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // Phase 3 — content & storage
    implementation("software.amazon.awssdk:s3:2.25.31")
    implementation("org.apache.pdfbox:pdfbox:3.0.1")
    implementation("org.jsoup:jsoup:1.17.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
}

spotbugs {
    toolVersion.set("4.8.6")
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
    excludeFilter.set(file("spotbugs-exclude.xml"))
    ignoreFailures.set(false)
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")
}

tasks.named("spotbugsMain") {
    dependsOn("compileKotlin")
}
// Emit HTML reports for human review alongside the XML report required by CI
tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("xml") { required.set(true) }
    reports.create("html") { required.set(true) }
}

// NOTE: dep-check 10.0.4 has a Jackson version mismatch bug — compiled against 2.17+ but its
// plugin classpath resolves to 2.14.2 via Gradle's isolated plugin classloader.
// Project-level resolutionStrategy cannot reach the plugin classloader, so the Gradle task
// './gradlew :server:dependencyCheckAnalyze' currently fails locally.
// CI uses the dep-check CLI via GitHub Actions (see .github/workflows/dependency-check.yml)
// which does not use Gradle's plugin mechanism and is not affected.
// See HACKS.md H04. Remove this comment when dep-check ships a fixed Jackson dependency.
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "$projectDir/dependency-check-suppression.xml"
}

// Copies all runtime JARs to build/dependencies for the CI dep-check CLI scan.
tasks.register<Copy>("copyRuntimeDependencies") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("dependencies"))
}
