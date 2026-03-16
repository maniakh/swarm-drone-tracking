package com.swarm.telemetry.model;

/**
 * TELEMETRY DTO - Data Transfer Object
 * ======================================
 * DTO = Data Transfer Object. This is a lightweight version of TelemetryData
 * used for REST API communication (JSON input/output).
 *
 * Why do we need a DTO separate from the Entity?
 *   - Entity (@Entity) is tied to the database and has JPA-specific fields (id, etc.)
 *   - DTO is a simple POJO (Plain Old Java Object) for transferring data over HTTP
 *   - This separation follows the "Clean Architecture" principle
 *
 * This DTO is used in:
 *   - REST API responses (GET /telemetry/latest → returns List<TelemetryDTO>)
 *   - REST API requests  (POST /telemetry → receives TelemetryDTO)
 *   - Inter-service communication (Telemetry → Anomaly Service)
 *
 * JSON example:
 * {
 *   "droneId": "DR-001",
 *   "lat": 41.0123,
 *   "lon": 28.9756,
 *   "altitude": 120.5,
 *   "speed": 12.3,
 *   "battery": 78.2,
 *   "timestamp": "2026-03-16T10:30:00Z"
 * }
 */
public class TelemetryDTO {
    private String droneId;    // Drone identifier (e.g., "DR-001")
    private double lat;        // GPS latitude
    private double lon;        // GPS longitude
    private double altitude;   // Altitude in meters
    private double speed;      // Speed in m/s
    private double battery;    // Battery percentage
    private String timestamp;  // ISO-8601 timestamp string

    // Default constructor (required for JSON deserialization by Jackson)
    public TelemetryDTO() {}

    // Full constructor (used by TelemetrySimulator to create DTO from drone state)
    public TelemetryDTO(String droneId, double lat, double lon, double altitude,
                        double speed, double battery, String timestamp) {
        this.droneId = droneId;
        this.lat = lat;
        this.lon = lon;
        this.altitude = altitude;
        this.speed = speed;
        this.battery = battery;
        this.timestamp = timestamp;
    }

    // ─── Getters & Setters ───
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
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
