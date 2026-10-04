package com.ai.common.infra.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties under {@code app.agent-skills}: enabled flag, ids and location. */
@ConfigurationProperties(prefix = "app.agent-skills")
public class AgentSkillsProperties {

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
