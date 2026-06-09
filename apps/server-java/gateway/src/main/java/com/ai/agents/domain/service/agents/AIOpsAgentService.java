package com.ai.agents.domain.service.agents;

import com.ai.agents.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * AIOps Agent domain service.
 * Manages incident response, anomaly detection, and root cause analysis.
 */
@Service
public final class AIOpsAgentService {

    private final Map<String, Incident> incidents = new HashMap<>();

    /**
     * Detect anomaly based on metric and sensitivity.
     */
    public AnomalyResult detectAnomaly(String metric, String timeRange, double sensitivity) {
        double score = calculateAnomalyScore(metric);
        double threshold = 0.7 - (sensitivity * 0.4);
        boolean isAnomaly = score > threshold;

        String explanation = getAnomalyExplanation(score, threshold, isAnomaly);

        return new AnomalyResult(
                metric,
                score,
                threshold,
                sensitivity,
                timeRange,
                isAnomaly,
                explanation
        );
    }

    private double calculateAnomalyScore(String metric) {
        return 0.3 + (Math.abs(metric.hashCode()) % 50) / 100.0;
    }

    private String getAnomalyExplanation(double score, double threshold, boolean isAnomaly) {
        if (!isAnomaly) {
            return "Normal operation - no anomalies detected";
        }
        double deviation = (score - threshold) * 10;
        if (deviation > 3) return "Critical anomaly - immediate action needed";
        if (deviation > 2) return "Significant anomaly - immediate attention required";
        if (deviation > 1) return "Moderate anomaly - investigation recommended";
        return "Slight deviation detected - monitoring";
    }

    /**
     * Create a new incident.
     */
    public Incident createIncident(String title, String description, Incident.Severity severity, List<String> affectedSystems) {
        Incident incident = Incident.create(title, description, severity, affectedSystems);
        incidents.put(incident.idValue(), incident);
        return incident;
    }

    /**
     * Get incident by ID.
     */
    public Optional<Incident> getIncident(String incidentId) {
        return Optional.ofNullable(incidents.get(incidentId));
    }

    /**
     * List incidents with optional filters.
     */
    public List<Incident> listIncidents(Incident.IncidentStatus status, Incident.Severity severity) {
        return incidents.values().stream()
                .filter(i -> status == null || i.status() == status)
                .filter(i -> severity == null || i.severity() == severity)
                .toList();
    }

    /**
     * Update incident status.
     */
    public Incident updateIncidentStatus(String incidentId, Incident.IncidentStatus newStatus, String message) {
        Incident incident = incidents.get(incidentId);
        if (incident == null) {
            throw new IllegalArgumentException("Incident not found: " + incidentId);
        }
        Incident updated = incident.updateStatus(newStatus, message);
        incidents.put(incidentId, updated);
        return updated;
    }

    /**
     * Perform root cause analysis.
     */
    public RootCauseResult analyzeRootCause(String incidentId, List<String> affectedServices) {
        Incident incident = incidents.get(incidentId);

        String rootCause = determineRootCause(incidentId);
        double confidence = 0.75 + (Math.abs(incidentId.hashCode()) % 25) / 100.0;

        List<String> contributingFactors = List.of(
                "High memory usage detected before incident",
                "Connection pool at maximum capacity",
                "Multiple retries from upstream services",
                "Recent configuration change"
        );

        List<String> recommendations = List.of(
                "Scale up database connection pool",
                "Implement circuit breaker pattern",
                "Add horizontal pod autoscaling",
                "Review and optimize query performance"
        );

        return new RootCauseResult(
                incidentId,
                rootCause,
                confidence,
                contributingFactors,
                recommendations
        );
    }

    private String determineRootCause(String incidentId) {
        String[] rootCauses = {
                "Database connection pool exhaustion",
                "Memory leak in application service",
                "Network latency spike due to infrastructure issue",
                "Configuration change caused cascade failure",
                "Increased traffic beyond capacity"
        };
        return rootCauses[Math.abs(incidentId.hashCode()) % rootCauses.length];
    }

    /**
     * Get system health overview.
     */
    public SystemHealthResult getSystemHealth() {
        Map<String, String> services = Map.of(
                "api-gateway", "healthy",
                "auth-service", "healthy",
                "user-service", "healthy",
                "order-service", "degraded",
                "payment-service", "healthy",
                "notification-service", "healthy"
        );

        long healthy = services.values().stream().filter("healthy"::equals).count();
        long degraded = services.values().stream().filter("degraded"::equals).count();

        return new SystemHealthResult(
                degraded == 0 ? "healthy" : "degraded",
                (int) healthy,
                (int) degraded,
                services
        );
    }

    public record AnomalyResult(
            String metric,
            double score,
            double threshold,
            double sensitivity,
            String timeRange,
            boolean isAnomaly,
            String explanation
    ) {
        public boolean isAnomaly() { return isAnomaly; }
    }

    public record RootCauseResult(
            String incidentId,
            String rootCause,
            double confidence,
            List<String> contributingFactors,
            List<String> recommendations
    ) {}

    public record SystemHealthResult(
            String overallStatus,
            int healthyCount,
            int degradedCount,
            Map<String, String> services
    ) {}
}
