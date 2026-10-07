package com.ai.skill.service;

import com.ai.common.domain.model.DomainStrings;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.model.SkillId;
import com.ai.skill.domain.repository.SkillRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Skill use case that enforces per-owner unique names and derives names from templates. */
@Service
@Transactional
@RequiredArgsConstructor
public class SkillService {

  private final SkillRepository skillRepository;

  /** Lists the owner's skills. */
  public List<Skill> list(String ownerKey) {
    return skillRepository.findAllByOwnerKeyOrderByNameAsc(OwnerKey.parse(ownerKey));
  }

  /**
   * Builds the "Active Skills" system prompt from the owner's enabled skills among {@code
   * skillIds}; unknown, invalid or disabled ids are ignored.
   */
  public Optional<String> activeSkillsPrompt(String ownerKey, List<String> skillIds) {
    List<SkillId> ids = parseSkillIds(skillIds);
    if (ids.isEmpty()) {
      return Optional.empty();
    }
    List<Skill> skills = skillRepository.findEnabledByOwnerKeyAndIds(OwnerKey.parse(ownerKey), ids);
    return Optional.ofNullable(SkillSystemPromptBuilder.build(skills));
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
    skill.changeEnabled(enabled);
    return skillRepository.save(skill);
  }

  /** Deletes the owner's skill. */
  public void delete(String ownerKey, String id) {
    findOwnedSkill(ownerKey, id);
    skillRepository.deleteByIdAndOwnerKey(SkillId.of(id), OwnerKey.parse(ownerKey));
  }

  private Skill findOwnedSkill(String ownerKey, String id) {
    return skillRepository
        .findByIdAndOwnerKey(SkillId.of(id), OwnerKey.parse(ownerKey))
        .orElseThrow(() -> DomainException.notFound("SKILL_NOT_FOUND", "Skill not found: " + id));
  }

  private void assertNameAvailable(String ownerKey, String name, SkillId excludeId) {
    String normalized = DomainStrings.normalizeName(name);
    if (skillRepository.existsByOwnerKeyAndNameIgnoringId(
        OwnerKey.parse(ownerKey), normalized, excludeId)) {
      throw DomainException.conflict(
          "SKILL_NAME_CONFLICT", "Skill name already exists: " + normalized);
    }
  }

  private String findNextAvailableName(String ownerKey, String baseName) {
    OwnerKey owner = OwnerKey.parse(ownerKey);
    return DomainStrings.copyNameCandidates(baseName, DomainStrings.DEFAULT_NAME_MAX)
        .filter(name -> !skillRepository.existsByOwnerKeyAndNameIgnoringId(owner, name, null))
        .findFirst()
        .orElseGet(
            () ->
                DomainStrings.copyName(
                    baseName,
                    SkillId.generate().toString().substring(0, 8),
                    DomainStrings.DEFAULT_NAME_MAX));
  }

  private static List<SkillId> parseSkillIds(List<String> skillIds) {
    List<SkillId> parsed = new ArrayList<>();
    if (skillIds == null) {
      return parsed;
    }
    for (String skillId : skillIds) {
      if (skillId == null || skillId.isBlank()) {
        continue;
      }
      try {
        parsed.add(SkillId.of(skillId.trim()));
      } catch (IllegalArgumentException expected) {
      }
    }
    return parsed;
  }
}
