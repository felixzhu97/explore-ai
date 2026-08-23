package com.ai.skill.test.fixture;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import com.ai.testsupport.fake.AbstractClientIdOwnedFakeRepository;
import java.util.List;
import java.util.Optional;

/** In-memory {@link SkillRepository} for service-layer unit tests. */
public class FakeSkillRepository extends AbstractClientIdOwnedFakeRepository<Skill, SkillId>
    implements SkillRepository {

  @Override
  protected SkillId getId(Skill entity) {
    return entity.getId();
  }

  @Override
  protected String getClientId(Skill entity) {
    return entity.getClientId();
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
  public Optional<Skill> findByIdAndClientId(SkillId id, String clientId) {
    return super.findByIdAndClientId(id, clientId);
  }

  @Override
  public List<Skill> findAllByClientId(String clientId) {
    return super.findAllByClientId(clientId);
  }

  @Override
  public List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids) {
    return findAllByClientId(clientId).stream()
        .filter(Skill::isEnabled)
        .filter(skill -> ids.contains(skill.getId()))
        .toList();
  }

  @Override
  public void deleteByIdAndClientId(SkillId id, String clientId) {
    super.deleteByIdAndClientId(id, clientId);
  }

  @Override
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, SkillId excludeId) {
    return super.existsByClientIdAndNameIgnoringId(clientId, name, excludeId);
  }
}
