plugins {
    java
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework:spring-web")
    api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    api("io.projectreactor:reactor-core:3.7.0")
    api("jakarta.validation:jakarta.validation-api:3.0.2")
    api("org.jspecify:jspecify:1.0.0")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}
