package com.ai.common.infra.prompt;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Thin wrapper over Spring AI {@link PromptTemplate} for classpath prompt resources. Static
 * fragments that contain JSON braces (for example A2UI examples) are loaded and composed via {@link
 * #load} / {@link #joinSections} without rendering.
 */
public final class ClasspathPromptTemplate {

  private ClasspathPromptTemplate() {}

  /** Loads a prompt file from the classpath. */
  public static String load(String relativePath) {
    return ClasspathPromptLoader.load(relativePath);
  }

  /** Joins prompt sections with blank lines. */
  public static String joinSections(String... sections) {
    return ClasspathPromptLoader.joinSections(sections);
  }

  /** Fills the template with the variables. */
  public static String render(String templateText, Map<String, ?> variables) {
    return new PromptTemplate(templateText).render(toObjectMap(variables));
  }

  /** Loads a prompt file and fills it with the variables. */
  public static String loadAndRender(String relativePath, Map<String, ?> variables) {
    return render(load(relativePath), variables);
  }

  private static Map<String, Object> toObjectMap(Map<String, ?> variables) {
    Map<String, Object> objectMap = HashMap.newHashMap(variables.size());
    objectMap.putAll(variables);
    return objectMap;
  }
}
