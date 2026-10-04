package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
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
  private final OwnerPartitionScope ownerPartition;

  public JpaAutomationRunRepository(
      SpringDataAutomationRunRepository delegate,
      EntityManager entityManager,
      OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.entityManager = entityManager;
    this.ownerPartition = ownerPartition;
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
  public List<AutomationRun> findByScheduleIdAndOwnerKey(
      ScheduleId scheduleId, String ownerKey, int limit) {
    return ownerPartition.apply(
        OwnerKey.parse(ownerKey),
        () -> delegate.findByScheduleIdOrderByCreatedAtDesc(scheduleId, PageRequest.of(0, limit)));
  }
}
