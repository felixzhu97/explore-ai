package com.ai.skill.infra.persistence;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link Skill}. */
@Repository
public interface SpringDataSkillRepository extends JpaRepository<Skill, SkillId> {

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  List<Skill> findAllByEnabledTrueAndIdIn(List<SkillId> ids);

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByName(String name);

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByNameAndIdNot(String name, SkillId excludeId);
}
