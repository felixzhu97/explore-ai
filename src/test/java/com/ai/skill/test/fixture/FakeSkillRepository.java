package com.ai.skill.test.fixture;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** In-memory {@link SkillRepository} for service-layer unit tests. */
public class FakeSkillRepository implements SkillRepository {

  private final List<Skill> skills = new ArrayList<>();
  private int saveCount;

  /** Seeds a skill and returns it for test setup. */
  public Skill seed(Skill skill) {
    skills.add(skill);
    return skill;
  }

  /** Returns how many times {@link #save(Skill)} was invoked. */
  public int saveCount() {
    return saveCount;
  }

  @Override
  public Skill save(Skill skill) {
    saveCount++;
    skills.removeIf(existing -> existing.getId().equals(skill.getId()));
    skills.add(skill);
    return skill;
  }

  @Override
  public Optional<Skill> findByIdAndClientId(SkillId id, String clientId) {
    return skills.stream()
        .filter(skill -> skill.getId().equals(id) && skill.getClientId().equals(clientId))
        .findFirst();
  }

  @Override
  public List<Skill> findAllByClientId(String clientId) {
    return skills.stream().filter(skill -> skill.getClientId().equals(clientId)).toList();
  }

  @Override
  public List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids) {
    return skills.stream()
        .filter(skill -> skill.getClientId().equals(clientId))
        .filter(Skill::isEnabled)
        .filter(skill -> ids.contains(skill.getId()))
        .toList();
  }

  @Override
  public void deleteByIdAndClientId(SkillId id, String clientId) {
    skills.removeIf(skill -> skill.getId().equals(id) && skill.getClientId().equals(clientId));
  }

  @Override
  public boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, SkillId excludeId) {
    return skills.stream()
        .filter(skill -> skill.getClientId().equals(clientId))
        .filter(skill -> skill.getName().equals(name))
        .anyMatch(skill -> excludeId == null || !skill.getId().equals(excludeId));
  }
}
