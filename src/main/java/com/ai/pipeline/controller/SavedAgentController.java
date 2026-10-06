package com.ai.pipeline.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.pipeline.controller.dto.CreateSavedAgentRequest;
import com.ai.pipeline.controller.dto.SavedAgentResponse;
import com.ai.pipeline.controller.dto.SetSavedAgentEnabledRequest;
import com.ai.pipeline.controller.dto.UpdateSavedAgentRequest;
import com.ai.pipeline.service.SavedAgentService;
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
public class SavedAgentController {

  private final SavedAgentService savedAgentService;
  private final OwnerContext ownerContext;

  /** Lists the owner's saved agents. */
  @GetMapping
  public List<SavedAgentResponse> listLibrary(HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return savedAgentService.listLibrary(ownerKey).stream().map(SavedAgentResponse::from).toList();
  }

  /** Saves a new agent. */
  @PostMapping
  public ResponseEntity<SavedAgentResponse> create(
      @Valid @RequestBody CreateSavedAgentRequest body, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SavedAgentResponse.from(
                savedAgentService.create(
                    ownerKey,
                    body.typeKey(),
                    body.name(),
                    body.description(),
                    body.systemPrompt(),
                    body.toolKeys())));
  }

  /** Updates a saved agent. */
  @PutMapping("/{id}")
  public SavedAgentResponse update(
      @PathVariable String id,
      @Valid @RequestBody UpdateSavedAgentRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return SavedAgentResponse.from(
        savedAgentService.update(
            ownerKey, id, body.name(), body.description(), body.systemPrompt(), body.toolKeys()));
  }

  /** Turns a saved agent on or off. */
  @PatchMapping("/{id}/enabled")
  public SavedAgentResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetSavedAgentEnabledRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return SavedAgentResponse.from(savedAgentService.setEnabled(ownerKey, id, body.enabled()));
  }

  /** Deletes a saved agent. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    savedAgentService.delete(ownerKey, id);
    return ResponseEntity.noContent().build();
  }
}
