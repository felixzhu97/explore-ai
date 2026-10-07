package com.ai.pipeline.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.pipeline.controller.dto.CreateCustomAgentRequest;
import com.ai.pipeline.controller.dto.CustomAgentResponse;
import com.ai.pipeline.controller.dto.SetCustomAgentEnabledRequest;
import com.ai.pipeline.controller.dto.UpdateCustomAgentRequest;
import com.ai.pipeline.service.CustomAgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pipelines/agents")
@RequiredArgsConstructor
public class CustomAgentController {

  private final CustomAgentService customAgentService;
  private final OwnerContext ownerContext;

  /** Lists the owner's custom agents. */
  @GetMapping
  public List<CustomAgentResponse> listLibrary(HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return customAgentService.listLibrary(ownerKey).stream()
        .map(CustomAgentResponse::createResponse)
        .toList();
  }

  /** Saves a new agent. */
  @PostMapping
  public ResponseEntity<CustomAgentResponse> createAgent(
      @Valid @RequestBody CreateCustomAgentRequest body, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            CustomAgentResponse.createResponse(
                customAgentService.createAgent(
                    ownerKey,
                    body.agentType(),
                    body.name(),
                    body.description(),
                    body.systemPrompt(),
                    body.tools())));
  }

  /** Updates a custom agent. */
  @PutMapping("/{id}")
  public CustomAgentResponse updateAgent(
      @PathVariable String id,
      @Valid @RequestBody UpdateCustomAgentRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return CustomAgentResponse.createResponse(
        customAgentService.updateAgent(
            ownerKey, id, body.name(), body.description(), body.systemPrompt(), body.tools()));
  }

  /** Turns a custom agent on or off. */
  @PatchMapping("/{id}/enabled")
  public CustomAgentResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetCustomAgentEnabledRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return CustomAgentResponse.createResponse(
        customAgentService.setEnabled(ownerKey, id, body.enabled()));
  }

  /** Deletes a custom agent. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAgent(@PathVariable String id, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    customAgentService.deleteAgent(ownerKey, id);
    return ResponseEntity.noContent().build();
  }
}
