plugins {
    java
    id("org.springframework.boot") version "3.4.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot") apply false
    apply(plugin = "io.spring.dependency-management")

    group = "com.ai"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.2"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.assertj:assertj-core:3.26.3")
        testImplementation("org.mockito:mockito-core:5.11.0")
        testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
