package com.ai.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base for {@link DataJpaTest} slices on the shared {@code test} profile (H2 in-memory, schema from
 * the entities). Entities and repositories come from the application package scan.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractDataJpaTest {

  @Autowired protected TestEntityManager em;

  /** Writes pending changes and detaches everything so the next read hits the database. */
  protected void flushAndClear() {
    em.flush();
    em.clear();
  }
}
