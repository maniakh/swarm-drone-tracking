package com.swarm.dronemgmt.controller;

import com.swarm.dronemgmt.model.Drone;
import com.swarm.dronemgmt.service.DroneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DRONE CONTROLLER - REST API for Drone Management (CRUD)
 * =========================================================
 * Full CRUD REST API for managing drone metadata.
 * Follows RESTful conventions:
 *
 *   GET    /api/drones              → List all drones
 *   GET    /api/drones/{droneId}    → Get specific drone
 *   POST   /api/drones              → Register new drone
 *   PUT    /api/drones/{droneId}    → Update drone info
 *   DELETE /api/drones/{droneId}    → Remove drone
 *   GET    /api/drones/status/{s}   → Filter by status
 *   GET    /api/drones/team/{t}     → Filter by team
 *   GET    /api/drones/health       → Health check
 *
 * @RequestMapping("/api/drones") → All endpoints start with /api/drones
 */
@RestController
@RequestMapping("/api/drones")
@CrossOrigin(origins = "*")
public class DroneController {

    @Autowired
    private DroneService droneService;

    /** Health check */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "drone-management"));
    }

    /**
     * List all registered drones.
     * Example: curl http://localhost:8002/api/drones
     */
    @GetMapping
    public ResponseEntity<List<Drone>> getAllDrones() {
        return ResponseEntity.ok(droneService.getAllDrones());
    }

    /**
     * Get a specific drone by ID.
     * Returns 404 if drone not found.
     * Example: curl http://localhost:8002/api/drones/DR-001
     */
    @GetMapping("/{droneId}")
    public ResponseEntity<Drone> getDrone(@PathVariable String droneId) {
        return droneService.getDroneById(droneId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Register a new drone.
     * Example: curl -X POST http://localhost:8002/api/drones -H "Content-Type: application/json" -d '{"droneId":"DR-006",...}'
     */
    @PostMapping
    public ResponseEntity<Drone> createDrone(@RequestBody Drone drone) {
        return ResponseEntity.ok(droneService.createDrone(drone));
    }

    /**
     * Update an existing drone's information.
     * Only provided fields are updated (partial update).
     * Example: curl -X PUT http://localhost:8002/api/drones/DR-001 -d '{"mission":"New Mission"}'
     */
    @PutMapping("/{droneId}")
    public ResponseEntity<Drone> updateDrone(@PathVariable String droneId, @RequestBody Drone drone) {
        return droneService.updateDrone(droneId, drone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a drone from the registry.
     * Example: curl -X DELETE http://localhost:8002/api/drones/DR-001
     */
    @DeleteMapping("/{droneId}")
    public ResponseEntity<Map<String, String>> deleteDrone(@PathVariable String droneId) {
        if (droneService.deleteDrone(droneId)) {
            return ResponseEntity.ok(Map.of("status", "deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Find drones by operational status.
     * Example: curl http://localhost:8002/api/drones/status/ACTIVE
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Drone>> getDronesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(droneService.getDronesByStatus(status));
    }

    /**
     * Find drones by team assignment.
     * Example: curl http://localhost:8002/api/drones/team/Team-1
     */
    @GetMapping("/team/{team}")
    public ResponseEntity<List<Drone>> getDronesByTeam(@PathVariable String team) {
        return ResponseEntity.ok(droneService.getDronesByTeam(team));
    }
}
