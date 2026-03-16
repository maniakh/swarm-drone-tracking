package com.swarm.anomaly.service;

import com.swarm.anomaly.model.AnomalyDTO;
import com.swarm.anomaly.model.TelemetryDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ANOMALY DETECTION SERVICE - Rule-Based Anomaly Detection Engine
 * =================================================================
 * This is the "brain" of the anomaly detection system.
 * It receives telemetry data and checks it against predefined rules/thresholds.
 *
 * Detection rules (configurable via application.yml or environment variables):
 *   1. SPEED CHECK        → Is the drone faster than maxSpeed (40 m/s)?
 *   2. BATTERY CHECK      → Is the battery below minBattery (15%)?
 *   3. ALTITUDE CHECK     → Is altitude above maxAltitude (350m) or below minAltitude (5m)?
 *   4. GEO-FENCE CHECK    → Is the drone outside the allowed geographical area?
 *
 * Severity levels:
 *   critical → Immediate action needed (e.g., battery < 5%, geo-fence violation)
 *   high     → Serious issue (e.g., battery < 15%, speed > 60 m/s)
 *   medium   → Warning (e.g., speed > 40 m/s, altitude too low)
 *
 * Data flow:
 *   Telemetry Service → POST /api/anomaly/analyze → [this service checks rules] → POST /api/alert → Alert Service
 *
 * Prometheus metrics:
 *   - analyses_total           : Total number of analyses performed
 *   - anomalies_detected_total : Number of anomalies detected (tagged by type)
 */
@Service
public class AnomalyDetectionService {

    // ─── Configurable thresholds (injected from application.yml) ───
    @Value("${thresholds.max-speed:40}")
    private double maxSpeed;             // Maximum allowed speed in m/s

    @Value("${thresholds.min-battery:15}")
    private double minBattery;           // Minimum safe battery percentage

    @Value("${thresholds.max-altitude:350}")
    private double maxAltitude;          // Maximum allowed altitude in meters

    @Value("${thresholds.min-altitude:5}")
    private double minAltitude;          // Minimum safe altitude in meters

    // ─── Geo-fence boundaries (Istanbul area) ───
    @Value("${thresholds.geo-fence.lat-min:40.8}")
    private double geoLatMin;            // Minimum latitude (south boundary)

    @Value("${thresholds.geo-fence.lat-max:41.3}")
    private double geoLatMax;            // Maximum latitude (north boundary)

    @Value("${thresholds.geo-fence.lon-min:28.7}")
    private double geoLonMin;            // Minimum longitude (west boundary)

    @Value("${thresholds.geo-fence.lon-max:29.3}")
    private double geoLonMax;            // Maximum longitude (east boundary)

    @Value("${alert.service-url:http://alert:8004}")
    private String alertServiceUrl;      // URL of the Alert Service

    // ─── Dependencies ───
    private final MeterRegistry meterRegistry;   // Prometheus metrics
    private final WebClient webClient;           // HTTP client for calling Alert Service

    // ─── Internal state ───
    private final List<AnomalyDTO> anomalyHistory = new CopyOnWriteArrayList<>();  // Thread-safe list
    private final Map<String, Counter> typeCounters = new ConcurrentHashMap<>();   // Counters per anomaly type
    private Counter analysesCounter;             // Total analyses counter

    private static final int MAX_HISTORY = 500;  // Max anomalies to keep in memory

