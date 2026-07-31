package com.ai.mcp.web;

import com.ai.mcp.infrastructure.server.McpServerCapabilityCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for MCP Server management endpoints.
 */
@RestController
@RequestMapping("/api/mcp")
@Tag(name = "MCP Server", description = "MCP Server management")
public class McpController {

    private final McpServerCapabilityCatalog capabilityCatalog;

    public McpController(McpServerCapabilityCatalog capabilityCatalog) {
        this.capabilityCatalog = capabilityCatalog;
    }

    @GetMapping("/health")
    @Operation(summary = "MCP Server health check")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "server", McpServerCapabilityCatalog.SERVER_NAME,
                "version", McpServerCapabilityCatalog.SERVER_VERSION,
                "protocol", "MCP 1.0"
        ));
    }

    @GetMapping("/info")
    @Operation(summary = "Get MCP Server information")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(capabilityCatalog.infoPayload());
    }
}
