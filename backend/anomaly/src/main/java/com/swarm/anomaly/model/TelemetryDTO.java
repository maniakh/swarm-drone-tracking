package com.swarm.anomaly.model;

/**
 * TELEMETRY DTO - Input Data for Anomaly Analysis
 * =================================================
 * This is a copy of the TelemetryDTO from the Telemetry Service.
 * In a microservices architecture, each service has its own models
 * to maintain independence (no shared library dependencies).
 *
 * This DTO receives the drone telemetry data that needs to be analyzed.
 * The Anomaly Detection Service checks each field against configured thresholds.
 */
public class TelemetryDTO {
    private String droneId;
    private double lat;
    private double lon;
    private double altitude;
    private double speed;
    private double battery;
    private String timestamp;

    public TelemetryDTO() {}

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
