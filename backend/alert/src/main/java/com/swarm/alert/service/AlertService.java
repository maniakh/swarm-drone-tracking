package com.swarm.alert.service;

import com.swarm.alert.model.AlertDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final MeterRegistry meterRegistry;
    private final List<AlertDTO> alerts = new CopyOnWriteArrayList<>();
    private Counter alertCounter;

    private static final int MAX_ALERTS = 500;

    public AlertService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        alertCounter = Counter.builder("alerts_generated_total")
                .description("Total alerts generated").register(meterRegistry);
    }

    public AlertDTO createAlert(AlertDTO alert) {
        if (alert.getId() == null || alert.getId().isEmpty()) {
            alert.setId(UUID.randomUUID().toString().substring(0, 8));
        }
        alerts.add(alert);
        alertCounter.increment();

        // Log alert
        System.out.printf("[ALERT] %s | %s | %s | %s%n",
                alert.getSeverity().toUpperCase(),
                alert.getDroneId(), alert.getType(), alert.getMessage());

        // Trim
        while (alerts.size() > MAX_ALERTS) {
            alerts.remove(0);
        }

        return alert;
    }

    public List<AlertDTO> getAlerts(int limit, String droneId, String severity) {
        List<AlertDTO> filtered = new ArrayList<>(alerts);

        if (droneId != null && !droneId.isEmpty()) {
            filtered = filtered.stream()
                    .filter(a -> a.getDroneId().equals(droneId))
                    .collect(Collectors.toList());
        }
        if (severity != null && !severity.isEmpty()) {
            filtered = filtered.stream()
                    .filter(a -> a.getSeverity().equalsIgnoreCase(severity))
                    .collect(Collectors.toList());
        }

        int from = Math.max(0, filtered.size() - limit);
        return new ArrayList<>(filtered.subList(from, filtered.size()));
    }

    public boolean acknowledgeAlert(String alertId) {
        for (AlertDTO a : alerts) {
            if (a.getId().equals(alertId)) {
                a.setAcknowledged(true);
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> getStats() {
        Map<String, Long> byType = alerts.stream()
                .collect(Collectors.groupingBy(AlertDTO::getType, Collectors.counting()));
        Map<String, Long> bySev = alerts.stream()
                .collect(Collectors.groupingBy(AlertDTO::getSeverity, Collectors.counting()));
        long unack = alerts.stream().filter(a -> !a.isAcknowledged()).count();

        return Map.of(
                "total", alerts.size(),
                "unacknowledged", unack,
                "by_type", byType,
                "by_severity", bySev
        );
    }
}
