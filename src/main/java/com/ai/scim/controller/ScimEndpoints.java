package com.ai.scim.controller;

import com.ai.scim.service.ScimLocations;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** SCIM endpoint paths and absolute URL helpers. */
final class ScimEndpoints {

  static final String BASE = "/scim/v2";
  static final String USERS = BASE + "/Users";

  private ScimEndpoints() {}

  static ScimLocations currentLocations() {
    return new ScimLocations(
        ServletUriComponentsBuilder.fromCurrentContextPath().path(USERS).toUriString());
  }
}
