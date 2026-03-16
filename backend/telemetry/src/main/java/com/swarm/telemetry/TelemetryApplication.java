package com.swarm.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TELEMETRY SERVICE - Main Entry Point
 * =====================================
 * This is the heart of the system. It does two things:
 *   1) Simulates 5 drones flying over Istanbul, generating GPS/speed/battery data every 3 seconds
 *   2) Provides REST API endpoints so the dashboard can fetch real-time drone positions
 *
 * Flow:
 *   TelemetrySimulator (generates data) → TelemetryStore (keeps in memory) → TelemetryController (serves via REST)
 *                                       ↘ sends to Anomaly Service for analysis
 *
 * Annotations explained:
 *   @SpringBootApplication → Combines @Configuration + @EnableAutoConfiguration + @ComponentScan
 *                            This single annotation makes this class the Spring Boot entry point
 *   @EnableScheduling      → Enables @Scheduled methods (used by TelemetrySimulator to run every 3 seconds)
 */
@SpringBootApplication
@EnableScheduling
public class TelemetryApplication {
    public static void main(String[] args) {
        // SpringApplication.run() bootstraps the entire application:
        // - Starts embedded Tomcat server on port 8001
        // - Scans for @Service, @Controller, @Repository beans
        // - Connects to PostgreSQL database
        // - Begins the simulation scheduler
        SpringApplication.run(TelemetryApplication.class, args);
    }
}
