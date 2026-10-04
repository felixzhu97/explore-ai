package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.ScheduleId;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link AutomationSchedule}. */
@Repository
public interface SpringDataAutomationScheduleRepository
    extends JpaRepository<AutomationSchedule, ScheduleId> {

  List<AutomationSchedule> findByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
      Instant asOf, Pageable pageable);

  @Modifying
  @Query(
      """
      UPDATE VERSIONED AutomationSchedule s
      SET s.nextRunAt = :provisionalNextRunAt, s.updatedAt = :now
      WHERE s.id = :id AND s.enabled = true AND s.nextRunAt = :expectedNextRunAt
      """)
  int claimNextRun(
      @Param("id") ScheduleId id,
      @Param("expectedNextRunAt") Instant expectedNextRunAt,
      @Param("provisionalNextRunAt") Instant provisionalNextRunAt,
      @Param("now") Instant now);
}
