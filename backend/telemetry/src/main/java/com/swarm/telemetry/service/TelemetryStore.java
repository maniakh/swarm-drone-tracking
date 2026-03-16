package com.swarm.telemetry.service;

import com.swarm.telemetry.model.TelemetryDTO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TELEMETRY STORE - In-Memory Data Storage
 * ==========================================
 * Stores telemetry data in memory using ConcurrentHashMap for thread-safe access.
 * This is used instead of constantly reading from the database for performance.
 *
 * Data structure:
 *   latest  → Map<droneId, TelemetryDTO>         : Most recent data for each drone
 *   history → Map<droneId, List<TelemetryDTO>>    : Last 100 data points per drone
 *
 * Why in-memory instead of database?
 *   - Telemetry data comes every 3 seconds per drone
 *   - Dashboard polls every 2 seconds
 *   - Database I/O would be too slow for this frequency
 *   - We only need recent data for the dashboard (last 100 readings)
 *
 * Thread safety:
 *   ConcurrentHashMap and synchronizedList ensure safe access from multiple threads
 *   (simulator thread + REST API threads can access simultaneously)
 */
@Service
public class TelemetryStore {

    // Most recent telemetry reading for each drone (used by GET /telemetry/latest)
    private final Map<String, TelemetryDTO> latest = new ConcurrentHashMap<>();

    // Historical telemetry readings per drone (used by GET /telemetry/{droneId})
    private final Map<String, List<TelemetryDTO>> history = new ConcurrentHashMap<>();

    // Maximum history entries per drone (to prevent memory overflow)
    private static final int MAX_HISTORY = 100;

    /**
     * Store a new telemetry data point.
     * Called by TelemetrySimulator every 3 seconds for each drone.
     *
     * @param data The telemetry DTO to store
     */
    public void store(TelemetryDTO data) {
        // Update the "latest" map (overwrites previous value for this drone)
        latest.put(data.getDroneId(), data);

        // Add to history list (creates list if first entry for this drone)
        history.computeIfAbsent(data.getDroneId(), k -> Collections.synchronizedList(new ArrayList<>()));
        List<TelemetryDTO> list = history.get(data.getDroneId());
        list.add(data);

        // Trim history if it exceeds MAX_HISTORY (keep only last 100 entries)
        if (list.size() > MAX_HISTORY) {
            list.subList(0, list.size() - MAX_HISTORY).clear();
        }
    }

    /**
     * Get the latest telemetry for ALL drones.
     * Used by: GET /telemetry/latest (called by dashboard every 2 seconds)
     */
    public Collection<TelemetryDTO> getLatestAll() {
        return latest.values();
    }

    /**
     * Get the latest telemetry for a specific drone.
     * Used by: GET /telemetry/{droneId}/latest
     */
    public TelemetryDTO getLatest(String droneId) {
        return latest.get(droneId);
    }

    /**
     * Get historical telemetry data for a specific drone.
     * Used by: GET /telemetry/{droneId}?limit=20
     *
     * @param droneId The drone to get history for
     * @param limit   Maximum number of entries to return
     * @return List of recent telemetry data points
     */
    public List<TelemetryDTO> getHistory(String droneId, int limit) {
        List<TelemetryDTO> list = history.get(droneId);
        if (list == null) return Collections.emptyList();
        int from = Math.max(0, list.size() - limit);
        return new ArrayList<>(list.subList(from, list.size()));
    }
}
