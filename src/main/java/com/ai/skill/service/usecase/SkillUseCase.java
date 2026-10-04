package com.ai.skill.service.usecase;

import com.ai.skill.domain.model.Skill;
import com.ai.skill.service.SkillTemplate;
import java.util.List;

/** Owner-scoped CRUD, enablement, and template instantiation for user-defined skills. */
public interface SkillUseCase {
  List<Skill> list(String clientId);

  Skill get(String clientId, String id);

  Skill create(
      String clientId,
      String name,
      String description,
      String instructions,
      List<String> allowedTools);

  Skill update(
      String clientId,
      String id,
      String name,
      String description,
      String instructions,
      List<String> allowedTools);

  Skill setEnabled(String clientId, String id, boolean enabled);

  void delete(String clientId, String id);

  List<SkillTemplate> listTemplates(String language);

  Skill createFromTemplate(String clientId, String templateId, String language);
}