    public AnomalyDetectionService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.webClient = WebClient.create();
    }

    @PostConstruct
    public void init() {
        // Register Prometheus counter for total analyses performed
        analysesCounter = Counter.builder("analyses_total")
                .description("Total analyses performed").register(meterRegistry);
    }

    /**
     * MAIN ANALYSIS METHOD - Checks telemetry data against all rules
     * ================================================================
     * Called by: POST /api/anomaly/analyze (from Telemetry Service)
     *
     * @param data The telemetry data to analyze
     * @return List of detected anomalies (empty if no violations)
     */
    public List<AnomalyDTO> analyze(TelemetryDTO data) {
        analysesCounter.increment();
        List<AnomalyDTO> anomalies = new ArrayList<>();
        String ts = Instant.now().toString();

        // ──────────────────────────────────────────────────────
        // RULE 1: SPEED CHECK
        // If speed > 40 m/s → anomaly detected
        // Severity: "high" if > 60 m/s (1.5x limit), otherwise "medium"
        // ──────────────────────────────────────────────────────
        if (data.getSpeed() > maxSpeed) {
            String sev = data.getSpeed() > maxSpeed * 1.5 ? "high" : "medium";
            anomalies.add(new AnomalyDTO(data.getDroneId(), "SPEED_EXCEEDED", sev,
                    String.format("Drone %s speed limit exceeded: %.1f m/s (limit: %.0f)",
                            data.getDroneId(), data.getSpeed(), maxSpeed),
                    data.getSpeed(), maxSpeed, ts));
        }

        // ──────────────────────────────────────────────────────
        // RULE 2: BATTERY CHECK
        // If battery < 15% → anomaly detected
        // Severity: "critical" if < 5%, otherwise "high"
        // ──────────────────────────────────────────────────────
        if (data.getBattery() < minBattery) {
            String sev = data.getBattery() < 5 ? "critical" : "high";
            anomalies.add(new AnomalyDTO(data.getDroneId(), "LOW_BATTERY", sev,
                    String.format("Drone %s low battery: %.1f%% (minimum: %.0f%%)",
                            data.getDroneId(), data.getBattery(), minBattery),
                    data.getBattery(), minBattery, ts));
        }

        // ──────────────────────────────────────────────────────
        // RULE 3: ALTITUDE CHECK
        // If altitude > 350m → "ALTITUDE_EXCEEDED" (high severity)
        // If altitude < 5m   → "ALTITUDE_TOO_LOW" (medium severity)
        // ──────────────────────────────────────────────────────
        if (data.getAltitude() > maxAltitude) {
            anomalies.add(new AnomalyDTO(data.getDroneId(), "ALTITUDE_EXCEEDED", "high",
                    String.format("Drone %s altitude limit exceeded: %.1fm (limit: %.0fm)",
                            data.getDroneId(), data.getAltitude(), maxAltitude),
                    data.getAltitude(), maxAltitude, ts));
        } else if (data.getAltitude() < minAltitude) {
            anomalies.add(new AnomalyDTO(data.getDroneId(), "ALTITUDE_TOO_LOW", "medium",
                    String.format("Drone %s flying too low: %.1fm (minimum: %.0fm)",
                            data.getDroneId(), data.getAltitude(), minAltitude),
                    data.getAltitude(), minAltitude, ts));
        }

        // ──────────────────────────────────────────────────────
        // RULE 4: GEO-FENCE CHECK
        // If drone position is outside the allowed area → "critical" severity
        // Geo-fence: Istanbul area (lat: 40.8-41.3, lon: 28.7-29.3)
        // ──────────────────────────────────────────────────────
        if (data.getLat() < geoLatMin || data.getLat() > geoLatMax ||
            data.getLon() < geoLonMin || data.getLon() > geoLonMax) {
            anomalies.add(new AnomalyDTO(data.getDroneId(), "GEO_FENCE_VIOLATION", "critical",
                    String.format("Drone %s geo-fence violation! Position: (%.4f, %.4f)",
                            data.getDroneId(), data.getLat(), data.getLon()),
                    data.getLat(), geoLatMax, ts));
        }

        // ── Store anomalies and forward to Alert Service ──
        for (AnomalyDTO a : anomalies) {
            anomalyHistory.add(a);
            getOrCreateCounter(a.getType()).increment();  // Increment Prometheus counter

            // Forward each anomaly to the Alert Service (fire-and-forget)
            try {
                webClient.post()
                        .uri(alertServiceUrl + "/api/alert")
                        .bodyValue(a)
                        .retrieve().toBodilessEntity()
                        .subscribe(r -> {}, err -> {});
            } catch (Exception ignored) {}
        }

        // Trim history to prevent memory overflow
        while (anomalyHistory.size() > MAX_HISTORY) {
            anomalyHistory.remove(0);
        }

        return anomalies;
    }

    /**
     * Get or create a Prometheus counter for a specific anomaly type.
     * This creates counters like: anomalies_detected_total{type="SPEED_EXCEEDED"}
     */
    private Counter getOrCreateCounter(String type) {
        return typeCounters.computeIfAbsent(type, t ->
                Counter.builder("anomalies_detected_total")
                        .tag("type", t)
                        .description("Anomalies detected by type")
                        .register(meterRegistry));
    }

    /**
     * Get recent anomalies, optionally filtered by droneId.
     * Used by: GET /anomalies?limit=50&droneId=DR-001
     */
    public List<AnomalyDTO> getAnomalies(int limit, String droneId) {
        List<AnomalyDTO> filtered = anomalyHistory;
        if (droneId != null && !droneId.isEmpty()) {
            filtered = filtered.stream()
                    .filter(a -> a.getDroneId().equals(droneId))
                    .collect(Collectors.toList());
        }
        int from = Math.max(0, filtered.size() - limit);
        return new ArrayList<>(filtered.subList(from, filtered.size()));
    }

    /**
     * Get anomaly statistics (total count, grouped by type/severity/drone).
     * Used by: GET /anomalies/stats (called by dashboard for the Statistics view)
     */
    public Map<String, Object> getStats() {
        Map<String, Long> byType = anomalyHistory.stream()
                .collect(Collectors.groupingBy(AnomalyDTO::getType, Collectors.counting()));
        Map<String, Long> bySev = anomalyHistory.stream()
                .collect(Collectors.groupingBy(AnomalyDTO::getSeverity, Collectors.counting()));
        Map<String, Long> byDrone = anomalyHistory.stream()
                .collect(Collectors.groupingBy(AnomalyDTO::getDroneId, Collectors.counting()));

        return Map.of(
                "total", anomalyHistory.size(),
                "by_type", byType,
                "by_severity", bySev,
                "by_drone", byDrone
        );
    }

    /**
     * Get current configuration (thresholds).
     * Used by: GET /anomalies/config (for debugging/monitoring)
     */
    public Map<String, Object> getConfig() {
        return Map.of(
                "max_speed", maxSpeed, "min_battery", minBattery,
                "max_altitude", maxAltitude, "min_altitude", minAltitude,
                "geo_fence", Map.of("lat_min", geoLatMin, "lat_max", geoLatMax,
                        "lon_min", geoLonMin, "lon_max", geoLonMax)
        );
    }
}
