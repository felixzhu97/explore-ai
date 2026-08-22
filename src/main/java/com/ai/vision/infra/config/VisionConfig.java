package com.ai.vision.infra.config;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Documentation. */
@Configuration
@EnableConfigurationProperties(VisionModelProperties.class)
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-vision",
    havingValue = "true",
    matchIfMissing = true)
public class VisionConfig {

  static {
    configureJnaLibraryPath(Path.of("/opt/homebrew/lib"));
    configureJnaLibraryPath(Path.of("/usr/local/lib"));
  }

  @PostConstruct
  void configureNativeLibraries() {
    configureJnaLibraryPath(Path.of("/opt/homebrew/lib"));
    configureJnaLibraryPath(Path.of("/usr/local/lib"));
  }

  private static void configureJnaLibraryPath(Path libraryPath) {
    if (!Files.isDirectory(libraryPath)) {
      return;
    }
    String existing = System.getProperty("jna.library.path", "");
    String path = libraryPath.toString();
    if (existing.isBlank()) {
      System.setProperty("jna.library.path", path);
      return;
    }
    if (!existing.contains(path)) {
      System.setProperty("jna.library.path", existing + File.pathSeparator + path);
    }
  }
}
