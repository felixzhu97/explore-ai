package com.ai.skill.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for skill aggregates. */
@Repository
@RequiredArgsConstructor
public class JpaSkillRepository implements SkillRepository {

  private final SpringDataSkillRepository delegate;
  private static final Sort BY_NAME = Sort.by("name");

  private final OwnerPartitionScope ownerPartition;

  @Override
  @Transactional(readOnly = true)
  public Optional<Skill> findByIdAndOwnerKey(SkillId id, String ownerKey) {
    return ownerPartition.findOne(OwnerKey.parse(ownerKey), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Skill> findAllByOwnerKey(String ownerKey) {
    return ownerPartition.apply(OwnerKey.parse(ownerKey), () -> delegate.findAll(BY_NAME));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Skill> findEnabledByOwnerKeyAndIds(String ownerKey, List<SkillId> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return ownerPartition.apply(
        OwnerKey.parse(ownerKey), () -> delegate.findAllByEnabledTrueAndIdIn(ids));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByOwnerKeyAndNameIgnoringId(
      String ownerKey, String name, SkillId excludeId) {
    return ownerPartition.apply(
        OwnerKey.parse(ownerKey),
        () ->
            excludeId == null
                ? delegate.existsByName(name)
                : delegate.existsByNameAndIdNot(name, excludeId));
  }

  @Override
  @Transactional
  public Skill save(Skill skill) {
    return delegate.saveAndFlush(skill);
  }

  @Override
  @Transactional
  public void deleteByIdAndOwnerKey(SkillId id, String ownerKey) {
    ownerPartition.run(OwnerKey.parse(ownerKey), () -> delegate.deleteById(id));
  }
}
