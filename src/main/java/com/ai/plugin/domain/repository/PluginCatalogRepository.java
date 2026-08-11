package com.ai.plugin.domain.repository;

import com.ai.plugin.domain.vo.PluginDefinition;

import java.util.List;
import java.util.Optional;

public interface PluginCatalogRepository {

    List<PluginDefinition> findAll();

    Optional<PluginDefinition> findById(String definitionId);
}
