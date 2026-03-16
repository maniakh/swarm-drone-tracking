package com.swarm.dronemgmt.service;

import com.swarm.dronemgmt.model.Drone;
import com.swarm.dronemgmt.repository.DroneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
public class DroneService {

    @Autowired
    private DroneRepository droneRepository;

    @PostConstruct
    public void initDefaultDrones() {
        String[][] defaults = {
            {"DR-001", "DJI Matrice 300", "Recon Mission Alpha", "Team-1", "ACTIVE"},
            {"DR-002", "DJI Mavic 3", "Mapping Survey", "Team-1", "ACTIVE"},
            {"DR-003", "Autel EVO II", "Border Patrol", "Team-2", "ACTIVE"},
            {"DR-004", "Skydio X2", "Surveillance Beta", "Team-2", "ACTIVE"},
            {"DR-005", "DJI Phantom 4", "Cargo Transport", "Team-3", "ACTIVE"},
        };

        for (String[] d : defaults) {
            if (!droneRepository.existsByDroneId(d[0])) {
                Drone drone = new Drone();
                drone.setDroneId(d[0]);
                drone.setModel(d[1]);
                drone.setMission(d[2]);
                drone.setTeam(d[3]);
                drone.setStatus(d[4]);
                drone.setMaxSpeed(40.0);
                drone.setMaxAltitude(350.0);
                drone.setBatteryCapacity(100.0);
                droneRepository.save(drone);
            }
        }
        System.out.println("Default drones initialized");
    }

    public List<Drone> getAllDrones() {
        return droneRepository.findAll();
    }

    public Optional<Drone> getDroneById(String droneId) {
        return droneRepository.findByDroneId(droneId);
    }

    public Drone createDrone(Drone drone) {
        return droneRepository.save(drone);
    }

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

    public boolean deleteDrone(String droneId) {
        return droneRepository.findByDroneId(droneId).map(drone -> {
            droneRepository.delete(drone);
            return true;
        }).orElse(false);
    }

    public List<Drone> getDronesByStatus(String status) {
        return droneRepository.findByStatus(status);
    }

    public List<Drone> getDronesByTeam(String team) {
        return droneRepository.findByTeam(team);
    }
}
