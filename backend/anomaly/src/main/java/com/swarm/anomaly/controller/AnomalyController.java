package com.swarm.anomaly.controller;

import com.swarm.anomaly.model.AnomalyDTO;
import com.swarm.anomaly.model.TelemetryDTO;
import com.swarm.anomaly.service.AnomalyDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ANOMALY CONTROLLER - REST API Endpoints for Anomaly Detection
 * ===============================================================
 * Exposes endpoints for:
 *   1. Analyzing telemetry data (called by Telemetry Service)
 *   2. Querying detected anomalies (called by Dashboard)
 *   3. Viewing statistics and configuration
 *
 * Endpoints:
 *   GET  /health            → Health check
 *   POST /api/anomaly/analyze → Analyze telemetry data (main endpoint)
 *   GET  /anomalies          → List recent anomalies (with optional filters)
 *   GET  /anomalies/stats    → Get anomaly statistics (used by dashboard Statistics view)
 *   GET  /anomalies/config   → View current detection thresholds
 */
@RestController
@CrossOrigin(origins = "*")
public class AnomalyController {

    private final AnomalyDetectionService service;

    public AnomalyController(AnomalyDetectionService service) {
        this.service = service;
    }

    /** Health check endpoint */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "anomaly-detection");
    }

    /**
     * MAIN ANALYSIS ENDPOINT
     * Called by Telemetry Service every 3 seconds for each drone.
     * Receives telemetry data, runs all detection rules, returns any anomalies found.
     *
     * Request:  POST /api/anomaly/analyze with TelemetryDTO body
     * Response: { "droneId": "DR-001", "anomalyCount": 1, "anomalies": [...] }
     */
    @PostMapping("/api/anomaly/analyze")
    public Map<String, Object> analyze(@RequestBody TelemetryDTO data) {
        List<AnomalyDTO> anomalies = service.analyze(data);
        return Map.of(
                "droneId", data.getDroneId(),
                "anomalyCount", anomalies.size(),
                "anomalies", anomalies
        );
    }

    /**
     * List recent anomalies.
     * Called by the dashboard to display the alert panel.
     *
     * Query params:
     *   limit   → Max number of anomalies to return (default: 50)
     *   droneId → Filter by specific drone (optional)
     *
     * Example: GET /anomalies?limit=20&droneId=DR-001
     */
    @GetMapping("/anomalies")
    public List<AnomalyDTO> getAnomalies(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String droneId) {
        return service.getAnomalies(limit, droneId);
    }

    /**
     * Get anomaly statistics (total, by type, by severity, by drone).
     * Used by the dashboard's "Statistics" view for charts.
     */
    @GetMapping("/anomalies/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }

    /**
     * View current detection configuration/thresholds.
     * Useful for debugging: "What are the current limits?"
     */
    @GetMapping("/anomalies/config")
    public Map<String, Object> getConfig() {
        return service.getConfig();
    }
}
