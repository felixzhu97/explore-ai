package com.ai.base.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Global JPA auditing and repository scanning. Schema is managed by Liquibase on startup. */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = {
      "com.ai.adapter.out.persistence",
      "com.ai.rag.domain.repository",
      "com.ai.rag.infra.storage",
      "com.ai.chat.infra.persistence",
      "com.ai.rag.infra.persistence",
      "com.ai.skill.infra.persistence",
      "com.ai.pipeline.infra.persistence",
      "com.ai.automation.infra.persistence",
      "com.ai.account.infra.persistence",
      "com.ai.metrics.infra.persistence"
    })
@EnableTransactionManagement
public class BaseJpaConfig {}
