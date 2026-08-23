package com.ai.skill.domain.repository;

import com.ai.common.domain.repository.ClientIdOwnedNamedRepository;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;

/** Documentation. */
public interface SkillRepository extends ClientIdOwnedNamedRepository<Skill, SkillId> {

  /** Documentation. */
  List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids);
}
