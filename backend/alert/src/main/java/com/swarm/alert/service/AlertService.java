package com.swarm.alert.service;

import com.swarm.alert.model.AlertDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ALERT SERVICE - Alert Management Logic
 * ========================================
 * Manages the lifecycle of alerts:
 *   1. Create alert (from Anomaly Service)
 *   2. List alerts (for Dashboard)
 *   3. Acknowledge alert (by operator)
 *   4. Get statistics (for monitoring)
 *
 * Storage:
 *   CopyOnWriteArrayList is used for thread-safe access.
 *   Maximum 500 alerts are kept in memory (oldest are removed).
 *
 * Prometheus metric:
 *   alerts_generated_total → Counter of total alerts created
 */
@Service
public class AlertService {

    private final MeterRegistry meterRegistry;

    // Thread-safe list of all alerts (most recent at the end)
    private final List<AlertDTO> alerts = new CopyOnWriteArrayList<>();

    // Prometheus counter for total alerts generated
    private Counter alertCounter;

    // Maximum alerts to keep in memory
    private static final int MAX_ALERTS = 500;

    public AlertService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        alertCounter = Counter.builder("alerts_generated_total")
                .description("Total alerts generated").register(meterRegistry);
    }

    /**
     * Create a new alert.
     * Called by: POST /api/alert (from Anomaly Detection Service)
     *
     * Steps:
     *   1. Generate unique ID if not provided
     *   2. Add to the alerts list
     *   3. Increment Prometheus counter
     *   4. Log the alert to console (visible in Docker logs)
     *   5. Trim old alerts if list exceeds MAX_ALERTS
     */
    public AlertDTO createAlert(AlertDTO alert) {
        // Generate unique ID if not set
        if (alert.getId() == null || alert.getId().isEmpty()) {
            alert.setId(UUID.randomUUID().toString().substring(0, 8));
        }

        alerts.add(alert);
        alertCounter.increment();

        // Log to console (visible via `docker compose logs alert`)
        System.out.printf("[ALERT] %s | %s | %s | %s%n",
                alert.getSeverity().toUpperCase(),
                alert.getDroneId(), alert.getType(), alert.getMessage());

        // Remove oldest alerts if we exceed the maximum
        while (alerts.size() > MAX_ALERTS) {
            alerts.remove(0);
        }

        return alert;
    }

    /**
     * List alerts with optional filters.
     * Called by: GET /alerts?limit=50&droneId=DR-001&severity=critical
     *
     * @param limit    Maximum number of alerts to return
     * @param droneId  Filter by drone (optional)
     * @param severity Filter by severity (optional)
     * @return Filtered and limited list of alerts
     */
    public List<AlertDTO> getAlerts(int limit, String droneId, String severity) {
        List<AlertDTO> filtered = new ArrayList<>(alerts);

        // Apply droneId filter if provided
        if (droneId != null && !droneId.isEmpty()) {
            filtered = filtered.stream()
                    .filter(a -> a.getDroneId().equals(droneId))
                    .collect(Collectors.toList());
        }

        // Apply severity filter if provided
        if (severity != null && !severity.isEmpty()) {
            filtered = filtered.stream()
                    .filter(a -> a.getSeverity().equalsIgnoreCase(severity))
                    .collect(Collectors.toList());
        }

        // Return only the last 'limit' entries
        int from = Math.max(0, filtered.size() - limit);
        return new ArrayList<>(filtered.subList(from, filtered.size()));
    }

    /**
     * Acknowledge an alert (mark as "seen" by operator).
     * Called by: POST /alerts/{alertId}/acknowledge
     *
     * @param alertId The ID of the alert to acknowledge
     * @return true if found and acknowledged, false if not found
     */
    public boolean acknowledgeAlert(String alertId) {
        for (AlertDTO a : alerts) {
            if (a.getId().equals(alertId)) {
                a.setAcknowledged(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Get alert statistics.
     * Used by: GET /alerts/stats
     *
     * Returns: total count, unacknowledged count, grouped by type and severity
     */
    public Map<String, Object> getStats() {
        Map<String, Long> byType = alerts.stream()
                .collect(Collectors.groupingBy(AlertDTO::getType, Collectors.counting()));
        Map<String, Long> bySev = alerts.stream()
                .collect(Collectors.groupingBy(AlertDTO::getSeverity, Collectors.counting()));
        long unack = alerts.stream().filter(a -> !a.isAcknowledged()).count();

        return Map.of(
                "total", alerts.size(),
                "unacknowledged", unack,
                "by_type", byType,
                "by_severity", bySev
        );
    }
}
