package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.repository.AutomationScheduleRepository;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for automation schedules. */
@Repository
@RequiredArgsConstructor
public class JpaAutomationScheduleRepository implements AutomationScheduleRepository {

  private final SpringDataAutomationScheduleRepository delegate;
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

  private final OwnerPartitionScope ownerPartition;

  @Override
  @Transactional
  public AutomationSchedule save(AutomationSchedule schedule) {
    return delegate.saveAndFlush(schedule);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AutomationSchedule> findByIdAndOwnerKey(ScheduleId id, String ownerKey) {
    return ownerPartition.findOne(OwnerKey.parse(ownerKey), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AutomationSchedule> findAllByOwnerKey(String ownerKey) {
    return ownerPartition.apply(OwnerKey.parse(ownerKey), () -> delegate.findAll(NEWEST_FIRST));
  }

  @Override
  @Transactional(readOnly = true)
  public int countByOwnerKey(String ownerKey) {
    return Math.toIntExact(ownerPartition.apply(OwnerKey.parse(ownerKey), delegate::count));
  }

  @Override
  @Transactional
  public void deleteByIdAndOwnerKey(ScheduleId id, String ownerKey) {
    ownerPartition.run(OwnerKey.parse(ownerKey), () -> delegate.deleteById(id));
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
