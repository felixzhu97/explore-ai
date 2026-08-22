package com.ai.automation.infra.persistence;

import com.ai.automation.domain.model.AutomationRun;
import com.ai.automation.domain.vo.RunId;
import com.ai.automation.domain.vo.ScheduleId;
import com.ai.common.domain.vo.OwnerKey;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link AutomationRun}. */
@Repository
public interface SpringDataAutomationRunRepository extends JpaRepository<AutomationRun, RunId> {

  /** Documentation. */
  List<AutomationRun> findByScheduleIdAndOwnerKeyOrderByStartedAtDesc(
      ScheduleId scheduleId, OwnerKey ownerKey, Pageable pageable);
}
