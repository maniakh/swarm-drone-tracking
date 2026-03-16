package com.swarm.anomaly.model;

/**
 * ANOMALY DTO - Represents a Detected Anomaly
 * ==============================================
 * When a drone's telemetry data violates a rule, an AnomalyDTO is created.
 *
 * Fields:
 *   - droneId   : Which drone triggered the anomaly (e.g., "DR-001")
 *   - type      : Anomaly category (SPEED_EXCEEDED, LOW_BATTERY, ALTITUDE_EXCEEDED, etc.)
 *   - severity  : How serious it is (critical, high, medium, low)
 *   - message   : Human-readable description of what happened
 *   - value     : The actual value that triggered the anomaly (e.g., speed = 45 m/s)
 *   - threshold : The limit that was violated (e.g., max speed = 40 m/s)
 *   - timestamp : When the anomaly was detected
 *
 * Anomaly Types:
 *   SPEED_EXCEEDED      → Drone is flying too fast (> 40 m/s)
 *   LOW_BATTERY         → Battery is critically low (< 15%)
 *   ALTITUDE_EXCEEDED   → Drone is flying too high (> 350m)
 *   ALTITUDE_TOO_LOW    → Drone is flying too low (< 5m)
 *   GEO_FENCE_VIOLATION → Drone left the allowed geographical zone
 */
public class AnomalyDTO {
    private String droneId;
    private String type;       // Anomaly type (enum-like string)
    private String severity;   // critical | high | medium | low
    private String message;    // Human-readable description
    private double value;      // Actual measured value
    private double threshold;  // The limit that was violated
    private String timestamp;  // When detected (ISO-8601)

    public AnomalyDTO() {}

    public AnomalyDTO(String droneId, String type, String severity,
                      String message, double value, double threshold, String timestamp) {
        this.droneId = droneId;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.value = value;
        this.threshold = threshold;
        this.timestamp = timestamp;
    }

    // ─── Getters & Setters ───
    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
