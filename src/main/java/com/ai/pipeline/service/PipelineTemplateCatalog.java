package com.ai.pipeline.service;

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

/** Built-in pipeline templates loaded per language from {@code pipeline-templates/*.json}. */
public final class PipelineTemplateCatalog {

  private static final String DEFAULT_LANGUAGE = "en";
  private static final List<String> SUPPORTED_LANGUAGES = List.of("en", "zh", "ja", "fr", "es");
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Map<String, List<PipelineTemplateDefinition>> BY_LANGUAGE = loadAll();

  private PipelineTemplateCatalog() {}

  public static List<PipelineTemplateDefinition> listAll() {
    return listAll(DEFAULT_LANGUAGE);
  }

  public static List<PipelineTemplateDefinition> listAll(String language) {
    return BY_LANGUAGE.getOrDefault(normalizeLanguage(language), BY_LANGUAGE.get(DEFAULT_LANGUAGE));
  }

  public static Optional<PipelineTemplateDefinition> findById(String templateId) {
    return findById(templateId, DEFAULT_LANGUAGE);
  }

  /** Finds a template by id in the given language, falling back to the English catalog. */
  public static Optional<PipelineTemplateDefinition> findById(String templateId, String language) {
    if (templateId == null || templateId.isBlank()) {
      return Optional.empty();
    }
    Optional<PipelineTemplateDefinition> localized =
        listAll(language).stream().filter(template -> template.id().equals(templateId)).findFirst();
    if (localized.isPresent()) {
      return localized;
    }
    return listAll(DEFAULT_LANGUAGE).stream()
        .filter(template -> template.id().equals(templateId))
        .findFirst();
  }

  /** Returns the template's localized names across all supported languages. */
  public static Set<String> namesForTemplate(String templateId) {
    Set<String> names = new LinkedHashSet<>();
    if (templateId == null || templateId.isBlank()) {
      return names;
    }
    for (List<PipelineTemplateDefinition> templates : BY_LANGUAGE.values()) {
      for (PipelineTemplateDefinition template : templates) {
        if (template.id().equals(templateId)) {
          names.add(template.name());
        }
      }
    }
    return names;
  }

  /** Reduces a locale or Accept-Language value to a supported language code, else English. */
  public static String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) {
      return DEFAULT_LANGUAGE;
    }
    String primary = language.trim().toLowerCase(Locale.ROOT).split("[,;\\s]")[0];
    primary = primary.split("[-_]")[0];
    return SUPPORTED_LANGUAGES.contains(primary) ? primary : DEFAULT_LANGUAGE;
  }

  private static Map<String, List<PipelineTemplateDefinition>> loadAll() {
    Map<String, List<PipelineTemplateDefinition>> loaded = new LinkedHashMap<>();
    for (String language : SUPPORTED_LANGUAGES) {
      loaded.put(language, loadLanguage(language));
    }
    if (!loaded.containsKey(DEFAULT_LANGUAGE) || loaded.get(DEFAULT_LANGUAGE).isEmpty()) {
      throw new IllegalStateException("Default pipeline templates (en) are missing");
    }
    return Map.copyOf(loaded);
  }

  private static List<PipelineTemplateDefinition> loadLanguage(String language) {
    String path = "pipeline-templates/" + language + ".json";
    try (InputStream input =
        PipelineTemplateCatalog.class.getClassLoader().getResourceAsStream(path)) {
      if (input == null) {
        return List.of();
      }
      List<PipelineTemplateDefinition> templates =
          MAPPER.readValue(input, new TypeReference<>() {});
      return List.copyOf(templates);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load pipeline templates: " + path, ex);
    }
  }
}
