package com.ai.skill.domain.repository;

import com.ai.common.domain.repository.OwnerScopedRepository;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;

/** Owner-scoped repository for skills, including lookup of enabled skills by id. */
public interface SkillRepository extends OwnerScopedRepository<Skill, SkillId> {

  List<Skill> findEnabledByOwnerKeyAndIds(String ownerKey, List<SkillId> ids);
}
