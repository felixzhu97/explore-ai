package com.ai.skill.controller.dto;

import com.ai.skill.service.SkillTemplate;
import com.ai.skill.service.SkillTemplateCatalog;
import java.util.List;

/** Documentation. */
public record SkillTemplateResponse(
    String id,
    String name,
    String description,
    String instructions,
    List<String> allowedTools,
    List<String> nameAliases) {
  /** Documentation. */
  public static SkillTemplateResponse from(SkillTemplate template) {
    return new SkillTemplateResponse(
        template.id(),
        template.name(),
        template.description(),
        template.instructions(),
        template.allowedTools(),
        List.copyOf(SkillTemplateCatalog.namesForTemplate(template.id())));
  }
}
