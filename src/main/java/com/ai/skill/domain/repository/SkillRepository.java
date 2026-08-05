package com.ai.skill.domain.repository;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.vo.SkillId;

import java.util.List;
import java.util.Optional;

public interface SkillRepository {

    Skill save(Skill skill);

    Optional<Skill> findByIdAndClientId(SkillId id, String clientId);

    List<Skill> findAllByClientId(String clientId);

    List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids);

    void deleteByIdAndClientId(SkillId id, String clientId);

    boolean existsByClientIdAndNameIgnoringId(String clientId, String name, SkillId excludeId);
}
