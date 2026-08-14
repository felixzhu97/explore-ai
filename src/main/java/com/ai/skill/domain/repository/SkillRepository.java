package com.ai.skill.domain.repository;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;
import java.util.Optional;

/** Documentation. */
public interface SkillRepository {
  /** Documentation. */
  Skill save(Skill skill);

  /** Documentation. */
  Optional<Skill> findByIdAndClientId(SkillId id, String clientId);

  /** Documentation. */
  List<Skill> findAllByClientId(String clientId);

  /** Documentation. */
  List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids);

  /** Documentation. */
  void deleteByIdAndClientId(SkillId id, String clientId);

  /** Documentation. */
  boolean existsByClientIdAndNameIgnoringId(String clientId, String name, SkillId excludeId);
}
