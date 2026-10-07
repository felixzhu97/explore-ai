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
  private static final Map<String, List<BuiltinPipelineTemplate>> BY_LANGUAGE = loadAll();

  private PipelineTemplateCatalog() {}

  /** Lists the pipeline templates in English. */
  public static List<BuiltinPipelineTemplate> listAll() {
    return listAll(DEFAULT_LANGUAGE);
  }

  /** Lists the pipeline templates in the language. */
  public static List<BuiltinPipelineTemplate> listAll(String language) {
    return BY_LANGUAGE.getOrDefault(normalizeLanguage(language), BY_LANGUAGE.get(DEFAULT_LANGUAGE));
  }

  /** Finds an English pipeline template by id. */
  public static Optional<BuiltinPipelineTemplate> findById(String templateId) {
    return findById(templateId, DEFAULT_LANGUAGE);
  }

  /** Finds a template by id in the given language, falling back to the English catalog. */
  public static Optional<BuiltinPipelineTemplate> findById(String templateId, String language) {
    if (templateId == null || templateId.isBlank()) {
      return Optional.empty();
    }
    Optional<BuiltinPipelineTemplate> localized =
        listAll(language).stream().filter(template -> template.id().equals(templateId)).findFirst();
    if (localized.isPresent()) {
      return localized;
    }
    return listAll(DEFAULT_LANGUAGE).stream()
        .filter(template -> template.id().equals(templateId))
        .findFirst();
  }

  /** Returns the template's localized names across all supported languages. */
  public static Set<String> listNamesForTemplate(String templateId) {
    Set<String> names = new LinkedHashSet<>();
    if (templateId == null || templateId.isBlank()) {
      return names;
    }
    for (List<BuiltinPipelineTemplate> templates : BY_LANGUAGE.values()) {
      for (BuiltinPipelineTemplate template : templates) {
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

  private static Map<String, List<BuiltinPipelineTemplate>> loadAll() {
    Map<String, List<BuiltinPipelineTemplate>> loaded = new LinkedHashMap<>();
    for (String language : SUPPORTED_LANGUAGES) {
      loaded.put(language, loadLanguage(language));
    }
    if (!loaded.containsKey(DEFAULT_LANGUAGE) || loaded.get(DEFAULT_LANGUAGE).isEmpty()) {
      throw new IllegalStateException("Default pipeline templates (en) are missing");
    }
    return Map.copyOf(loaded);
  }

  private static List<BuiltinPipelineTemplate> loadLanguage(String language) {
    String path = "pipeline-templates/" + language + ".json";
    try (InputStream input =
        PipelineTemplateCatalog.class.getClassLoader().getResourceAsStream(path)) {
      if (input == null) {
        return List.of();
      }
      List<BuiltinPipelineTemplate> templates = MAPPER.readValue(input, new TypeReference<>() {});
      return List.copyOf(templates);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load pipeline templates: " + path, ex);
    }
  }
}
