package com.ai.skill.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for skill aggregates. */
@Repository
public class JpaSkillRepository implements SkillRepository {

  private final SpringDataSkillRepository delegate;

  /** Documentation. */
  public JpaSkillRepository(SpringDataSkillRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public Skill save(Skill skill) {
    return delegate.saveAndFlush(skill);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Skill> findByIdAndClientId(SkillId id, String clientId) {
    return delegate.findByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Skill> findAllByClientId(String clientId) {
    return delegate.findAllByOwnerKeyOrderByNameAsc(OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return delegate.findAllByOwnerKeyAndEnabledTrueAndIdIn(OwnerKey.parse(clientId), ids);
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(SkillId id, String clientId) {
    delegate.deleteByIdAndOwnerKey(id, OwnerKey.parse(clientId));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, SkillId excludeId) {
    OwnerKey ownerKey = OwnerKey.parse(clientId);
    if (excludeId == null) {
      return delegate.existsByOwnerKeyAndName(ownerKey, name);
    }
    return delegate.existsByOwnerKeyAndNameAndIdNot(ownerKey, name, excludeId);
  }
}
