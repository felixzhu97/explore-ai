package com.ai.skill.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for skill aggregates. */
@Repository
public class JpaSkillRepository implements SkillRepository {

  private final SpringDataSkillRepository delegate;
  private static final Sort BY_NAME = Sort.by("name");

  private final OwnerPartitionScope ownerPartition;

  public JpaSkillRepository(
      SpringDataSkillRepository delegate, OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.ownerPartition = ownerPartition;
  }

  @Override
  @Transactional
  public Skill save(Skill skill) {
    return delegate.saveAndFlush(skill);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Skill> findByIdAndClientId(SkillId id, String clientId) {
    return ownerPartition.findOne(OwnerKey.parse(clientId), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Skill> findAllByClientId(String clientId) {
    return ownerPartition.apply(OwnerKey.parse(clientId), () -> delegate.findAll(BY_NAME));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return ownerPartition.apply(
        OwnerKey.parse(clientId), () -> delegate.findAllByEnabledTrueAndIdIn(ids));
  }

  @Override
  @Transactional
  public void deleteByIdAndClientId(SkillId id, String clientId) {
    ownerPartition.run(OwnerKey.parse(clientId), () -> delegate.deleteById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, SkillId excludeId) {
    return ownerPartition.apply(
        OwnerKey.parse(clientId),
        () ->
            excludeId == null
                ? delegate.existsByName(name)
                : delegate.existsByNameAndIdNot(name, excludeId));
  }
}
