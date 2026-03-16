package com.swarm.telemetry.service;

import com.swarm.telemetry.model.TelemetryDTO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelemetryStore {

    private final Map<String, List<TelemetryDTO>> history = new ConcurrentHashMap<>();
    private final Map<String, TelemetryDTO> latest = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 100;

    public void store(TelemetryDTO data) {
        latest.put(data.getDroneId(), data);
        history.computeIfAbsent(data.getDroneId(), k -> Collections.synchronizedList(new ArrayList<>()));
        List<TelemetryDTO> list = history.get(data.getDroneId());
        list.add(data);
        if (list.size() > MAX_HISTORY) {
            list.subList(0, list.size() - MAX_HISTORY).clear();
        }
    }

    public Collection<TelemetryDTO> getLatestAll() {
        return latest.values();
    }

    public TelemetryDTO getLatest(String droneId) {
        return latest.get(droneId);
    }

    public List<TelemetryDTO> getHistory(String droneId, int limit) {
        List<TelemetryDTO> list = history.get(droneId);
        if (list == null) return Collections.emptyList();
        int from = Math.max(0, list.size() - limit);
        return new ArrayList<>(list.subList(from, list.size()));
    }
}
