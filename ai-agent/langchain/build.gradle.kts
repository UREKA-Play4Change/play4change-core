plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
}

dependencies {
    // Contract to implement
    implementation(project(":ai-agent:api"))
    implementation(project(":common"))

    implementation(libs.kotlinx.serialization.json)
    // Spring (for @Service, @Component, @Value injection)
    implementation(libs.spring.boot.starter.web)

    // LangChain4j — core + Mistral provider
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.mistral)
    implementation(libs.langchain4j.pgvector)

    // Database — pgvector for similarity search
    implementation(libs.postgresql.driver)
    implementation(libs.pgvector.jdbc)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)

    // Observability
    implementation(libs.micrometer.core)

    implementation(libs.postgresql.driver)
    implementation(libs.pgvector.jdbc)

    // Add this to provide JdbcTemplate and Spring Data capabilities
    implementation(libs.spring.boot.starter.data.jpa)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.runner)
}