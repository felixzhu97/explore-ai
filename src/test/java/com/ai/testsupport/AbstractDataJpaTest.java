package com.ai.testsupport;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base for {@link DataJpaTest} slices using the shared {@code test} profile (H2 in-memory,
 * Liquibase disabled). Subclasses add {@code @EntityScan} (module domain plus {@code
 * com.ai.base.domain} and {@code com.ai.common.domain}), {@code @EnableJpaRepositories} for Spring
 * Data interfaces, and {@code @Import} only for concrete repository adapters.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractDataJpaTest {}
