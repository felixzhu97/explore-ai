package com.ai.automation.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.automation.controller.dto.AutomationRunResponse;
import com.ai.automation.controller.dto.AutomationScheduleResponse;
import com.ai.automation.controller.dto.CreateAutomationScheduleRequest;
import com.ai.automation.controller.dto.SetAutomationEnabledRequest;
import com.ai.automation.controller.dto.UpdateAutomationScheduleRequest;
import com.ai.automation.service.AutomationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/automations/schedules")
@RequiredArgsConstructor
public class AutomationController {

  private final AutomationService automationService;
  private final OwnerContext ownerContext;

  /** Lists the owner's automation schedules. */
  @GetMapping
  public List<AutomationScheduleResponse> list(HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return automationService.list(ownerKey).stream().map(AutomationScheduleResponse::from).toList();
  }

  /** Lists recent runs of a schedule. */
  @GetMapping("/{id}/runs")
  public List<AutomationRunResponse> listRuns(
      @PathVariable String id,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return automationService.listRuns(ownerKey, id, limit).stream()
        .map(AutomationRunResponse::from)
        .toList();
  }

  /** Creates an automation schedule. */
  @PostMapping
  public ResponseEntity<AutomationScheduleResponse> create(
      @Valid @RequestBody CreateAutomationScheduleRequest body, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            AutomationScheduleResponse.from(
                automationService.create(
                    ownerKey,
                    body.name(),
                    body.scheduleKind(),
                    body.cronExpression(),
                    body.runAt(),
                    body.timezone(),
                    body.pipelineTemplateId(),
                    body.recipientEmail(),
                    body.brief())));
  }

  /** Updates an automation schedule. */
  @PutMapping("/{id}")
  public AutomationScheduleResponse update(
      @PathVariable String id,
      @Valid @RequestBody UpdateAutomationScheduleRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return AutomationScheduleResponse.from(
        automationService.update(
            ownerKey,
            id,
            body.name(),
            body.scheduleKind(),
            body.cronExpression(),
            body.runAt(),
            body.timezone(),
            body.pipelineTemplateId(),
            body.recipientEmail(),
            body.brief()));
  }

  /** Turns an automation schedule on or off. */
  @PatchMapping("/{id}/enabled")
  public AutomationScheduleResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetAutomationEnabledRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return AutomationScheduleResponse.from(
        automationService.setEnabled(ownerKey, id, body.enabled()));
  }

  /** Deletes an automation schedule. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    automationService.delete(ownerKey, id);
    return ResponseEntity.noContent().build();
  }
}
