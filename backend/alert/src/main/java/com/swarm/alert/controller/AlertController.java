package com.swarm.alert.controller;

import com.swarm.alert.model.AlertDTO;
import com.swarm.alert.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "alert");
    }

    @PostMapping("/api/alert")
    public AlertDTO createAlert(@RequestBody AlertDTO alert) {
        return service.createAlert(alert);
    }

    @GetMapping("/alerts")
    public List<AlertDTO> getAlerts(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String droneId,
            @RequestParam(required = false) String severity) {
        return service.getAlerts(limit, droneId, severity);
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable String alertId) {
        boolean ok = service.acknowledgeAlert(alertId);
        if (ok) return ResponseEntity.ok(Map.of("status", "acknowledged"));
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/alerts/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }
}
