package com.ai.metrics.controller.dto;

/** Request totals for domains without a dedicated inventory (vision, workflow). */
public record RequestsInventoryResponse(long requests, long errors)
    implements DomainInventoryResponse {}
