package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for automation run records. */
@Repository
public class JpaAutomationRunRepository implements AutomationRunRepository {

  private final SpringDataAutomationRunRepository delegate;
  private final EntityManager entityManager;

  /** Documentation. */
  public JpaAutomationRunRepository(
      SpringDataAutomationRunRepository delegate, EntityManager entityManager) {
    this.delegate = delegate;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional
  public AutomationRun save(AutomationRun run) {
    entityManager.persist(run);
    entityManager.flush();
    return run;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AutomationRun> findByScheduleIdAndClientId(
      ScheduleId scheduleId, String clientId, int limit) {
    return delegate.findByScheduleIdAndOwnerKeyOrderByCreatedAtDesc(
        scheduleId, OwnerKey.parse(clientId), PageRequest.of(0, limit));
  }
}
