package com.ai.metrics.controller.dto;

import java.util.List;

public record ToolsInventoryResponse(List<NamedCountResponse> topTools)
    implements CapabilityInventoryResponse {}
