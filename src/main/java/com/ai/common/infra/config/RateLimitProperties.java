package com.ai.common.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Rate limit settings: enabled flag and maximum requests per time window. */
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

  private boolean enabled = true;
  private int requestsPerWindow = 60;
  private int windowSeconds = 60;
}
