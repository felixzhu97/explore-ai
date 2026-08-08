package com.ai.pipeline.web;

import com.ai.account.web.OwnerContext;
import com.ai.common.web.ClientIdentity;
import com.ai.pipeline.application.usecase.WorkflowTemplateUseCase;
import com.ai.pipeline.web.dto.CreateWorkflowTemplateFromTemplateRequest;
import com.ai.pipeline.web.dto.CreateWorkflowTemplateRequest;
import com.ai.pipeline.web.dto.SavedWorkflowTemplateResponse;
import com.ai.pipeline.web.dto.SetWorkflowTemplateEnabledRequest;
import com.ai.pipeline.web.dto.UpdateWorkflowTemplateRequest;
import com.ai.pipeline.web.dto.WorkflowTemplateResponse;
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
import java.util.Locale;

@RestController
@RequestMapping("/api/pipelines/templates")
public class WorkflowTemplateController {

    private final OwnerContext ownerContext;

    private final WorkflowTemplateUseCase workflowTemplateUseCase;

    public WorkflowTemplateController(WorkflowTemplateUseCase workflowTemplateUseCase, OwnerContext ownerContext) {
        this.ownerContext = ownerContext;
        this.workflowTemplateUseCase = workflowTemplateUseCase;
    }

    @GetMapping
    public List<WorkflowTemplateResponse> listTemplates(
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest request) {
        String language = resolveLanguage(lang, request);
        return workflowTemplateUseCase.listTemplates(language).stream()
                .map(WorkflowTemplateResponse::from)
                .toList();
    }

    @GetMapping("/library")
    public List<SavedWorkflowTemplateResponse> listLibrary(HttpServletRequest request) {
        String clientId = ownerContext.requireValue(request);
        return workflowTemplateUseCase.listLibrary(clientId).stream()
                .map(SavedWorkflowTemplateResponse::from)
                .toList();
    }

    @PostMapping("/from-template")
    public ResponseEntity<SavedWorkflowTemplateResponse> createFromTemplate(
            @Valid @RequestBody CreateWorkflowTemplateFromTemplateRequest body,
            @RequestParam(value = "lang", required = false) String lang,
            HttpServletRequest request) {
        String clientId = ownerContext.requireValue(request);
        String language = resolveLanguage(lang, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SavedWorkflowTemplateResponse.from(
                        workflowTemplateUseCase.createFromTemplate(clientId, body.templateId(), language)));
    }

    @PostMapping("/library")
    public ResponseEntity<SavedWorkflowTemplateResponse> create(
            @Valid @RequestBody CreateWorkflowTemplateRequest body,
            HttpServletRequest request) {
        String clientId = ownerContext.requireValue(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SavedWorkflowTemplateResponse.from(workflowTemplateUseCase.create(
                        clientId,
                        body.name(),
                        body.description(),
                        body.agentTypes(),
                        body.shortTopic(),
                        body.briefPrompt(),
                        null)));
    }

    @PutMapping("/library/{id}")
    public SavedWorkflowTemplateResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateWorkflowTemplateRequest body,
            HttpServletRequest request) {
        String clientId = ownerContext.requireValue(request);
        return SavedWorkflowTemplateResponse.from(workflowTemplateUseCase.update(
                clientId,
                id,
                body.name(),
                body.description(),
                body.agentTypes(),
                body.shortTopic(),
                body.briefPrompt()));
    }

    @PatchMapping("/library/{id}/enabled")
    public SavedWorkflowTemplateResponse setEnabled(
            @PathVariable String id,
            @Valid @RequestBody SetWorkflowTemplateEnabledRequest body,
            HttpServletRequest request) {
        String clientId = ownerContext.requireValue(request);
        return SavedWorkflowTemplateResponse.from(
                workflowTemplateUseCase.setEnabled(clientId, id, body.enabled()));
    }

    @DeleteMapping("/library/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
        String clientId = ownerContext.requireValue(request);
        workflowTemplateUseCase.delete(clientId, id);
        return ResponseEntity.noContent().build();
    }

    private static String resolveLanguage(String lang, HttpServletRequest request) {
        if (lang != null && !lang.isBlank()) {
            return lang;
        }
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return Locale.ENGLISH.getLanguage();
        }
        return acceptLanguage;
    }
}
