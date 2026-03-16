package com.swarm.dronemgmt.repository;

import com.swarm.dronemgmt.model.Drone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DroneRepository extends JpaRepository<Drone, Long> {
    Optional<Drone> findByDroneId(String droneId);
    List<Drone> findByStatus(String status);
    List<Drone> findByTeam(String team);
    boolean existsByDroneId(String droneId);
}
