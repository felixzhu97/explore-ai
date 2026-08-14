package com.ai.skill.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Documentation. */
public final class SkillTemplateCatalog {

  private static final String DEFAULT_LANGUAGE = "en";
  private static final List<String> SUPPORTED_LANGUAGES = List.of("en", "zh", "ja", "fr", "es");
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Map<String, List<SkillTemplate>> BY_LANGUAGE = loadAll();

  private SkillTemplateCatalog() {}

  /** Documentation. */
  public static List<SkillTemplate> listAll() {
    return listAll(DEFAULT_LANGUAGE);
  }

  /** Documentation. */
  public static List<SkillTemplate> listAll(String language) {
    return BY_LANGUAGE.getOrDefault(normalizeLanguage(language), BY_LANGUAGE.get(DEFAULT_LANGUAGE));
  }

  /** Documentation. */
  public static Optional<SkillTemplate> findById(String templateId) {
    return findById(templateId, DEFAULT_LANGUAGE);
  }

  /** Documentation. */
  public static Optional<SkillTemplate> findById(String templateId, String language) {
    if (templateId == null || templateId.isBlank()) {
      return Optional.empty();
    }
    Optional<SkillTemplate> localized =
        listAll(language).stream().filter(template -> template.id().equals(templateId)).findFirst();
    if (localized.isPresent()) {
      return localized;
    }
    return listAll(DEFAULT_LANGUAGE).stream()
        .filter(template -> template.id().equals(templateId))
        .findFirst();
  }

  /** Documentation. */
  public static Set<String> namesForTemplate(String templateId) {
    Set<String> names = new LinkedHashSet<>();
    if (templateId == null || templateId.isBlank()) {
      return names;
    }
    for (List<SkillTemplate> templates : BY_LANGUAGE.values()) {
      for (SkillTemplate template : templates) {
        if (template.id().equals(templateId)) {
          names.add(template.name());
        }
      }
    }
    return names;
  }

  /** Documentation. */
  public static String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) {
      return DEFAULT_LANGUAGE;
    }
    String primary = language.trim().toLowerCase(Locale.ROOT).split("[,;\\s]")[0];
    primary = primary.split("[-_]")[0];
    return SUPPORTED_LANGUAGES.contains(primary) ? primary : DEFAULT_LANGUAGE;
  }

  private static Map<String, List<SkillTemplate>> loadAll() {
    Map<String, List<SkillTemplate>> loaded = new LinkedHashMap<>();
    for (String language : SUPPORTED_LANGUAGES) {
      loaded.put(language, loadLanguage(language));
    }
    if (!loaded.containsKey(DEFAULT_LANGUAGE) || loaded.get(DEFAULT_LANGUAGE).isEmpty()) {
      throw new IllegalStateException("Default skill templates (en) are missing");
    }
    return Map.copyOf(loaded);
  }

  private static List<SkillTemplate> loadLanguage(String language) {
    String path = "skill-templates/" + language + ".json";
    try (InputStream input =
        SkillTemplateCatalog.class.getClassLoader().getResourceAsStream(path)) {
      if (input == null) {
        return List.of();
      }
      List<SkillTemplate> templates = MAPPER.readValue(input, new TypeReference<>() {});
      return List.copyOf(templates);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load skill templates: " + path, ex);
    }
  }
}
