plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
}

group = "com.ai"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/spring") }
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:2.0.0")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springframework.ai:spring-ai-client-chat")  // Required for ChatClient and MessageChatMemoryAdvisor
    implementation("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MCP Server (webmvc, includes SSE + Streamable)
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
    testImplementation("org.springframework.ai:spring-ai-starter-mcp-server")

    // Local system monitoring
    implementation("com.github.oshi:oshi-core:6.8.0")

    // HTML parsing (for DuckDuckGo web search via HTML endpoint)
    implementation("org.jsoup:jsoup:1.18.3")

    // PDF Processing
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.hypersistence:hypersistence-utils-hibernate-73:3.15.2")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("com.h2database:h2")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testRuntimeOnly("org.testcontainers:postgresql:1.20.4")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(files(classDirectories.files.map { f ->
        fileTree(f) {
            exclude("com/ai/infrastructure/config/**")
        }
    }))
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    
    classDirectories.setFrom(files(classDirectories.files.map { f ->
        fileTree(f) {
            exclude("com/ai/infrastructure/config/**")
        }
    }))

    violationRules {
        rule {
            element = "PACKAGE"
            excludes = listOf(
                "com.ai.infrastructure.config.*"
            )
        }
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.87")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.70")
            }
        }
    }
}
