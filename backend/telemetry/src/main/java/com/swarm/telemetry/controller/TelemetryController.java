package com.swarm.telemetry.controller;

import com.swarm.telemetry.model.TelemetryDTO;
import com.swarm.telemetry.service.TelemetryStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * TELEMETRY CONTROLLER - REST API Endpoints
 * ===========================================
 * Exposes HTTP endpoints for telemetry data access.
 * This is what the dashboard and other services call to get drone data.
 *
 * Endpoints:
 *   GET  /health                    → Health check (used by Docker & monitoring)
 *   POST /telemetry                 → Receive telemetry data (external input)
 *   GET  /telemetry/latest          → Get latest position of ALL drones (used by dashboard)
 *   GET  /telemetry/{droneId}       → Get telemetry history for a specific drone
 *   GET  /telemetry/{droneId}/latest → Get latest data for a specific drone
 *
 * Annotations:
 *   @RestController  → Combines @Controller + @ResponseBody (returns JSON directly)
 *   @CrossOrigin     → Allows requests from any origin (needed for browser → API calls)
 *   @GetMapping      → Maps HTTP GET requests to a method
 *   @PostMapping     → Maps HTTP POST requests to a method
 *   @PathVariable    → Extracts value from URL path (e.g., /telemetry/{droneId})
 *   @RequestParam    → Extracts value from query string (e.g., ?limit=20)
 *   @RequestBody     → Deserializes JSON request body to Java object
 */
@RestController
@CrossOrigin(origins = "*")
public class TelemetryController {

    private final TelemetryStore store;

    // Constructor injection - Spring provides the TelemetryStore automatically
    public TelemetryController(TelemetryStore store) {
        this.store = store;
    }

    /**
     * Health check endpoint.
     * Used by Docker healthcheck and monitoring tools to verify the service is alive.
     * Example: curl http://localhost:8001/health
     * Response: {"status": "healthy", "service": "telemetry"}
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "telemetry");
    }

    /**
     * Receive telemetry data from an external source.
     * In production, real drones would POST their data here.
     * In our case, the TelemetrySimulator generates data internally,
     * but this endpoint allows manual testing via curl/Postman.
     *
     * Example: curl -X POST http://localhost:8001/telemetry -H "Content-Type: application/json" -d '{"droneId":"DR-001",...}'
     */
    @PostMapping("/telemetry")
    public Map<String, Object> receiveTelemetry(@RequestBody TelemetryDTO data) {
        store.store(data);
        return Map.of("status", "ok", "message", "Telemetry received");
    }

    /**
     * Get the latest telemetry for ALL drones.
     * This is the MAIN endpoint used by the dashboard - called every 2 seconds.
     * Returns the most recent data point for each active drone.
     *
     * Example: curl http://localhost:8001/telemetry/latest
     * Response: [{"droneId":"DR-001","lat":41.01,"lon":28.97,...}, ...]
     */
    @GetMapping("/telemetry/latest")
    public Collection<TelemetryDTO> getLatest() {
        return store.getLatestAll();
    }

    /**
     * Get telemetry history for a specific drone.
     * Useful for drawing flight paths or analyzing past behavior.
     *
     * Example: curl http://localhost:8001/telemetry/DR-001?limit=20
     * Returns 404 if drone not found.
     */
    @GetMapping("/telemetry/{droneId}")
    public ResponseEntity<List<TelemetryDTO>> getDroneTelemetry(
            @PathVariable String droneId,
            @RequestParam(defaultValue = "20") int limit) {
        List<TelemetryDTO> data = store.getHistory(droneId, limit);
        if (data.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    /**
     * Get the latest telemetry for a specific drone.
     * Example: curl http://localhost:8001/telemetry/DR-001/latest
     */
    @GetMapping("/telemetry/{droneId}/latest")
    public ResponseEntity<TelemetryDTO> getDroneLatest(@PathVariable String droneId) {
        TelemetryDTO data = store.getLatest(droneId);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }
}
