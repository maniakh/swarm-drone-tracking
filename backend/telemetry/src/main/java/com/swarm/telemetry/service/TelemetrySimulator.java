package com.swarm.telemetry.service;

import com.swarm.telemetry.model.TelemetryDTO;
import com.swarm.telemetry.model.TelemetryData;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TELEMETRY SIMULATOR - Simulates 5 Drones Flying Over Istanbul
 * ===============================================================
 * This is the data generator of the entire system. Instead of using real drones,
 * we simulate realistic flight behavior using mathematical models:
 *
 *   - Each drone has a state (position, speed, altitude, battery)
 *   - Every 3 seconds (@Scheduled), all drone states are updated
 *   - Updated data is stored in TelemetryStore and forwarded to Anomaly Service
 *
 * Key concepts used:
 *   @Service   → Makes this a Spring-managed bean (singleton by default)
 *   @Scheduled → Runs simulateTick() automatically every 3 seconds
 *   @Value     → Injects values from application.yml configuration
 *   WebClient  → Non-blocking HTTP client for inter-service communication
 *
 * Anomaly injection:
 *   Occasionally (1-1.5% probability per tick), the simulator injects anomalies
 *   such as excessive speed, low battery, or high altitude to test the detection system.
 *
 * Prometheus metrics:
 *   - telemetry_received_total : Counter of total telemetry data points generated
 *   - active_drones            : Gauge showing current number of active drones
 */
@Service
public class TelemetrySimulator {

    // ─── Configuration values injected from application.yml ───
    @Value("${simulation.enabled:true}")
    private boolean simulationEnabled;       // Whether simulation is active

    @Value("${simulation.num-drones:5}")
    private int numDrones;                   // Number of drones to simulate

    @Value("${anomaly.service-url:http://anomaly:8003}")
    private String anomalyServiceUrl;        // URL of the Anomaly Detection Service

    // ─── Dependencies (injected by Spring via constructor) ───
    private final TelemetryStore store;      // In-memory store for telemetry data
    private final MeterRegistry meterRegistry; // Prometheus metrics registry
    private final WebClient webClient;       // HTTP client for calling Anomaly Service

    // ─── Internal state ───
    private Counter telemetryCounter;        // Prometheus counter for total data points
    private final Map<String, DroneState> droneStates = new ConcurrentHashMap<>(); // State of each drone

    /**
     * Constructor injection - Spring automatically provides TelemetryStore and MeterRegistry.
     * WebClient is created manually (no need for dependency injection here).
     */
    public TelemetrySimulator(TelemetryStore store, MeterRegistry meterRegistry) {
        this.store = store;
        this.meterRegistry = meterRegistry;
        this.webClient = WebClient.create();
    }

    /**
     * Called once after the bean is created.
     * Sets up Prometheus metrics and initializes drone states.
     */
    @PostConstruct
    public void init() {
        // Register a counter metric - incremented every time a telemetry point is generated
        telemetryCounter = Counter.builder("telemetry_received_total")
                .description("Total telemetry data points").register(meterRegistry);

        // Register a gauge metric - dynamically reads the number of active drones
        Gauge.builder("active_drones", store, s -> s.getLatestAll().size())
                .description("Number of active drones").register(meterRegistry);

        // Create initial drone states if simulation is enabled
        if (simulationEnabled) {
            for (int i = 1; i <= numDrones; i++) {
                String id = String.format("DR-%03d", i);  // DR-001, DR-002, etc.
                droneStates.put(id, new DroneState(id));
            }
            System.out.println("Simulation initialized with " + numDrones + " drones");
        }
    }

    /**
     * MAIN SIMULATION LOOP - Runs every 3 seconds (configurable via SIM_INTERVAL_MS)
     *
     * For each drone:
     *   1. Update its state (position, speed, altitude, battery + possible anomaly)
     *   2. Convert state to DTO and store it
     *   3. Increment Prometheus counter
     *   4. Forward the data to Anomaly Service for analysis (async, non-blocking)
     */
    @Scheduled(fixedDelayString = "${simulation.interval-ms:3000}")
    public void simulateTick() {
        if (!simulationEnabled) return;

        for (DroneState state : droneStates.values()) {
            // Step 1: Update drone state (random walk + anomaly injection)
            state.update();

            // Step 2: Convert to DTO and store in memory
            TelemetryDTO dto = state.toDTO();
            store.store(dto);
            telemetryCounter.increment();

            // Step 3: Forward to Anomaly Detection Service (fire-and-forget, async)
            try {
                webClient.post()
                        .uri(anomalyServiceUrl + "/api/anomaly/analyze")
                        .bodyValue(dto)
                        .retrieve()
                        .toBodilessEntity()
                        .subscribe(
                                r -> {},       // Success: do nothing (fire-and-forget)
                                err -> {}      // Error: silently ignore (anomaly service may be starting up)
                        );
            } catch (Exception ignored) {}
        }
    }

