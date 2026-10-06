package com.ai.skill.service;

import com.ai.skill.domain.exception.SkillNameConflictException;
import com.ai.skill.domain.exception.SkillNotFoundException;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Skill use case that enforces per-owner unique names and derives names from templates. */
@Service
@RequiredArgsConstructor
public class SkillService {

  private final SkillRepository skillRepository;

  /** Lists the owner's skills. */
  public List<Skill> list(String ownerKey) {
    return skillRepository.findAllByOwnerKey(ownerKey);
  }

  /** Lists the built-in skill templates. */
  public List<SkillTemplate> listTemplates(String language) {
    return SkillTemplateCatalog.listAll(language);
  }

  /** Returns the owner's skill. */
  public Skill get(String ownerKey, String id) {
    return findOwnedSkill(ownerKey, id);
  }

  /** Creates a skill, rejecting a taken name. */
  public Skill create(
      String ownerKey,
      String name,
      String description,
      String instructions,
      List<String> allowedTools) {
    assertNameAvailable(ownerKey, name, null);
    Skill skill = Skill.create(ownerKey, name, description, instructions, allowedTools);
    return skillRepository.save(skill);
  }

  /** Copies a built-in skill template into the owner's skills. */
  public Skill createFromTemplate(String ownerKey, String templateId, String language) {
    SkillTemplate template =
        SkillTemplateCatalog.findById(templateId, language)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown skill template: " + templateId));
    return create(
        ownerKey,
        findNextAvailableName(ownerKey, template.name()),
        template.description(),
        template.instructions(),
        template.allowedTools());
  }

  /** Replaces the owner's skill, keeping names unique per owner. */
  public Skill update(
      String ownerKey,
      String id,
      String name,
      String description,
      String instructions,
      List<String> allowedTools) {
    Skill skill = findOwnedSkill(ownerKey, id);
    assertNameAvailable(ownerKey, name, skill.getId());
    skill.update(name, description, instructions, allowedTools);
    return skillRepository.save(skill);
  }

  /** Enables or disables the owner's skill. */
  public Skill setEnabled(String ownerKey, String id, boolean enabled) {
    Skill skill = findOwnedSkill(ownerKey, id);
    if (enabled) {
      skill.enable();
    } else {
      skill.disable();
    }
    return skillRepository.save(skill);
  }

  /** Deletes the owner's skill. */
  public void delete(String ownerKey, String id) {
    findOwnedSkill(ownerKey, id);
    skillRepository.deleteByIdAndOwnerKey(SkillId.of(id), ownerKey);
  }

  private Skill findOwnedSkill(String ownerKey, String id) {
    return skillRepository
        .findByIdAndOwnerKey(SkillId.of(id), ownerKey)
        .orElseThrow(() -> new SkillNotFoundException(id));
  }

  private void assertNameAvailable(String ownerKey, String name, SkillId excludeId) {
    if (skillRepository.existsByOwnerKeyAndNameIgnoringId(ownerKey, name, excludeId)) {
      throw new SkillNameConflictException(name);
    }
  }

  private String findNextAvailableName(String ownerKey, String baseName) {
    if (!skillRepository.existsByOwnerKeyAndNameIgnoringId(ownerKey, baseName, null)) {
      return baseName;
    }
    for (int suffix = 2; suffix <= 99; suffix++) {
      String candidate = baseName + " (" + suffix + ")";
      if (!skillRepository.existsByOwnerKeyAndNameIgnoringId(ownerKey, candidate, null)) {
        return candidate;
      }
    }
    return baseName + " (" + SkillId.generate().value().substring(0, 8) + ")";
  }
}
