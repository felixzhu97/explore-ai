package com.ai.pipeline.web;

import com.ai.account.web.OwnerContext;
import com.ai.pipeline.application.usecase.AgentDefinitionUseCase;
import com.ai.pipeline.web.dto.CreateSavedAgentRequest;
import com.ai.pipeline.web.dto.SavedAgentResponse;
import com.ai.pipeline.web.dto.SetSavedAgentEnabledRequest;
import com.ai.pipeline.web.dto.UpdateSavedAgentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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

/** Documentation. */
@RestController
@RequestMapping("/api/pipelines/agents/library")
public class PipelineAgentLibraryController {

  private final OwnerContext ownerContext;

  private final AgentDefinitionUseCase agentDefinitionUseCase;

  /** Documentation. */
  public PipelineAgentLibraryController(
      AgentDefinitionUseCase agentDefinitionUseCase, OwnerContext ownerContext) {
    this.ownerContext = ownerContext;
    this.agentDefinitionUseCase = agentDefinitionUseCase;
  }

  /** Documentation. */
  @GetMapping
  public List<SavedAgentResponse> listLibrary(HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return agentDefinitionUseCase.listLibrary(clientId).stream()
        .map(SavedAgentResponse::from)
        .toList();
  }

  /** Documentation. */
  @PostMapping
  public ResponseEntity<SavedAgentResponse> create(
      @Valid @RequestBody CreateSavedAgentRequest body, HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SavedAgentResponse.from(
                agentDefinitionUseCase.create(
                    clientId,
                    body.typeKey(),
                    body.name(),
                    body.description(),
                    body.systemPrompt(),
                    body.toolKeys())));
  }

  /** Documentation. */
  @PutMapping("/{id}")
  public SavedAgentResponse update(
      @PathVariable String id,
      @Valid @RequestBody UpdateSavedAgentRequest body,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return SavedAgentResponse.from(
        agentDefinitionUseCase.update(
            clientId, id, body.name(), body.description(), body.systemPrompt(), body.toolKeys()));
  }

  /** Documentation. */
  @PatchMapping("/{id}/enabled")
  public SavedAgentResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetSavedAgentEnabledRequest body,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return SavedAgentResponse.from(agentDefinitionUseCase.setEnabled(clientId, id, body.enabled()));
  }

  /** Documentation. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    agentDefinitionUseCase.delete(clientId, id);
    return ResponseEntity.noContent().build();
  }
}
