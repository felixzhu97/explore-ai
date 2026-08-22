package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for automation schedules. */
@Repository
public class JpaAutomationScheduleRepository implements AutomationScheduleRepository {

  private final SpringDataAutomationScheduleRepository delegate;

  /** Documentation. */
  public JpaAutomationScheduleRepository(SpringDataAutomationScheduleRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public AutomationSchedule save(AutomationSchedule schedule) {
    return delegate.saveAndFlush(schedule);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AutomationSchedule> findByIdAndClientId(ScheduleId id, String clientId) {
    return delegate.findByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AutomationSchedule> findAllByClientId(String clientId) {
    return delegate.findAllByOwnerKeyOrderByCreatedAtDesc(OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public int countByClientId(String clientId) {
    return delegate.countByOwnerKey(OwnerKey.parse(clientId));
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(ScheduleId id, String clientId) {
    delegate.deleteByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AutomationSchedule> findDue(Instant asOf, int limit) {
    return delegate.findByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
        asOf, PageRequest.of(0, limit));
  }

  @Override
  @Transactional
  public boolean claim(ScheduleId id, Instant expectedNextRunAt, Instant provisionalNextRunAt) {
    int updated = delegate.claimNextRun(id, expectedNextRunAt, provisionalNextRunAt, Instant.now());
    return updated == 1;
  }
}
