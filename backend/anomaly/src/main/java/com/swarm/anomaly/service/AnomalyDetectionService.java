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

@Service
public class AnomalyDetectionService {

    @Value("${thresholds.max-speed:40}")
    private double maxSpeed;
    @Value("${thresholds.min-battery:15}")
    private double minBattery;
    @Value("${thresholds.max-altitude:350}")
    private double maxAltitude;
    @Value("${thresholds.min-altitude:5}")
    private double minAltitude;
    @Value("${thresholds.geo-fence.lat-min:40.8}")
    private double geoLatMin;
    @Value("${thresholds.geo-fence.lat-max:41.3}")
    private double geoLatMax;
    @Value("${thresholds.geo-fence.lon-min:28.7}")
    private double geoLonMin;
    @Value("${thresholds.geo-fence.lon-max:29.3}")
    private double geoLonMax;
    @Value("${alert.service-url:http://alert:8004}")
    private String alertServiceUrl;

    private final MeterRegistry meterRegistry;
    private final WebClient webClient;
    private final List<AnomalyDTO> anomalyHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Counter> typeCounters = new ConcurrentHashMap<>();
    private Counter analysesCounter;

    private static final int MAX_HISTORY = 500;

    public AnomalyDetectionService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.webClient = WebClient.create();
    }

    @PostConstruct
    public void init() {
        analysesCounter = Counter.builder("analyses_total")
                .description("Total analyses performed").register(meterRegistry);
    }

    public List<AnomalyDTO> analyze(TelemetryDTO data) {
        analysesCounter.increment();
        List<AnomalyDTO> anomalies = new ArrayList<>();
        String ts = Instant.now().toString();

        // 1. Speed check
        if (data.getSpeed() > maxSpeed) {
            String sev = data.getSpeed() > maxSpeed * 1.5 ? "high" : "medium";
            anomalies.add(new AnomalyDTO(data.getDroneId(), "SPEED_EXCEEDED", sev,
                    String.format("Drone %s speed limit exceeded: %.1f m/s (limit: %.0f)",
                            data.getDroneId(), data.getSpeed(), maxSpeed),
                    data.getSpeed(), maxSpeed, ts));
        }

        // 2. Battery check
        if (data.getBattery() < minBattery) {
            String sev = data.getBattery() < 5 ? "critical" : "high";
            anomalies.add(new AnomalyDTO(data.getDroneId(), "LOW_BATTERY", sev,
                    String.format("Drone %s low battery: %.1f%% (minimum: %.0f%%)",
                            data.getDroneId(), data.getBattery(), minBattery),
                    data.getBattery(), minBattery, ts));
        }

        // 3. Altitude check
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

        // 4. Geo-fence check
        if (data.getLat() < geoLatMin || data.getLat() > geoLatMax ||
            data.getLon() < geoLonMin || data.getLon() > geoLonMax) {
            anomalies.add(new AnomalyDTO(data.getDroneId(), "GEO_FENCE_VIOLATION", "critical",
                    String.format("Drone %s geo-fence violation! Position: (%.4f, %.4f)",
                            data.getDroneId(), data.getLat(), data.getLon()),
                    data.getLat(), geoLatMax, ts));
        }

        // Store & send alerts
        for (AnomalyDTO a : anomalies) {
            anomalyHistory.add(a);
            getOrCreateCounter(a.getType()).increment();

            // Forward to alert service
            try {
                webClient.post()
                        .uri(alertServiceUrl + "/api/alert")
                        .bodyValue(a)
                        .retrieve().toBodilessEntity()
                        .subscribe(r -> {}, err -> {});
            } catch (Exception ignored) {}
        }

        // Trim history
        while (anomalyHistory.size() > MAX_HISTORY) {
            anomalyHistory.remove(0);
        }

        return anomalies;
    }

    private Counter getOrCreateCounter(String type) {
        return typeCounters.computeIfAbsent(type, t ->
                Counter.builder("anomalies_detected_total")
                        .tag("type", t)
                        .description("Anomalies detected by type")
                        .register(meterRegistry));
    }

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

    public Map<String, Object> getConfig() {
        return Map.of(
                "max_speed", maxSpeed, "min_battery", minBattery,
                "max_altitude", maxAltitude, "min_altitude", minAltitude,
                "geo_fence", Map.of("lat_min", geoLatMin, "lat_max", geoLatMax,
                        "lon_min", geoLonMin, "lon_max", geoLonMax)
        );
    }
}
