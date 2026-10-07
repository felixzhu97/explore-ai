package com.ai.workflow.domain.model;

import lombok.Value;

/** Classification decision and specialized-route output from a routing workflow. */
@Value
public class RoutingResult {
  String selection;
  String reasoning;
  String output;
}
