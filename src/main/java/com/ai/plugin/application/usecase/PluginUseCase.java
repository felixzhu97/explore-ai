package com.ai.plugin.application.usecase;

import com.ai.plugin.domain.model.PluginInstallation;
import com.ai.plugin.domain.vo.PluginDefinition;

import java.util.List;

public interface PluginUseCase {

    List<PluginDefinition> listCatalog();

    List<PluginInstallation> listInstalled(String ownerKey);

    PluginInstallation install(
            String ownerKey,
            String definitionId,
            String endpoint,
            String authToken,
            String customName);

    PluginInstallation setEnabled(String ownerKey, String installationId, boolean enabled);

    void uninstall(String ownerKey, String installationId);

    List<String> listTools(String ownerKey, String installationId);

    PluginInstallation ensureBuiltinInstalled(String ownerKey);
}
