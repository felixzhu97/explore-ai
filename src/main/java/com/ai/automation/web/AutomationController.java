package com.ai.automation.web;

import com.ai.automation.application.usecase.AutomationUseCase;
import com.ai.automation.web.dto.AutomationRunResponse;
import com.ai.automation.web.dto.AutomationScheduleResponse;
import com.ai.automation.web.dto.CreateAutomationScheduleRequest;
import com.ai.automation.web.dto.SetAutomationEnabledRequest;
import com.ai.automation.web.dto.UpdateAutomationScheduleRequest;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/automations/schedules")
public class AutomationController {

    private final AutomationUseCase automationUseCase;

    public AutomationController(AutomationUseCase automationUseCase) {
        this.automationUseCase = automationUseCase;
    }

    @GetMapping
    public List<AutomationScheduleResponse> list(HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return automationUseCase.list(clientId).stream()
                .map(AutomationScheduleResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AutomationScheduleResponse> create(
            @Valid @RequestBody CreateAutomationScheduleRequest body,
            HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AutomationScheduleResponse.from(automationUseCase.create(
                        clientId,
                        body.name(),
                        body.cronExpression(),
                        body.timezone(),
                        body.workflowTemplateId(),
                        body.recipientEmail(),
                        body.brief())));
    }

    @PutMapping("/{id}")
    public AutomationScheduleResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateAutomationScheduleRequest body,
            HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return AutomationScheduleResponse.from(automationUseCase.update(
                clientId,
                id,
                body.name(),
                body.cronExpression(),
                body.timezone(),
                body.workflowTemplateId(),
                body.recipientEmail(),
                body.brief()));
    }

    @PatchMapping("/{id}/enabled")
    public AutomationScheduleResponse setEnabled(
            @PathVariable String id,
            @Valid @RequestBody SetAutomationEnabledRequest body,
            HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return AutomationScheduleResponse.from(
                automationUseCase.setEnabled(clientId, id, body.enabled()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        automationUseCase.delete(clientId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/runs")
    public List<AutomationRunResponse> listRuns(
            @PathVariable String id,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return automationUseCase.listRuns(clientId, id, limit).stream()
                .map(AutomationRunResponse::from)
                .toList();
    }
}
