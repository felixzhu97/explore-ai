package com.ai.common.infra.skills;

import com.ai.common.domain.model.BundledSkill;
import com.ai.common.infra.config.AgentSkillsProperties;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** Holds loaded agent skills and exposes their tool callback and system-prompt catalog. */
@Component
public class AgentSkillsRuntime {
  private final boolean enabled;
  private final List<BundledSkill> skills;
  private final ToolCallback skillToolCallback;

  AgentSkillsRuntime(AgentSkillsProperties agentProperties, BundledSkillLoader skillLoader) {
    this.enabled = agentProperties.isEnabled();
    this.skills = skillLoader.loadEnabledSkills();
    this.skillToolCallback = buildSkillToolCallback(this.skills);
  }

  /** Tells whether skills are on and at least one is loaded. */
  public boolean isEnabled() {
    return enabled && !skills.isEmpty();
  }

  /** Returns the loaded skills. */
  public List<BundledSkill> getSkills() {
    return skills;
  }

  /** Returns the skill tool when skills are on. */
  public Optional<ToolCallback> findSkillToolCallback() {
    return (!isEnabled() || skillToolCallback == null)
        ? Optional.empty()
        : Optional.of(skillToolCallback);
  }

  /** Appends a catalog of available skills to the base prompt when skills are enabled. */
  public String augmentSystemPrompt(String basePrompt) {
    if (!isEnabled() || skills.isEmpty()) {
      return basePrompt;
    }
    String catalog =
        skills.stream()
            .map(skill -> "- " + skill.getName() + ": " + skill.getDescription())
            .collect(Collectors.joining("\n"));
    return basePrompt
        + "\n\n## Agent Skills\n"
        + "Use the Skill tool when a configured skill matches the user request.\n"
        + "Available skills:\n"
        + catalog;
  }

  private static ToolCallback buildSkillToolCallback(List<BundledSkill> skills) {
    if (skills.isEmpty()) {
      return null;
    }
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    SkillsTool.Builder builder = SkillsTool.builder();
    for (BundledSkill skill : skills) {
      try {
        builder.addSkillsResource(
            resourceLoader.getResource(toSpringResourceLocation(skill.getResourceLocation())));
      } catch (Exception expected) {
      }
    }
    try {
      return builder.build();
    } catch (Exception ex) {
      return null;
    }
  }

  private static String toSpringResourceLocation(String resourceUri) {
    if (resourceUri.startsWith("file:")) {
      return resourceUri;
    }
    int jarMarker = resourceUri.indexOf("!/");
    if (resourceUri.startsWith("jar:") && jarMarker > 0) {
      return resourceUri.substring(0, jarMarker + 2)
          + normalizeClasspathEntry(resourceUri.substring(jarMarker + 2));
    }
    return normalizeClasspathEntry(resourceUri);
  }

  private static String normalizeClasspathEntry(String entry) {
    while (entry.startsWith("/")) {
      entry = entry.substring(1);
    }
    return "classpath:" + entry;
  }
}
