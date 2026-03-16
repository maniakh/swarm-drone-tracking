package com.swarm.dronemgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * DRONE MANAGEMENT SERVICE - Main Entry Point
 * ==============================================
 * This microservice manages drone metadata (registration, model, team, mission info).
 * It uses PostgreSQL to persistently store drone information.
 *
 * Unlike Telemetry/Anomaly/Alert services that deal with real-time data,
 * this service manages static drone information:
 *   - Drone ID and model (e.g., "DR-001", "DJI Matrice 300")
 *   - Assigned mission (e.g., "Recon Mission Alpha")
 *   - Team assignment (e.g., "Team-1")
 *   - Status (ACTIVE, IDLE, MAINTENANCE, OFFLINE)
 *   - Performance limits (maxSpeed, maxAltitude, batteryCapacity)
 *
 * On startup, 5 default drones are automatically created if they don't exist.
 *
 * Port: 8002 (configured in application.yml)
 * Database: PostgreSQL (shared with Telemetry Service)
 */
@SpringBootApplication
public class DroneMgmtApplication {
    public static void main(String[] args) {
        SpringApplication.run(DroneMgmtApplication.class, args);
    }
}
