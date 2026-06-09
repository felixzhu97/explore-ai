package com.ai.agents.domain.service.agents;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Monitoring Agent domain service.
 * Manages Prometheus/Grafana metrics and alerting.
 */
@Service
public final class MonitoringAgentService {

    private final Map<String, AlertRule> alertRules = new HashMap<>();

    /**
     * Query metrics with aggregation.
     */
    public MetricQueryResult queryMetrics(String metric, String timeRange, String aggregation) {
        double value = calculateMetricValue(metric);
        double avg = value;
        double min = value * 0.8;
        double max = value * 1.2;

        return new MetricQueryResult(
                metric,
                value,
                avg,
                min,
                max,
                aggregation,
                timeRange,
                Instant.now()
        );
    }

    private double calculateMetricValue(String metric) {
        return 50.0 + (Math.abs(metric.hashCode()) % 100);
    }

    /**
     * Get metric time series.
     */
    public TimeSeriesResult getTimeSeries(String metric, String timeRange, int points) {
        List<Double> values = new ArrayList<>();
        Random random = new Random(metric.hashCode());
        for (int i = 0; i < points; i++) {
            values.add(50.0 + random.nextDouble() * 100);
        }

        return new TimeSeriesResult(
                metric,
                timeRange,
                values,
                List.of("timestamp", "value"),
                Instant.now()
        );
    }

    /**
     * Create an alert rule.
     */
    public AlertRule createAlert(String name, String metric, String condition, double threshold) {
        AlertRule rule = new AlertRule(
                name,
                metric,
                condition,
                threshold,
                "active",
                Instant.now()
        );
        alertRules.put(name, rule);
        return rule;
    }

    /**
     * Get alert rule.
     */
    public Optional<AlertRule> getAlert(String name) {
        return Optional.ofNullable(alertRules.get(name));
    }

    /**
     * List alert rules.
     */
    public List<AlertRule> listAlerts(String status) {
        return alertRules.values().stream()
                .filter(a -> status == null || status.equals(a.status()))
                .toList();
    }

    /**
     * Fire an alert.
     */
    public AlertRule fireAlert(String name, String message) {
        AlertRule rule = alertRules.get(name);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + name);
        }
        AlertRule fired = new AlertRule(
                rule.name(),
                rule.metric(),
                rule.condition(),
                rule.threshold(),
                "firing",
                rule.createdAt()
        );
        alertRules.put(name, fired);
        return fired;
    }

    /**
     * Resolve an alert.
     */
    public AlertRule resolveAlert(String name, String message) {
        AlertRule rule = alertRules.get(name);
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule not found: " + name);
        }
        AlertRule resolved = new AlertRule(
                rule.name(),
                rule.metric(),
                rule.condition(),
                rule.threshold(),
                "resolved",
                rule.createdAt()
        );
        alertRules.put(name, resolved);
        return resolved;
    }

    public record MetricQueryResult(
            String metric,
            double value,
            double avg,
            double min,
            double max,
            String aggregation,
            String timeRange,
            Instant timestamp
    ) {}

    public record TimeSeriesResult(
            String metric,
            String timeRange,
            List<Double> values,
            List<String> labels,
            Instant timestamp
    ) {}

    public record AlertRule(
            String name,
            String metric,
            String condition,
            double threshold,
            String status,
            Instant createdAt
    ) {
        public boolean isActive() { return "active".equals(status) || "firing".equals(status); }
    }
}
