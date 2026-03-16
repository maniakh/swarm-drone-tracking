package com.swarm.telemetry.controller;

import com.swarm.telemetry.model.TelemetryDTO;
import com.swarm.telemetry.service.TelemetryStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class TelemetryController {

    private final TelemetryStore store;

    public TelemetryController(TelemetryStore store) {
        this.store = store;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "telemetry");
    }

    @PostMapping("/telemetry")
    public Map<String, Object> receiveTelemetry(@RequestBody TelemetryDTO data) {
        store.store(data);
        return Map.of("status", "ok", "message", "Telemetry received");
    }

    @GetMapping("/telemetry/latest")
    public Collection<TelemetryDTO> getLatest() {
        return store.getLatestAll();
    }

    @GetMapping("/telemetry/{droneId}")
    public ResponseEntity<List<TelemetryDTO>> getDroneTelemetry(
            @PathVariable String droneId,
            @RequestParam(defaultValue = "20") int limit) {
        List<TelemetryDTO> data = store.getHistory(droneId, limit);
        if (data.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/telemetry/{droneId}/latest")
    public ResponseEntity<TelemetryDTO> getDroneLatest(@PathVariable String droneId) {
        TelemetryDTO data = store.getLatest(droneId);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }
}
