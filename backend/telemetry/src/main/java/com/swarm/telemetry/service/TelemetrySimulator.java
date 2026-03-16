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

@Service
public class TelemetrySimulator {

    @Value("${simulation.enabled:true}")
    private boolean simulationEnabled;

    @Value("${simulation.num-drones:5}")
    private int numDrones;

    @Value("${anomaly.service-url:http://anomaly:8003}")
    private String anomalyServiceUrl;

    private final TelemetryStore store;
    private final MeterRegistry meterRegistry;
    private final WebClient webClient;

    private Counter telemetryCounter;
    private final Map<String, DroneState> droneStates = new ConcurrentHashMap<>();

    public TelemetrySimulator(TelemetryStore store, MeterRegistry meterRegistry) {
        this.store = store;
        this.meterRegistry = meterRegistry;
        this.webClient = WebClient.create();
    }

    @PostConstruct
    public void init() {
        telemetryCounter = Counter.builder("telemetry_received_total")
                .description("Total telemetry data points").register(meterRegistry);
        Gauge.builder("active_drones", store, s -> s.getLatestAll().size())
                .description("Number of active drones").register(meterRegistry);

        if (simulationEnabled) {
            for (int i = 1; i <= numDrones; i++) {
                String id = String.format("DR-%03d", i);
                droneStates.put(id, new DroneState(id));
            }
            System.out.println("Simulation initialized with " + numDrones + " drones");
        }
    }

    @Scheduled(fixedDelayString = "${simulation.interval-ms:3000}")
    public void simulateTick() {
        if (!simulationEnabled) return;

        for (DroneState state : droneStates.values()) {
            state.update();
            TelemetryDTO dto = state.toDTO();
            store.store(dto);
            telemetryCounter.increment();

            // Forward to anomaly service
            try {
                webClient.post()
                        .uri(anomalyServiceUrl + "/api/anomaly/analyze")
                        .bodyValue(dto)
                        .retrieve()
                        .toBodilessEntity()
                        .subscribe(
                                r -> {},
                                err -> {} // silently ignore
                        );
            } catch (Exception ignored) {}
        }
    }

    /**
     * Realistic drone state simulation.
     * Normal flight parameters + rare anomaly injection.
     */
    static class DroneState {
        private final String droneId;
        private double lat, lon, altitude, speed, battery;
        private final Random rng = new Random();

        // Realistic flight area: Istanbul
        DroneState(String droneId) {
            this.droneId = droneId;
            this.lat = 41.01 + rng.nextGaussian() * 0.02;
            this.lon = 28.97 + rng.nextGaussian() * 0.02;
            this.altitude = 80 + rng.nextDouble() * 70;   // 80-150m range
            this.speed = 8 + rng.nextDouble() * 7;         // 8-15 m/s
            this.battery = 85 + rng.nextDouble() * 15;     // 85-100%
        }

        void update() {
            // Normal movement (realistic random walk)
            lat += rng.nextGaussian() * 0.0008;
            lon += rng.nextGaussian() * 0.0008;

            // Speed: average ~12 m/s, slow change
            speed += rng.nextGaussian() * 0.8;
            speed = Math.max(3, Math.min(28, speed));

            // Altitude: average ~120m, slow change
            altitude += rng.nextGaussian() * 2;
            altitude = Math.max(30, Math.min(280, altitude));

            // Battery: slow drain
            battery -= 0.02 + rng.nextDouble() * 0.06;
            if (battery < 20) {
                // Below 20%: simulate return to charging station
                battery = 90 + rng.nextDouble() * 10;
            }

            // Rare anomaly injection (realistic rates)

            // 1.5% chance speed anomaly (wind gust, emergency maneuver)
            if (rng.nextDouble() < 0.015) {
                speed = 42 + rng.nextDouble() * 10;  // 42-52 m/s
            }

            // 1% chance low battery (sensor fault or sudden drain)
            if (rng.nextDouble() < 0.01) {
                battery = 3 + rng.nextDouble() * 8;  // 3-11%
            }

            // 1% chance altitude anomaly (thermal updraft, control error)
            if (rng.nextDouble() < 0.01) {
                altitude = 360 + rng.nextDouble() * 80;  // 360-440m
            }
        }

        TelemetryDTO toDTO() {
            return new TelemetryDTO(
                    droneId,
                    Math.round(lat * 1e6) / 1e6,
                    Math.round(lon * 1e6) / 1e6,
                    Math.round(altitude * 10.0) / 10.0,
                    Math.round(speed * 10.0) / 10.0,
                    Math.round(battery * 10.0) / 10.0,
                    Instant.now().toString()
            );
        }
    }
}
