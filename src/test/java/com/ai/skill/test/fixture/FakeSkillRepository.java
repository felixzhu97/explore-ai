package com.ai.skill.test.fixture;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.model.SkillId;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.testsupport.fake.AbstractOwnerScopedFakeRepository;
import java.util.List;
import java.util.Optional;

/** In-memory {@link SkillRepository} for service-layer unit tests. */
public class FakeSkillRepository extends AbstractOwnerScopedFakeRepository<Skill, SkillId>
    implements SkillRepository {

  @Override
  protected SkillId getId(Skill entity) {
    return entity.getId();
  }

  @Override
  protected String getOwnerKeyValue(Skill entity) {
    return entity.getOwnerKeyValue();
  }

  @Override
  protected String getName(Skill entity) {
    return entity.getName();
  }

  @Override
  public Skill save(Skill skill) {
    return super.save(skill);
  }

  @Override
  public Optional<Skill> findByIdAndOwnerKey(SkillId id, String ownerKey) {
    return super.findByIdAndOwnerKey(id, ownerKey);
  }

  @Override
  public List<Skill> findAllByOwnerKey(String ownerKey) {
    return super.findAllByOwnerKey(ownerKey);
  }

  @Override
  public List<Skill> findEnabledByOwnerKeyAndIds(String ownerKey, List<SkillId> ids) {
    return findAllByOwnerKey(ownerKey).stream()
        .filter(Skill::isEnabled)
        .filter(skill -> ids.contains(skill.getId()))
        .toList();
  }

  @Override
  public void deleteByIdAndOwnerKey(SkillId id, String ownerKey) {
    super.deleteByIdAndOwnerKey(id, ownerKey);
  }

  @Override
  public boolean existsByOwnerKeyAndNameIgnoringId(
      String ownerKey, String name, SkillId excludeId) {
    return super.existsByOwnerKeyAndNameIgnoringId(ownerKey, name, excludeId);
  }
}
