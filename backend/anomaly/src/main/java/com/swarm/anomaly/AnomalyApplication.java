package com.swarm.anomaly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ANOMALY DETECTION SERVICE - Main Entry Point
 * ==============================================
 * This microservice analyzes incoming telemetry data and detects anomalies.
 *
 * How it works:
 *   1. Telemetry Service sends each drone's data via POST /api/anomaly/analyze
 *   2. This service applies rule-based checks (speed, battery, altitude, geo-fence)
 *   3. If any threshold is violated, an anomaly is created
 *   4. Anomalies are forwarded to the Alert Service for notification
 *
 * Flow: Telemetry Service → [this service] → Alert Service
 *
 * Port: 8003 (configured in application.yml)
 * No database needed - anomalies are stored in-memory (CopyOnWriteArrayList)
 */
@SpringBootApplication
public class AnomalyApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnomalyApplication.class, args);
    }
}