    /**
     * DRONE STATE - Internal class that holds and updates a single drone's flight parameters
     * =====================================================================================
     * Uses a Gaussian random walk to simulate realistic drone movement:
     *   - Position changes slightly each tick (random walk)
     *   - Speed fluctuates around 12 m/s average
     *   - Altitude fluctuates around 120m average
     *   - Battery slowly drains, recharges when below 20%
     *
     * Anomaly injection (to test the anomaly detection system):
     *   - 1.5% chance of speed anomaly per tick (wind gust / emergency maneuver)
     *   - 1.0% chance of battery anomaly per tick (sensor fault / sudden drain)
     *   - 1.0% chance of altitude anomaly per tick (thermal updraft / control error)
     */
    static class DroneState {
        private final String droneId;
        private double lat, lon, altitude, speed, battery;
        private final Random rng = new Random();

        /**
         * Initialize drone with realistic starting values in Istanbul area.
         * Gaussian distribution adds slight randomness to each drone's start position.
         */
        DroneState(String droneId) {
            this.droneId = droneId;
            this.lat = 41.01 + rng.nextGaussian() * 0.02;       // Istanbul latitude ± small offset
            this.lon = 28.97 + rng.nextGaussian() * 0.02;       // Istanbul longitude ± small offset
            this.altitude = 80 + rng.nextDouble() * 70;          // Start at 80-150m
            this.speed = 8 + rng.nextDouble() * 7;               // Start at 8-15 m/s
            this.battery = 85 + rng.nextDouble() * 15;           // Start at 85-100%
        }

        /**
         * Update drone state for one simulation tick.
         * Called every 3 seconds by simulateTick().
         */
        void update() {
            // ── NORMAL MOVEMENT (realistic random walk) ──
            // Small position changes based on Gaussian distribution
            lat += rng.nextGaussian() * 0.0008;   // ~80m movement per tick
            lon += rng.nextGaussian() * 0.0008;

            // Speed: gradual change, stays between 3-28 m/s
            speed += rng.nextGaussian() * 0.8;
            speed = Math.max(3, Math.min(28, speed));

            // Altitude: gradual change, stays between 30-280m
            altitude += rng.nextGaussian() * 2;
            altitude = Math.max(30, Math.min(280, altitude));

            // Battery: slowly drains (0.02-0.08% per tick ≈ realistic for a 30-min flight)
            battery -= 0.02 + rng.nextDouble() * 0.06;
            if (battery < 20) {
                // Simulate "return to charging station" when battery gets low
                battery = 90 + rng.nextDouble() * 10;
            }

            // ── RARE ANOMALY INJECTION (to test the detection system) ──

            // 1.5% chance: Speed anomaly (simulates wind gust or emergency maneuver)
            // Normal speed limit is 40 m/s, this generates 42-52 m/s → triggers SPEED_EXCEEDED
            if (rng.nextDouble() < 0.015) {
                speed = 42 + rng.nextDouble() * 10;
            }

            // 1% chance: Low battery anomaly (simulates sensor fault or sudden drain)
            // Normal minimum is 15%, this generates 3-11% → triggers LOW_BATTERY
            if (rng.nextDouble() < 0.01) {
                battery = 3 + rng.nextDouble() * 8;
            }

            // 1% chance: Altitude anomaly (simulates thermal updraft or control error)
            // Normal max altitude is 350m, this generates 360-440m → triggers ALTITUDE_EXCEEDED
            if (rng.nextDouble() < 0.01) {
                altitude = 360 + rng.nextDouble() * 80;
            }
        }

        /**
         * Convert current drone state to a TelemetryDTO for API/storage use.
         * Values are rounded for cleaner output.
         */
        TelemetryDTO toDTO() {
            return new TelemetryDTO(
                    droneId,
                    Math.round(lat * 1e6) / 1e6,          // Round to 6 decimal places
                    Math.round(lon * 1e6) / 1e6,
                    Math.round(altitude * 10.0) / 10.0,    // Round to 1 decimal place
                    Math.round(speed * 10.0) / 10.0,
                    Math.round(battery * 10.0) / 10.0,
                    Instant.now().toString()                // ISO-8601 timestamp
            );
        }
    }
}
