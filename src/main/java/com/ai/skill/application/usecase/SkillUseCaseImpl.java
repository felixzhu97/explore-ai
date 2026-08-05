package com.ai.skill.application.usecase;

import com.ai.skill.application.SkillTemplate;
import com.ai.skill.application.SkillTemplateCatalog;
import com.ai.skill.domain.exception.SkillNameConflictException;
import com.ai.skill.domain.exception.SkillNotFoundException;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillUseCaseImpl implements SkillUseCase {

    private final SkillRepository skillRepository;

    public SkillUseCaseImpl(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Override
    public List<Skill> list(String clientId) {
        return skillRepository.findAllByClientId(clientId);
    }

    @Override
    public Skill get(String clientId, String id) {
        return findOwnedSkill(clientId, id);
    }

    @Override
    public Skill create(
            String clientId,
            String name,
            String description,
            String instructions,
            List<String> allowedTools) {
        assertNameAvailable(clientId, name, null);
        Skill skill = Skill.create(clientId, name, description, instructions, allowedTools);
        return skillRepository.save(skill);
    }

    @Override
    public Skill update(
            String clientId,
            String id,
            String name,
            String description,
            String instructions,
            List<String> allowedTools) {
        Skill skill = findOwnedSkill(clientId, id);
        assertNameAvailable(clientId, name, skill.getId());
        skill.update(name, description, instructions, allowedTools);
        return skillRepository.save(skill);
    }

    @Override
    public Skill setEnabled(String clientId, String id, boolean enabled) {
        Skill skill = findOwnedSkill(clientId, id);
        if (enabled) {
            skill.enable();
        } else {
            skill.disable();
        }
        return skillRepository.save(skill);
    }

    @Override
    public void delete(String clientId, String id) {
        findOwnedSkill(clientId, id);
        skillRepository.deleteByIdAndClientId(SkillId.of(id), clientId);
    }

    @Override
    public List<SkillTemplate> listTemplates(String language) {
        return SkillTemplateCatalog.listAll(language);
    }

    @Override
    public Skill createFromTemplate(String clientId, String templateId, String language) {
        SkillTemplate template = SkillTemplateCatalog.findById(templateId, language)
                .orElseThrow(() -> new IllegalArgumentException("Unknown skill template: " + templateId));
        return create(
                clientId,
                nextAvailableName(clientId, template.name()),
                template.description(),
                template.instructions(),
                template.allowedTools());
    }

    private Skill findOwnedSkill(String clientId, String id) {
        return skillRepository.findByIdAndClientId(SkillId.of(id), clientId)
                .orElseThrow(() -> new SkillNotFoundException(id));
    }

    private void assertNameAvailable(String clientId, String name, SkillId excludeId) {
        if (skillRepository.existsByClientIdAndNameIgnoringId(clientId, name, excludeId)) {
            throw new SkillNameConflictException(name);
        }
    }

    private String nextAvailableName(String clientId, String baseName) {
        if (!skillRepository.existsByClientIdAndNameIgnoringId(clientId, baseName, null)) {
            return baseName;
        }
        for (int suffix = 2; suffix <= 99; suffix++) {
            String candidate = baseName + " (" + suffix + ")";
            if (!skillRepository.existsByClientIdAndNameIgnoringId(clientId, candidate, null)) {
                return candidate;
            }
        }
        return baseName + " (" + SkillId.generate().value().substring(0, 8) + ")";
    }
}
