package com.ai.common.infra.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties under {@code app.agent-skills}: enabled flag, ids and location. */
@ConfigurationProperties(prefix = "app.agent-skills")
@Getter
public class AgentSkillsProperties {

  @Setter private boolean enabled = false;
  private List<String> ids = new ArrayList<>();
  @Setter private String resourceLocation = "classpath:agent/skills/";

  /** Sets the enabled skill ids, treating null as empty. */
  public void setIds(List<String> ids) {
    this.ids = ids == null ? new ArrayList<>() : ids;
  }
}
