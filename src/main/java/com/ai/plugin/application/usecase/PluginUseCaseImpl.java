package com.ai.plugin.application.usecase;

import com.ai.plugin.domain.exception.PluginAlreadyInstalledException;
import com.ai.plugin.domain.exception.PluginNotFoundException;
import com.ai.plugin.domain.model.PluginInstallation;
import com.ai.plugin.domain.repository.PluginCatalogRepository;
import com.ai.plugin.domain.repository.PluginInstallationRepository;
import com.ai.plugin.domain.repository.PluginToolGateway;
import com.ai.plugin.domain.vo.PluginDefinition;
import com.ai.plugin.domain.vo.PluginHealthStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PluginUseCaseImpl implements PluginUseCase {

    private final PluginCatalogRepository catalogRepository;
    private final PluginInstallationRepository installationRepository;
    private final PluginToolGateway toolGateway;

    public PluginUseCaseImpl(
            PluginCatalogRepository catalogRepository,
            PluginInstallationRepository installationRepository,
            PluginToolGateway toolGateway) {
        this.catalogRepository = catalogRepository;
        this.installationRepository = installationRepository;
        this.toolGateway = toolGateway;
    }

    @Override
    public List<PluginDefinition> listCatalog() {
        return catalogRepository.findAll();
    }

    @Override
    public List<PluginInstallation> listInstalled(String ownerKey) {
        ensureBuiltinInstalled(ownerKey);
        return installationRepository.findAllByOwnerKey(ownerKey);
    }

    @Override
    public PluginInstallation install(
            String ownerKey,
            String definitionId,
            String endpoint,
            String authToken,
            String customName) {
        PluginDefinition definition = catalogRepository.findById(definitionId)
                .orElseThrow(() -> new PluginNotFoundException("Plugin not found: " + definitionId));

        if (!definition.builtin() && !PluginInstallation.CUSTOM_DEFINITION_ID.equals(definitionId)) {
            installationRepository.findByOwnerKeyAndDefinitionId(ownerKey, definitionId).ifPresent(existing -> {
                throw new PluginAlreadyInstalledException("Plugin already installed: " + definition.name());
            });
        }

        if (definition.builtin()) {
            return ensureBuiltinInstalled(ownerKey);
        }

        String resolvedEndpoint = requireEndpoint(endpoint, definition);
        if (PluginInstallation.CUSTOM_DEFINITION_ID.equals(definitionId)) {
            boolean duplicate = installationRepository.findAllByOwnerKey(ownerKey).stream()
                    .anyMatch(row -> row.isCustom()
                            && resolvedEndpoint.equalsIgnoreCase(nullToEmpty(row.getEndpoint())));
            if (duplicate) {
                throw new PluginAlreadyInstalledException("Custom Plugin already installed for this endpoint");
            }
        }

        String displayName = customName != null && !customName.isBlank()
                ? customName.trim()
                : definition.name();
        PluginInstallation installation = PluginInstallation.create(
                ownerKey,
                definition.id(),
                displayName,
                resolvedEndpoint,
                authToken);
        if (toolGateway.pingRemote(resolvedEndpoint, authToken)) {
            installation.markHealth(PluginHealthStatus.HEALTHY);
        } else {
            installation.markHealth(PluginHealthStatus.UNHEALTHY);
        }
        return installationRepository.save(installation);
    }

    @Override
    public PluginInstallation setEnabled(String ownerKey, String installationId, boolean enabled) {
        PluginInstallation installation = requireInstallation(ownerKey, installationId);
        installation.setEnabled(enabled);
        return installationRepository.save(installation);
    }

    @Override
    public void uninstall(String ownerKey, String installationId) {
        PluginInstallation installation = requireInstallation(ownerKey, installationId);
        if (installation.isBuiltin()) {
            installation.disable();
            installationRepository.save(installation);
            return;
        }
        installationRepository.deleteByIdAndOwnerKey(installationId, ownerKey);
    }

    @Override
    public List<String> listTools(String ownerKey, String installationId) {
        PluginInstallation installation = requireInstallation(ownerKey, installationId);
        if (installation.isBuiltin()) {
            return List.of("get_weather", "get_forecast", "search_knowledge_base", "list_documents");
        }
        if (installation.getEndpoint() == null || installation.getEndpoint().isBlank()) {
            return List.of();
        }
        try {
            List<String> names = toolGateway.listRemoteToolNames(
                    installation.getEndpoint(),
                    installation.getAuthToken());
            installation.markHealth(PluginHealthStatus.HEALTHY);
            installationRepository.save(installation);
            return names;
        } catch (RuntimeException ex) {
            installation.markHealth(PluginHealthStatus.UNHEALTHY);
            installationRepository.save(installation);
            return List.of();
        }
    }

    @Override
    public PluginInstallation ensureBuiltinInstalled(String ownerKey) {
        return installationRepository.findByOwnerKeyAndDefinitionId(
                        ownerKey,
                        PluginInstallation.BUILTIN_DEFINITION_ID)
                .orElseGet(() -> {
                    PluginDefinition builtin = catalogRepository
                            .findById(PluginInstallation.BUILTIN_DEFINITION_ID)
                            .orElseThrow(() -> new PluginNotFoundException("Built-in Plugin missing from catalog"));
                    PluginInstallation created = PluginInstallation.create(
                            ownerKey,
                            builtin.id(),
                            builtin.name(),
                            null,
                            null);
                    created.markHealth(PluginHealthStatus.HEALTHY);
                    return installationRepository.save(created);
                });
    }

    private PluginInstallation requireInstallation(String ownerKey, String installationId) {
        return installationRepository.findByIdAndOwnerKey(installationId, ownerKey)
                .orElseThrow(() -> new PluginNotFoundException("Installation not found: " + installationId));
    }

    private static String requireEndpoint(String endpoint, PluginDefinition definition) {
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint.trim();
        }
        throw new IllegalArgumentException(
                "Streamable HTTP endpoint is required to install " + definition.name());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
