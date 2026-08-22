package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.repository.AutomationRunRepository;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for automation run records. */
@Repository
public class JpaAutomationRunRepository implements AutomationRunRepository {

  private final SpringDataAutomationRunRepository delegate;

  /** Documentation. */
  public JpaAutomationRunRepository(SpringDataAutomationRunRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public AutomationRun save(AutomationRun run) {
    return delegate.saveAndFlush(run);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AutomationRun> findByScheduleIdAndClientId(
      ScheduleId scheduleId, String clientId, int limit) {
    return delegate.findByScheduleIdAndOwnerKeyOrderByStartedAtDesc(
        scheduleId, OwnerKey.parse(clientId), PageRequest.of(0, limit));
  }
}
