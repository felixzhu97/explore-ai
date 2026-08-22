package com.ai.automation.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.automation.controller.dto.AutomationRunResponse;
import com.ai.automation.controller.dto.AutomationScheduleResponse;
import com.ai.automation.controller.dto.CreateAutomationScheduleRequest;
import com.ai.automation.controller.dto.SetAutomationEnabledRequest;
import com.ai.automation.controller.dto.UpdateAutomationScheduleRequest;
import com.ai.automation.domain.vo.ScheduleKind;
import com.ai.automation.service.usecase.AutomationUseCase;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Documentation. */
@RestController
@RequestMapping("/api/automations/schedules")
public class AutomationController {

  private final OwnerContext ownerContext;

  private final AutomationUseCase automationUseCase;

  /** Documentation. */
  public AutomationController(AutomationUseCase automationUseCase, OwnerContext ownerContext) {
    this.ownerContext = ownerContext;
    this.automationUseCase = automationUseCase;
  }

  /** Documentation. */
  @GetMapping
  public List<AutomationScheduleResponse> list(HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return automationUseCase.list(clientId).stream().map(AutomationScheduleResponse::from).toList();
  }

  /** Documentation. */
  @PostMapping
  public ResponseEntity<AutomationScheduleResponse> create(
      @Valid @RequestBody CreateAutomationScheduleRequest body, HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            AutomationScheduleResponse.from(
                automationUseCase.create(
                    clientId,
                    body.name(),
                    ScheduleKind.from(body.scheduleKind()),
                    body.cronExpression(),
                    body.runAt(),
                    body.timezone(),
                    body.workflowTemplateId(),
                    body.recipientEmail(),
                    body.brief())));
  }

  /** Documentation. */
  @PutMapping("/{id}")
  public AutomationScheduleResponse update(
      @PathVariable String id,
      @Valid @RequestBody UpdateAutomationScheduleRequest body,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return AutomationScheduleResponse.from(
        automationUseCase.update(
            clientId,
            id,
            body.name(),
            ScheduleKind.from(body.scheduleKind()),
            body.cronExpression(),
            body.runAt(),
            body.timezone(),
            body.workflowTemplateId(),
            body.recipientEmail(),
            body.brief()));
  }

  /** Documentation. */
  @PatchMapping("/{id}/enabled")
  public AutomationScheduleResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetAutomationEnabledRequest body,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return AutomationScheduleResponse.from(
        automationUseCase.setEnabled(clientId, id, body.enabled()));
  }

  /** Documentation. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    automationUseCase.delete(clientId, id);
    return ResponseEntity.noContent().build();
  }

  /** Documentation. */
  @GetMapping("/{id}/runs")
  public List<AutomationRunResponse> listRuns(
      @PathVariable String id,
      @RequestParam(value = "limit", defaultValue = "20") int limit,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return automationUseCase.listRuns(clientId, id, limit).stream()
        .map(AutomationRunResponse::from)
        .toList();
  }
}
