package com.swarm.anomaly.model;

public class AnomalyDTO {
    private String droneId;
    private String type;
    private String severity;
    private String message;
    private double value;
    private double threshold;
    private String timestamp;

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
