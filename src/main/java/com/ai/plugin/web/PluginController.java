package com.ai.plugin.web;

import com.ai.account.web.OwnerContext;
import com.ai.plugin.application.usecase.PluginUseCase;
import com.ai.plugin.web.dto.InstallPluginRequest;
import com.ai.plugin.web.dto.PluginDefinitionResponse;
import com.ai.plugin.web.dto.PluginInstallationResponse;
import com.ai.plugin.web.dto.SetPluginEnabledRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final PluginUseCase pluginUseCase;
    private final OwnerContext ownerContext;

    public PluginController(PluginUseCase pluginUseCase, OwnerContext ownerContext) {
        this.pluginUseCase = pluginUseCase;
        this.ownerContext = ownerContext;
    }

    @GetMapping("/catalog")
    public List<PluginDefinitionResponse> catalog() {
        return pluginUseCase.listCatalog().stream()
                .map(PluginDefinitionResponse::from)
                .toList();
    }

    @GetMapping("/installed")
    public List<PluginInstallationResponse> installed(HttpServletRequest request) {
        String ownerKey = ownerContext.requireValue(request);
        return pluginUseCase.listInstalled(ownerKey).stream()
                .map(PluginInstallationResponse::from)
                .toList();
    }

    @PostMapping("/install")
    public ResponseEntity<PluginInstallationResponse> install(
            @Valid @RequestBody InstallPluginRequest body,
            HttpServletRequest request) {
        String ownerKey = ownerContext.requireValue(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PluginInstallationResponse.from(pluginUseCase.install(
                        ownerKey,
                        body.definitionId(),
                        body.endpoint(),
                        body.authToken(),
                        body.customName())));
    }

    @PatchMapping("/installed/{id}")
    public PluginInstallationResponse setEnabled(
            @PathVariable String id,
            @Valid @RequestBody SetPluginEnabledRequest body,
            HttpServletRequest request) {
        String ownerKey = ownerContext.requireValue(request);
        return PluginInstallationResponse.from(
                pluginUseCase.setEnabled(ownerKey, id, Boolean.TRUE.equals(body.enabled())));
    }

    @DeleteMapping("/installed/{id}")
    public ResponseEntity<Void> uninstall(@PathVariable String id, HttpServletRequest request) {
        String ownerKey = ownerContext.requireValue(request);
        pluginUseCase.uninstall(ownerKey, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/installed/{id}/tools")
    public Map<String, List<String>> tools(@PathVariable String id, HttpServletRequest request) {
        String ownerKey = ownerContext.requireValue(request);
        return Map.of("tools", pluginUseCase.listTools(ownerKey, id));
    }
}
