package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for automation schedules. */
@Repository
public class JpaAutomationScheduleRepository implements AutomationScheduleRepository {

  private final SpringDataAutomationScheduleRepository delegate;
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

  private final OwnerPartitionScope ownerPartition;

  public JpaAutomationScheduleRepository(
      SpringDataAutomationScheduleRepository delegate, OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.ownerPartition = ownerPartition;
  }

  @Override
  @Transactional
  public AutomationSchedule save(AutomationSchedule schedule) {
    return delegate.saveAndFlush(schedule);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AutomationSchedule> findByIdAndClientId(ScheduleId id, String clientId) {
    return ownerPartition.findOne(OwnerKey.parse(clientId), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AutomationSchedule> findAllByClientId(String clientId) {
    return ownerPartition.apply(OwnerKey.parse(clientId), () -> delegate.findAll(NEWEST_FIRST));
  }

  @Override
  @Transactional(readOnly = true)
  public int countByClientId(String clientId) {
    return Math.toIntExact(ownerPartition.apply(OwnerKey.parse(clientId), delegate::count));
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(ScheduleId id, String clientId) {
    ownerPartition.run(OwnerKey.parse(clientId), () -> delegate.deleteById(id));
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
