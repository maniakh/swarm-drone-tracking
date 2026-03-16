package com.swarm.alert.model;

import java.util.UUID;

/**
 * ALERT DTO - Represents an Alert Notification
 * ===============================================
 * An alert is created when an anomaly is detected.
 * It contains the anomaly information plus an ID and acknowledgment status.
 *
 * Fields:
 *   - id           : Unique alert identifier (auto-generated UUID)
 *   - droneId      : Which drone triggered the alert
 *   - type         : Alert category (same as anomaly type)
 *   - severity     : How serious (critical, high, medium, low)
 *   - message      : Human-readable description
 *   - value        : The measured value that caused the alert
 *   - threshold    : The threshold that was violated
 *   - timestamp    : When the alert was created
 *   - acknowledged : Whether an operator has acknowledged this alert (default: false)
 *
 * Lifecycle:
 *   1. Created (acknowledged = false)
 *   2. Displayed on dashboard
 *   3. Operator clicks "acknowledge" → acknowledged = true
 */
public class AlertDTO {
    private String id;              // Unique identifier (first 8 chars of UUID)
    private String droneId;         // Source drone
    private String type;            // Alert type (SPEED_EXCEEDED, LOW_BATTERY, etc.)
    private String severity;        // Severity level
    private String message;         // Human-readable message
    private double value;           // Actual measured value
    private double threshold;       // Threshold that was violated
    private String timestamp;       // When created
    private boolean acknowledged;   // Has an operator acknowledged this?

    /**
     * Default constructor - auto-generates a unique ID.
     * UUID.randomUUID() creates a universally unique identifier.
     * We take only the first 8 characters for readability.
     */
    public AlertDTO() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.acknowledged = false;
    }

    // ─── Getters & Setters ───
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }
}
