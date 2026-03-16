package com.swarm.telemetry.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * TELEMETRY DATA - JPA Entity (Database Table Mapping)
 * =====================================================
 * This class maps to the "telemetry" table in PostgreSQL.
 * Each row represents a single telemetry reading from a drone at a point in time.
 *
 * JPA (Java Persistence API) annotations tell Spring how to map this object to a database table:
 *   @Entity → Marks this class as a database entity
 *   @Table  → Specifies the table name in PostgreSQL
 *   @Id     → Marks the primary key field
 *   @Column → Maps a field to a specific database column
 *
 * Fields explained:
 *   - droneId   : Unique identifier for the drone (e.g., "DR-001")
 *   - lat / lon : GPS coordinates (latitude / longitude) - Istanbul area
 *   - altitude  : Flight altitude in meters
 *   - speed     : Drone speed in meters per second (m/s)
 *   - battery   : Battery level as percentage (0-100%)
 *   - timestamp : When this reading was taken (ISO-8601 format)
 */
@Entity
@Table(name = "telemetry")
public class TelemetryData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment primary key
    private Long id;

    @Column(name = "drone_id")
    private String droneId;

    private double lat;       // Latitude  (e.g., 41.01 for Istanbul)
    private double lon;       // Longitude (e.g., 28.97 for Istanbul)
    private double altitude;  // Meters above sea level
    private double speed;     // Meters per second
    private double battery;   // Percentage (0-100)

    @Column(name = "timestamp")
    private Instant timestamp;

    // Default constructor required by JPA
    public TelemetryData() {}

    // Full constructor for creating telemetry records programmatically
    public TelemetryData(String droneId, double lat, double lon, double altitude,
                         double speed, double battery, Instant timestamp) {
        this.droneId = droneId;
        this.lat = lat;
        this.lon = lon;
        this.altitude = altitude;
        this.speed = speed;
        this.battery = battery;
        this.timestamp = timestamp;
    }

    // ─── Getters & Setters (required by JPA and JSON serialization) ───
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getBattery() { return battery; }
    public void setBattery(double battery) { this.battery = battery; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
