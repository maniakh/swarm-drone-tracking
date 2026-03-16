package com.swarm.alert.controller;

import com.swarm.alert.model.AlertDTO;
import com.swarm.alert.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ALERT CONTROLLER - REST API Endpoints for Alert Management
 * ============================================================
 * Handles alert CRUD operations and provides data for the dashboard.
 *
 * Endpoints:
 *   GET  /health                        → Health check
 *   POST /api/alert                     → Create new alert (from Anomaly Service)
 *   GET  /alerts                        → List alerts (with optional filters)
 *   POST /alerts/{alertId}/acknowledge  → Mark alert as acknowledged
 *   GET  /alerts/stats                  → Get alert statistics
 */
@RestController
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    /** Health check endpoint */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "alert");
    }

    /**
     * Create a new alert.
     * Called automatically by the Anomaly Detection Service when an anomaly is found.
     * The anomaly data is converted to an alert and stored.
     *
     * Example: POST /api/alert with AlertDTO body
     */
    @PostMapping("/api/alert")
    public AlertDTO createAlert(@RequestBody AlertDTO alert) {
        return service.createAlert(alert);
    }

    /**
     * List alerts with optional filters.
     * Called by the dashboard every 2 seconds to refresh the alert panel.
     *
     * Query params:
     *   limit    → Max results (default: 50)
     *   droneId  → Filter by drone (optional)
     *   severity → Filter by severity (optional)
     *
     * Example: GET /alerts?limit=100&severity=critical
     */
    @GetMapping("/alerts")
    public List<AlertDTO> getAlerts(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String droneId,
            @RequestParam(required = false) String severity) {
        return service.getAlerts(limit, droneId, severity);
    }

    /**
     * Acknowledge an alert.
     * Called when an operator reviews and acknowledges an alert.
     * Returns 200 if successful, 404 if alert not found.
     *
     * Example: POST /alerts/a1b2c3d4/acknowledge
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable String alertId) {
        boolean ok = service.acknowledgeAlert(alertId);
        if (ok) return ResponseEntity.ok(Map.of("status", "acknowledged"));
        return ResponseEntity.notFound().build();
    }

    /**
     * Get alert statistics.
     * Returns total count, unacknowledged count, and breakdowns by type/severity.
     */
    @GetMapping("/alerts/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }
}
