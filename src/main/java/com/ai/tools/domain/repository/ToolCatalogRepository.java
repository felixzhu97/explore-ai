package com.ai.tools.domain.repository;

import com.ai.tools.domain.vo.ToolCatalogEntry;

import java.util.List;

public interface ToolCatalogRepository {

    List<ToolCatalogEntry> listCatalog();
}
