package com.swarm.dronemgmt.repository;

import com.swarm.dronemgmt.model.Drone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DRONE REPOSITORY - Database Access Layer (Spring Data JPA)
 * ============================================================
 * This interface extends JpaRepository which provides CRUD operations automatically.
 * Spring Data JPA generates the SQL queries based on method names - no SQL needed!
 *
 * Auto-generated methods from JpaRepository:
 *   findAll()          → SELECT * FROM drones
 *   save(drone)        → INSERT or UPDATE
 *   delete(drone)      → DELETE FROM drones WHERE id = ?
 *   existsById(id)     → SELECT COUNT(*) FROM drones WHERE id = ?
 *
 * Custom query methods (Spring generates SQL from method name):
 *   findByDroneId("DR-001")    → SELECT * FROM drones WHERE drone_id = 'DR-001'
 *   findByStatus("ACTIVE")     → SELECT * FROM drones WHERE status = 'ACTIVE'
 *   findByTeam("Team-1")       → SELECT * FROM drones WHERE team = 'Team-1'
 *   existsByDroneId("DR-001")  → SELECT COUNT(*) FROM drones WHERE drone_id = 'DR-001'
 */
@Repository
public interface DroneRepository extends JpaRepository<Drone, Long> {

    /** Find a drone by its human-readable ID (e.g., "DR-001") */
    Optional<Drone> findByDroneId(String droneId);

    /** Find all drones with a specific status (e.g., "ACTIVE") */
    List<Drone> findByStatus(String status);

    /** Find all drones assigned to a specific team (e.g., "Team-1") */
    List<Drone> findByTeam(String team);

    /** Check if a drone with this ID already exists (used during initialization) */
    boolean existsByDroneId(String droneId);
}
