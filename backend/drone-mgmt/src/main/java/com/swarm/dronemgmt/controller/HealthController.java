package com.swarm.dronemgmt.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ROOT HEALTH CONTROLLER
 * ========================
 * Provides a health check at the root level (/health).
 * This is separate from the DroneController's health endpoint (/api/drones/health)
 * because Docker healthcheck and monitoring tools usually check the root path.
 *
 * Example: curl http://localhost:8002/health
 * Response: {"status": "healthy", "service": "drone-management"}
 */
@RestController
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "service", "drone-management");
    }
}
