package com.ai.rag.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA repository scanning. Schema is managed by Liquibase on startup.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.ai.adapter.out.persistence",
    "com.ai.rag.domain",
    "com.ai.rag.infrastructure"
})
@EnableTransactionManagement
public class JpaConfig {
}
