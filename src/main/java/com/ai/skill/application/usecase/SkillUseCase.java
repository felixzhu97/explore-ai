package com.ai.skill.application.usecase;

import com.ai.skill.application.SkillTemplate;
import com.ai.skill.domain.model.Skill;

import java.util.List;

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
