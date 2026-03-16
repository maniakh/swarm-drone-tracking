package com.swarm.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ALERT SERVICE - Main Entry Point
 * ==================================
 * This microservice receives anomaly notifications and manages alerts.
 *
 * How it works:
 *   1. Anomaly Detection Service detects a problem → sends POST /api/alert
 *   2. This service stores the alert in memory with a unique ID
 *   3. Dashboard polls GET /alerts to display them to the operator
 *   4. Operator can acknowledge alerts via POST /alerts/{id}/acknowledge
 *
 * In production, this service would also:
 *   - Send Slack notifications
 *   - Send email alerts
 *   - Trigger webhooks
 *   - Escalate unacknowledged alerts
 *
 * Port: 8004 (configured in application.yml)
 * No database needed - alerts are stored in-memory
 */
@SpringBootApplication
public class AlertApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertApplication.class, args);
    }
}
