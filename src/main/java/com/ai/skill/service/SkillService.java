package com.ai.skill.service;

import com.ai.common.domain.vo.DomainStrings;
import com.ai.skill.domain.exception.SkillNameConflictException;
import com.ai.skill.domain.exception.SkillNotFoundException;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Skill use case that enforces per-owner unique names and derives names from templates. */
@Service
@RequiredArgsConstructor
public class SkillService {

  private static final Logger log = LoggerFactory.getLogger(SkillService.class);

  private final SkillRepository skillRepository;

  /** Lists the owner's skills. */
  public List<Skill> list(String ownerKey) {
    return skillRepository.findAllByOwnerKey(ownerKey);
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
    List<Skill> skills = skillRepository.findEnabledByOwnerKeyAndIds(ownerKey, ids);
    if (skills.size() < ids.size()) {
      log.debug(
          "Ignored unknown or disabled skill ids: requested={}, resolved={}",
          ids.size(),
          skills.size());
    }
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
    skillRepository.deleteByIdAndOwnerKey(SkillId.of(id), ownerKey);
  }

  private Skill findOwnedSkill(String ownerKey, String id) {
    return skillRepository
        .findByIdAndOwnerKey(SkillId.of(id), ownerKey)
        .orElseThrow(() -> new SkillNotFoundException(id));
  }

  private void assertNameAvailable(String ownerKey, String name, SkillId excludeId) {
    String normalized = DomainStrings.normalizeName(name);
    if (skillRepository.existsByOwnerKeyAndNameIgnoringId(ownerKey, normalized, excludeId)) {
      throw new SkillNameConflictException(normalized);
    }
  }

  private String findNextAvailableName(String ownerKey, String baseName) {
    return DomainStrings.copyNameCandidates(baseName, DomainStrings.DEFAULT_NAME_MAX)
        .filter(name -> !skillRepository.existsByOwnerKeyAndNameIgnoringId(ownerKey, name, null))
        .findFirst()
        .orElseGet(
            () ->
                DomainStrings.copyName(
                    baseName,
                    SkillId.generate().value().substring(0, 8),
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
      } catch (IllegalArgumentException ignored) {
        log.debug("Ignoring invalid skill id");
      }
    }
    return parsed;
  }
}
