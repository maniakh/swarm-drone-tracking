package com.swarm.anomaly.controller;

import com.swarm.anomaly.model.AnomalyDTO;
import com.swarm.anomaly.model.TelemetryDTO;
import com.swarm.anomaly.service.AnomalyDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class AnomalyController {

    private final AnomalyDetectionService service;

    public AnomalyController(AnomalyDetectionService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "anomaly-detection");
    }

    @PostMapping("/api/anomaly/analyze")
    public Map<String, Object> analyze(@RequestBody TelemetryDTO data) {
        List<AnomalyDTO> anomalies = service.analyze(data);
        return Map.of(
                "droneId", data.getDroneId(),
                "anomalyCount", anomalies.size(),
                "anomalies", anomalies
        );
    }

    @GetMapping("/anomalies")
    public List<AnomalyDTO> getAnomalies(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String droneId) {
        return service.getAnomalies(limit, droneId);
    }

    @GetMapping("/anomalies/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }

    @GetMapping("/anomalies/config")
    public Map<String, Object> getConfig() {
        return service.getConfig();
    }
}
