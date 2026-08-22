package com.ai.skill.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link Skill}. */
@Repository
public interface SpringDataSkillRepository extends JpaRepository<Skill, SkillId> {

  /** Documentation. */
  java.util.Optional<Skill> findByIdAndOwnerKey(SkillId id, OwnerKey ownerKey);

  /** Documentation. */
  List<Skill> findAllByOwnerKeyOrderByNameAsc(OwnerKey ownerKey);

  /** Documentation. */
  List<Skill> findAllByOwnerKeyAndEnabledTrueAndIdIn(OwnerKey ownerKey, List<SkillId> ids);

  /** Documentation. */
  void deleteByIdAndOwnerKey(SkillId id, OwnerKey ownerKey);

  /** Documentation. */
  boolean existsByOwnerKeyAndName(OwnerKey ownerKey, String name);

  /** Documentation. */
  boolean existsByOwnerKeyAndNameAndIdNot(OwnerKey ownerKey, String name, SkillId excludeId);
}
