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

    // Image processing
    implementation("org.bytedeco:javacv-platform:1.5.10")
    implementation("org.bytedeco:opencv:4.9.0-1.5.10")
    implementation("org.bytedeco:leptonica:1.84.1-1.5.10")
    implementation("net.sourceforge.tess4j:tess4j:5.16.0")

    // Common module
    implementation(project(":common"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
