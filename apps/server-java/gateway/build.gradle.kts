plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // LangChain4j for AI model integration
    implementation("dev.langchain4j:langchain4j-core:1.5.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.5.0")

    // AI Agents module
    implementation(project(":services:ai-agents"))

    // RAG Service module
    implementation(project(":services:rag-service"))

    // TTS Service module
    implementation(project(":services:tts-service"))

    // Vision Service module
    implementation(project(":services:vision-service"))

    // Media Agent module
    implementation(project(":services:media-agent"))

    implementation(project(":common"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
