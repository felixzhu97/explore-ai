package com.ai.agents.domain.service.agents;

import com.ai.agents.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * K8s Agent domain service.
 * Manages Kubernetes operations with domain logic.
 */
@Service
public final class K8sAgentService {

    private static final Set<String> ALLOWED_SUBCOMMANDS = Set.of(
            "get", "describe", "logs", "top", "events", "explain",
            "api-resources", "api-versions", "cluster-info"
    );

    private static final Set<String> ALLOWED_RESOURCE_TYPES = Set.of(
            "pods", "pod", "po", "services", "service", "svc",
            "deployments", "deployment", "deploy", "replicasets", "replicaset",
            "namespaces", "namespace", "ns", "nodes", "node", "no",
            "events", "ev", "endpoints", "configmaps", "secrets",
            "ingresses", "persistentvolumeclaims", "hpa", "cronjobs", "jobs"
    );

    private static final List<String> DANGEROUS_PATTERNS = List.of(
            "&&", "||", ";", "|", ">", ">>", "<", "$(", "`",
            "exec", "attach", "cp", "port-forward", "proxy",
            "rollout undo", "delete", "apply -f", "replace"
    );

    /**
     * Validate kubectl arguments for security.
     */
    public ValidationResult validateCommand(String command) {
        if (command == null || command.isBlank()) {
            return ValidationResult.invalid("Command cannot be empty");
        }

        String lower = command.toLowerCase();
        for (String pattern : DANGEROUS_PATTERNS) {
            if (lower.contains(pattern.toLowerCase())) {
                return ValidationResult.invalid("Dangerous pattern not allowed: " + pattern);
            }
        }

        String[] parts = command.trim().split("\\s+");
        if (parts.length > 0 && !ALLOWED_SUBCOMMANDS.contains(parts[0])) {
            return ValidationResult.invalid("Subcommand not allowed: " + parts[0]);
        }

        return ValidationResult.valid();
    }

    /**
     * Validate resource type.
     */
    public ValidationResult validateResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return ValidationResult.invalid("Resource type cannot be empty");
        }

        String normalized = resourceType.toLowerCase().replaceAll("s+$", "");
        if (!ALLOWED_RESOURCE_TYPES.contains(normalized) && !ALLOWED_RESOURCE_TYPES.contains(resourceType.toLowerCase())) {
            return ValidationResult.invalid("Resource type not allowed: " + resourceType);
        }

        return ValidationResult.valid();
    }

    /**
     * Build kubectl command arguments.
     */
    public List<String> buildArgs(String subcommand, String resource, String name, String namespace) {
        List<String> args = new ArrayList<>();
        args.add(subcommand);

        if (resource != null && !resource.isBlank()) {
            args.add(resource);
        }

        if (name != null && !name.isBlank()) {
            args.add(name);
        }

        if (namespace != null && !namespace.isBlank() && !"default".equals(namespace)) {
            args.add("-n");
            args.add(namespace);
        }

        return args;
    }

    /**
     * Determine output format based on operation.
     */
    public String getOutputFormat(String subcommand, String resource) {
        if ("get".equals(subcommand)) {
            if ("nodes".equalsIgnoreCase(resource) || "node".equalsIgnoreCase(resource)) {
                return "wide";
            }
            return "json";
        }
        if ("describe".equals(subcommand)) {
            return "yaml";
        }
        return "text";
    }

    public record ValidationResult(boolean isValid, String error) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        public static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }
    }
}
