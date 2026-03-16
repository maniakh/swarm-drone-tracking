package com.swarm.dronemgmt.controller;

import com.swarm.dronemgmt.model.Drone;
import com.swarm.dronemgmt.service.DroneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drones")
@CrossOrigin(origins = "*")
public class DroneController {

    @Autowired
    private DroneService droneService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "drone-management"));
    }

    @GetMapping
    public ResponseEntity<List<Drone>> getAllDrones() {
        return ResponseEntity.ok(droneService.getAllDrones());
    }

    @GetMapping("/{droneId}")
    public ResponseEntity<Drone> getDrone(@PathVariable String droneId) {
        return droneService.getDroneById(droneId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Drone> createDrone(@RequestBody Drone drone) {
        return ResponseEntity.ok(droneService.createDrone(drone));
    }

    @PutMapping("/{droneId}")
    public ResponseEntity<Drone> updateDrone(@PathVariable String droneId, @RequestBody Drone drone) {
        return droneService.updateDrone(droneId, drone)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{droneId}")
    public ResponseEntity<Map<String, String>> deleteDrone(@PathVariable String droneId) {
        if (droneService.deleteDrone(droneId)) {
            return ResponseEntity.ok(Map.of("status", "deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Drone>> getDronesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(droneService.getDronesByStatus(status));
    }

    @GetMapping("/team/{team}")
    public ResponseEntity<List<Drone>> getDronesByTeam(@PathVariable String team) {
        return ResponseEntity.ok(droneService.getDronesByTeam(team));
    }
}
