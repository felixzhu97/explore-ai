plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.projectreactor:reactor-core:3.7.0")

    implementation("dev.langchain4j:langchain4j-core:1.5.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.5.0")
    implementation("dev.langchain4j:langchain4j-ollama:1.5.0")
    implementation("dev.langchain4j:langchain4j-qdrant:1.5.0")
    implementation("dev.langchain4j:langchain4j-hugging-face:1.5.0")

    implementation(project(":common"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:qdrant:1.19.8")
}
