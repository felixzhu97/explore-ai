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

  /** Documentation. */
  public static String load(String relativePath) {
    return ClasspathPromptLoader.load(relativePath);
  }

  /** Documentation. */
  public static String joinSections(String... sections) {
    return ClasspathPromptLoader.joinSections(sections);
  }

  /** Documentation. */
  public static String render(String templateText, Map<String, ?> variables) {
    return new PromptTemplate(templateText).render(toObjectMap(variables));
  }

  /** Documentation. */
  public static String loadAndRender(String relativePath, Map<String, ?> variables) {
    return render(load(relativePath), variables);
  }

  private static Map<String, Object> toObjectMap(Map<String, ?> variables) {
    Map<String, Object> objectMap = HashMap.newHashMap(variables.size());
    objectMap.putAll(variables);
    return objectMap;
  }
}
