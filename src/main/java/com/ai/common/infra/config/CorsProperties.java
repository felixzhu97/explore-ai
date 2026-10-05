package com.ai.common.infra.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties under {@code app.cors} listing allowed CORS origin patterns. */
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

  private List<String> allowedOriginPatterns =
      new ArrayList<>(List.of("http://localhost:4200", "http://localhost:3000"));
}
