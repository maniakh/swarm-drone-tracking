package com.swarm.dronemgmt.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DRONE ENTITY - Database Model for Drone Metadata
 * ===================================================
 * This JPA entity maps to the "drones" table in PostgreSQL.
 * Stores static information about each drone in the swarm.
 *
 * Fields:
 *   - id              : Auto-generated primary key (internal use only)
 *   - droneId         : Human-readable drone identifier (e.g., "DR-001") - unique
 *   - model           : Drone hardware model (e.g., "DJI Matrice 300")
 *   - mission         : Currently assigned mission (e.g., "Recon Mission Alpha")
 *   - team            : Team assignment (e.g., "Team-1")
 *   - status          : Current operational status (ACTIVE, IDLE, MAINTENANCE, OFFLINE)
 *   - maxSpeed        : Maximum allowed speed in m/s
 *   - maxAltitude     : Maximum allowed altitude in meters
 *   - batteryCapacity : Battery capacity in percentage
 *   - createdAt       : When this drone was registered
 *   - updatedAt       : When this drone's info was last updated
 *
 * JPA Lifecycle Callbacks:
 *   @PrePersist → Automatically sets createdAt and updatedAt when first saved
 *   @PreUpdate  → Automatically updates updatedAt on every modification
 */
@Entity
@Table(name = "drones")
public class Drone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String droneId;            // e.g., "DR-001" (unique identifier)

    @Column(nullable = false)
    private String model;              // e.g., "DJI Matrice 300"

    @Column
    private String mission;            // e.g., "Recon Mission Alpha"

    @Column
    private String team;               // e.g., "Team-1"

    @Column
    private String status;             // ACTIVE, IDLE, MAINTENANCE, OFFLINE

    @Column
    private Double maxSpeed;           // Maximum speed limit (m/s)

    @Column
    private Double maxAltitude;        // Maximum altitude limit (meters)

    @Column
    private Double batteryCapacity;    // Battery capacity (%)

    @Column
    private LocalDateTime createdAt;   // Registration timestamp

    @Column
    private LocalDateTime updatedAt;   // Last update timestamp

    /**
     * JPA lifecycle callback - runs before the entity is first saved.
     * Automatically sets creation and update timestamps.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback - runs before every update.
     * Automatically refreshes the update timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ───
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getMission() { return mission; }
    public void setMission(String mission) { this.mission = mission; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(Double maxSpeed) { this.maxSpeed = maxSpeed; }

    public Double getMaxAltitude() { return maxAltitude; }
    public void setMaxAltitude(Double maxAltitude) { this.maxAltitude = maxAltitude; }

    public Double getBatteryCapacity() { return batteryCapacity; }
    public void setBatteryCapacity(Double batteryCapacity) { this.batteryCapacity = batteryCapacity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
