package com.ai.metrics.controller.dto;

/** Request totals for capabilities without a dedicated inventory (vision, workflow). */
public record RequestsInventoryResponse(long requests, long errors)
    implements CapabilityInventoryResponse {}
