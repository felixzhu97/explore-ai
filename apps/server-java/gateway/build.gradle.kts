plugins {
    java
    `java-library`
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
}

group = "com.ai"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
    
    // Reactive
    implementation("io.projectreactor:reactor-core:3.7.0")
    
    // LangChain4j for AI/Agent integration
    implementation("dev.langchain4j:langchain4j-core:1.5.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.5.0")
    implementation("dev.langchain4j:langchain4j-ollama:1.5.0")
    
    // Kubernetes Client for K8sAgent
    implementation("io.kubernetes:client-java:21.0.0")
    
    // HTTP Client (for external service integration)
    implementation("io.projectreactor.netty:reactor-netty:1.1.23")
    
    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    
    // Validation
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
