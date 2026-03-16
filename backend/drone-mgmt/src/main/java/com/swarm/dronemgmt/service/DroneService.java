package com.swarm.dronemgmt.service;

import com.swarm.dronemgmt.model.Drone;
import com.swarm.dronemgmt.repository.DroneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * DRONE SERVICE - Business Logic for Drone Management
 * =====================================================
 * Contains all the business logic for managing drones:
 *   - Auto-initialization of 5 default drones on startup
 *   - CRUD operations (Create, Read, Update, Delete)
 *   - Query by status and team
 *
 * @PostConstruct ensures default drones exist in the database on first run.
 * If the database already has these drones (e.g., after restart), they are skipped.
 */
@Service
public class DroneService {

    @Autowired
    private DroneRepository droneRepository;

    /**
     * Initialize default drones on application startup.
     * This runs once when the service starts.
     * Uses existsByDroneId() to avoid duplicate entries on restart.
     *
     * Default fleet:
     *   DR-001 : DJI Matrice 300  → Recon Mission Alpha (Team-1)
     *   DR-002 : DJI Mavic 3     → Mapping Survey (Team-1)
     *   DR-003 : Autel EVO II    → Border Patrol (Team-2)
     *   DR-004 : Skydio X2       → Surveillance Beta (Team-2)
     *   DR-005 : DJI Phantom 4   → Cargo Transport (Team-3)
     */
    @PostConstruct
    public void initDefaultDrones() {
        // Each row: [droneId, model, mission, team, status]
        String[][] defaults = {
            {"DR-001", "DJI Matrice 300", "Recon Mission Alpha", "Team-1", "ACTIVE"},
            {"DR-002", "DJI Mavic 3", "Mapping Survey", "Team-1", "ACTIVE"},
            {"DR-003", "Autel EVO II", "Border Patrol", "Team-2", "ACTIVE"},
            {"DR-004", "Skydio X2", "Surveillance Beta", "Team-2", "ACTIVE"},
            {"DR-005", "DJI Phantom 4", "Cargo Transport", "Team-3", "ACTIVE"},
        };

        for (String[] d : defaults) {
            // Only create if this drone doesn't already exist
            if (!droneRepository.existsByDroneId(d[0])) {
                Drone drone = new Drone();
                drone.setDroneId(d[0]);
                drone.setModel(d[1]);
                drone.setMission(d[2]);
                drone.setTeam(d[3]);
                drone.setStatus(d[4]);
                drone.setMaxSpeed(40.0);          // Default speed limit: 40 m/s
                drone.setMaxAltitude(350.0);       // Default altitude limit: 350m
                drone.setBatteryCapacity(100.0);   // Full battery capacity
                droneRepository.save(drone);
            }
        }
        System.out.println("Default drones initialized");
    }

    /** Get all registered drones. Used by: GET /api/drones */
    public List<Drone> getAllDrones() {
        return droneRepository.findAll();
    }

    /** Find a specific drone by its ID. Used by: GET /api/drones/{droneId} */
    public Optional<Drone> getDroneById(String droneId) {
        return droneRepository.findByDroneId(droneId);
    }

    /** Register a new drone. Used by: POST /api/drones */
    public Drone createDrone(Drone drone) {
        return droneRepository.save(drone);
    }

    /**
     * Update an existing drone's information.
     * Only updates fields that are provided (non-null).
     * Used by: PUT /api/drones/{droneId}
     */
    public Optional<Drone> updateDrone(String droneId, Drone droneUpdate) {
        return droneRepository.findByDroneId(droneId).map(existing -> {
            if (droneUpdate.getModel() != null) existing.setModel(droneUpdate.getModel());
            if (droneUpdate.getMission() != null) existing.setMission(droneUpdate.getMission());
            if (droneUpdate.getTeam() != null) existing.setTeam(droneUpdate.getTeam());
            if (droneUpdate.getStatus() != null) existing.setStatus(droneUpdate.getStatus());
            if (droneUpdate.getMaxSpeed() != null) existing.setMaxSpeed(droneUpdate.getMaxSpeed());
            if (droneUpdate.getMaxAltitude() != null) existing.setMaxAltitude(droneUpdate.getMaxAltitude());
            return droneRepository.save(existing);
        });
    }

    /** Delete a drone. Used by: DELETE /api/drones/{droneId} */
    public boolean deleteDrone(String droneId) {
        return droneRepository.findByDroneId(droneId).map(drone -> {
            droneRepository.delete(drone);
            return true;
        }).orElse(false);
    }

    /** Find drones by status. Used by: GET /api/drones/status/ACTIVE */
    public List<Drone> getDronesByStatus(String status) {
        return droneRepository.findByStatus(status);
    }

    /** Find drones by team. Used by: GET /api/drones/team/Team-1 */
    public List<Drone> getDronesByTeam(String team) {
        return droneRepository.findByTeam(team);
    }
}
