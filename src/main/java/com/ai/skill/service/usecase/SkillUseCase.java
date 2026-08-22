package com.ai.skill.service.usecase;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.service.SkillTemplate;
import java.util.List;

/** Documentation. */
public interface SkillUseCase {
  /** Documentation. */
  List<Skill> list(String clientId);

  /** Documentation. */
  Skill get(String clientId, String id);

  /** Documentation. */
  Skill create(
      String clientId,
      String name,
      String description,
      String instructions,
      List<String> allowedTools);

  /** Documentation. */
  Skill update(
      String clientId,
      String id,
      String name,
      String description,
      String instructions,
      List<String> allowedTools);

  /** Documentation. */
  Skill setEnabled(String clientId, String id, boolean enabled);

  /** Documentation. */
  void delete(String clientId, String id);

  /** Documentation. */
  List<SkillTemplate> listTemplates(String language);

  /** Documentation. */
  Skill createFromTemplate(String clientId, String templateId, String language);
}
