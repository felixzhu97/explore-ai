package com.ai.metrics.infra.persistence;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.InvocationEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link AiInvocationEvent}. */
@Repository
public interface SpringDataAiInvocationEventRepository
    extends JpaRepository<AiInvocationEvent, InvocationEventId> {}
