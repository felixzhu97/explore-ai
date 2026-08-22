package com.ai.common.infra.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Documentation. */
@ConfigurationProperties(prefix = "app.pipeline")
public class AgentSkillsProperties {

  private Skills skills = new Skills();

  public Skills getSkills() {
    return skills;
  }

  public void setSkills(Skills skills) {
    this.skills = skills;
  }

  /** Documentation. */
  public static class Skills {
    private boolean enabled = false;
    private List<String> ids = new ArrayList<>();
    private String resourceLocation = "classpath:agent/skills/";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public List<String> getIds() {
      return ids;
    }

    public void setIds(List<String> ids) {
      this.ids = ids == null ? new ArrayList<>() : ids;
    }

    public String getResourceLocation() {
      return resourceLocation;
    }

    public void setResourceLocation(String resourceLocation) {
      this.resourceLocation = resourceLocation;
    }
  }
}
