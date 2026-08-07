package com.ai.automation.domain.repository;

import com.ai.automation.domain.model.AutomationSchedule;
import com.ai.automation.domain.vo.ScheduleId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AutomationScheduleRepository {

    AutomationSchedule save(AutomationSchedule schedule);

    Optional<AutomationSchedule> findByIdAndClientId(ScheduleId id, String clientId);

    List<AutomationSchedule> findAllByClientId(String clientId);

    int countByClientId(String clientId);

    void deleteByIdAndClientId(ScheduleId id, String clientId);

    List<AutomationSchedule> findDue(Instant asOf, int limit);

    /**
     * Optimistic claim: advances {@code next_run_at} only when it still matches {@code expectedNextRunAt}.
     *
     * @return true if this caller won the claim
     */
    boolean claim(ScheduleId id, Instant expectedNextRunAt, Instant provisionalNextRunAt);
}
