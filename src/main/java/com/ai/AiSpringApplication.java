package com.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI chat service Spring Boot application main class.
 */
@SpringBootApplication
@ComponentScan(basePackages = { "com.ai", "com.ai.rag" })
public class AiSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSpringApplication.class, args);
    }
}
