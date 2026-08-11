package com.ai.plugin.domain.repository;

import com.ai.plugin.domain.model.PluginInstallation;

import java.util.List;
import java.util.Optional;

public interface PluginInstallationRepository {

    PluginInstallation save(PluginInstallation installation);

    Optional<PluginInstallation> findByIdAndOwnerKey(String id, String ownerKey);

    List<PluginInstallation> findAllByOwnerKey(String ownerKey);

    Optional<PluginInstallation> findByOwnerKeyAndDefinitionId(String ownerKey, String definitionId);

    void deleteByIdAndOwnerKey(String id, String ownerKey);
}
