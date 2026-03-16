package com.swarm.anomaly.model;

public class TelemetryDTO {
    private String droneId;
    private double lat;
    private double lon;
    private double altitude;
    private double speed;
    private double battery;
    private String timestamp;

    public TelemetryDTO() {}

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
