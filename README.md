# Swarm Drone Tracking & DevOps Microservices

A full-scale **microservices** project that simulates swarm drone flights, detects anomalies in real-time, generates alerts, and visualizes everything through an interactive FlightRadar-style dashboard. Includes complete **DevOps** infrastructure with monitoring, logging, and CI/CD.

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Java](https://img.shields.io/badge/Java_17-Spring_Boot_3-6DB33F?logo=spring)
![React](https://img.shields.io/badge/React-Dashboard-61DAFB?logo=react)
![Prometheus](https://img.shields.io/badge/Prometheus-Monitoring-E6522C?logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-Dashboard-F46800?logo=grafana)

---

## Screenshots

### Live Map — Real-Time Drone Tracking
![Dashboard Live Map](docs/screenshots/dashboard-live-map.png)

### Alerts Panel — Anomaly Notifications
![Dashboard Alerts](docs/screenshots/dashboard-alerts.png)

### Statistics Panel — System Overview
![Dashboard Statistics](docs/screenshots/dashboard-statistics.png)

### Grafana — Service Monitoring Dashboard
![Grafana Dashboard](docs/screenshots/grafana-dashboard.png)

### Prometheus — Metrics Targets
![Prometheus Targets](docs/screenshots/prometheus-targets.png)

---

## What Does This Project Do?

This project simulates **5 drones** flying over Istanbul and provides a complete monitoring system:

1. **Telemetry Service** generates realistic drone data (GPS, speed, altitude, battery) every 3 seconds
2. **Anomaly Detection Service** analyzes each data point against rules (speed limit, battery level, altitude, geo-fence)
3. **Alert Service** stores detected anomalies as alerts for operator review
4. **Dashboard** shows everything on a real-time interactive map
5. **Prometheus + Grafana** monitors the health of all services

> **One command deploys everything:** `docker compose up -d --build`

---

## System Architecture

```
                    ┌─────────────────┐
                    │   Dashboard     │
                    │  (React+Leaflet)│ ← Operator sees drone positions, alerts, statistics
                    │   Port: 3000    │
                    └────────┬────────┘
                             │ polls every 2 seconds
          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Telemetry     │ │    Anomaly      │ │     Alert       │
│    Service      │→│   Detection     │→│    Service      │
│  (Spring Boot)  │ │  (Spring Boot)  │ │  (Spring Boot)  │
│   Port: 8001    │ │   Port: 8003    │ │   Port: 8004    │
└────────┬────────┘ └─────────────────┘ └─────────────────┘
         │               generates             stores
         │  simulates     data every           alerts
         │  5 drones      3 seconds
         ▼
┌─────────────────┐ ┌─────────────────┐
│   PostgreSQL    │◄│  Drone Mgmt     │
│   Database      │ │  (Spring Boot)  │ ← Manages drone metadata (model, team, mission)
│   Port: 5432    │ │   Port: 8002    │
└─────────────────┘ └─────────────────┘

┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Prometheus    │ │    Grafana      │ │      Loki       │
│   Port: 9090    │ │   Port: 3001    │ │   Port: 3100    │
│  (scrapes all   │ │  (visualizes    │ │  (aggregates    │
│   service       │ │   metrics)      │ │   logs)         │
│   metrics)      │ │                 │ │                 │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## Data Flow (Step by Step)

```
Step 1: TelemetrySimulator generates fake drone data (position, speed, battery)
        ↓
Step 2: Data is stored in TelemetryStore (in-memory) and sent to Anomaly Service
        ↓
Step 3: AnomalyDetectionService checks rules:
        • Speed > 40 m/s?       → SPEED_EXCEEDED
        • Battery < 15%?        → LOW_BATTERY
        • Altitude > 350m?      → ALTITUDE_EXCEEDED
        • Outside geo-fence?    → GEO_FENCE_VIOLATION
        ↓
Step 4: Detected anomalies are forwarded to Alert Service as alerts
        ↓
Step 5: Dashboard polls all 3 APIs every 2 seconds and updates the map
```

---

## Quick Start

### Prerequisites
- **Docker Desktop** (includes Docker & Docker Compose)
- **Git**

### Deploy Everything with One Command

```bash
# 1. Clone the repository
git clone https://github.com/maniakh/swarm-drone-tracking.git
cd swarm-drone-tracking

# 2. Build and start all 9 services
docker compose up -d --build

# 3. Wait ~60 seconds for Java services to start, then open:
#    Dashboard: http://localhost:3000
#    Grafana:   http://localhost:3001 (admin/admin)
```

### Check Service Status
```bash
# See all running containers
docker compose ps

# Watch logs from all services
docker compose logs -f

# Watch logs from a specific service
docker compose logs -f telemetry
```

---

## Service Access

| Service | URL | Description |
|---------|-----|-------------|
| Dashboard | http://localhost:3000 | Main monitoring panel (FlightRadar-style) |
| Telemetry API | http://localhost:8001 | Drone telemetry data |
| Drone Mgmt API | http://localhost:8002 | Drone metadata CRUD |
| Anomaly API | http://localhost:8003 | Anomaly detection & stats |
| Alert API | http://localhost:8004 | Alert management |
| Grafana | http://localhost:3001 | Monitoring dashboards (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics collection |
| Loki | http://localhost:3100 | Log aggregation |

---

## Project Structure

```
swarm-drone-tracking/
├── backend/
│   ├── telemetry/                    # Telemetry Service (Java Spring Boot)
│   │   ├── src/main/java/.../
│   │   │   ├── TelemetryApplication.java     # Main entry point
│   │   │   ├── controller/
│   │   │   │   └── TelemetryController.java  # REST API endpoints
│   │   │   ├── service/
│   │   │   │   ├── TelemetrySimulator.java   # Drone simulation engine
│   │   │   │   └── TelemetryStore.java       # In-memory data storage
│   │   │   └── model/
│   │   │       ├── TelemetryData.java        # Database entity
│   │   │       └── TelemetryDTO.java         # Data transfer object
│   │   ├── src/main/resources/
│   │   │   └── application.yml               # Service configuration
│   │   ├── pom.xml                           # Maven dependencies
│   │   └── Dockerfile
│   │
│   ├── anomaly/                      # Anomaly Detection Service
│   │   ├── src/main/java/.../
│   │   │   ├── AnomalyApplication.java
│   │   │   ├── controller/AnomalyController.java
│   │   │   ├── service/AnomalyDetectionService.java  # Rule-based detection engine
│   │   │   └── model/
│   │   │       ├── AnomalyDTO.java
│   │   │       └── TelemetryDTO.java
│   │   └── ...
│   │
│   ├── alert/                        # Alert Service
│   │   ├── src/main/java/.../
│   │   │   ├── AlertApplication.java
│   │   │   ├── controller/AlertController.java
│   │   │   ├── service/AlertService.java
│   │   │   └── model/AlertDTO.java
│   │   └── ...
│   │
│   └── drone-mgmt/                  # Drone Management Service
│       ├── src/main/java/.../
│       │   ├── DroneMgmtApplication.java
│       │   ├── controller/
│       │   │   ├── DroneController.java      # Full CRUD REST API
│       │   │   └── HealthController.java
│       │   ├── service/DroneService.java
│       │   ├── repository/DroneRepository.java  # Spring Data JPA
│       │   └── model/Drone.java              # JPA Entity
│       └── ...
│
├── frontend/
│   └── dashboard/                    # React Dashboard (CDN-based, no npm needed)
│       ├── public/
│       │   ├── index.html            # HTML entry point
│       │   ├── app.js                # React components (map, panels, charts)
│       │   └── style.css             # Dark theme + glassmorphism styles
│       ├── nginx.conf                # Nginx configuration for serving static files
│       └── Dockerfile
│
├── infra/
│   ├── docker/
│   │   └── init.sql                  # PostgreSQL initialization script
│   ├── prometheus/
│   │   ├── prometheus.yml            # Scrape configuration for all services
│   │   └── alert_rules.yml           # System-level alert rules
│   ├── grafana/
│   │   └── provisioning/
│   │       ├── datasources/datasources.yml    # Auto-configure Prometheus + Loki
│   │       └── dashboards/
│   │           ├── dashboards.yml             # Dashboard provisioning config
│   │           └── drone-dashboard.json       # Pre-built monitoring dashboard
│   └── loki/
│       └── loki-config.yml           # Loki log aggregation config
│
├── .github/
│   └── workflows/
│       └── ci-cd.yml                 # GitHub Actions CI/CD pipeline
│
├── docker-compose.yml                # Single-file deployment for all 9 services
├── .env.example                     # Environment variables template (copy to .env)
├── .gitignore
└── README.md
```

---

## API Examples

### Get Live Drone Positions
```bash
curl http://localhost:8001/telemetry/latest
```

### Get Anomaly Statistics
```bash
curl http://localhost:8003/anomalies/stats
```

### List Recent Alerts
```bash
curl http://localhost:8004/alerts?limit=20
```

### Get All Registered Drones
```bash
curl http://localhost:8002/api/drones
```

### Send Manual Telemetry Data
```bash
curl -X POST http://localhost:8001/telemetry \
  -H "Content-Type: application/json" \
  -d '{
    "droneId": "DR-001",
    "lat": 41.01,
    "lon": 28.97,
    "altitude": 120,
    "speed": 12.3,
    "battery": 78,
    "timestamp": "2026-03-16T10:00:00Z"
  }'
```

---

## Anomaly Detection Rules

| Rule | Condition | Severity | Example |
|------|-----------|----------|---------|
| Speed Exceeded | speed > 40 m/s | medium (>40) / high (>60) | Wind gust pushes drone to 45 m/s |
| Low Battery | battery < 15% | high (<15%) / critical (<5%) | Sensor fault reports 3% battery |
| Altitude Exceeded | altitude > 350m | high | Thermal updraft lifts drone to 400m |
| Too Low Altitude | altitude < 5m | medium | Control error drops drone to 2m |
| Geo-Fence Violation | outside Istanbul area | critical | Drone drifts outside allowed zone |

---

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Backend | Java 17 + Spring Boot 3 | All 4 microservices |
| Database | PostgreSQL 16 | Drone metadata storage |
| Frontend | React 18 + Leaflet.js | Interactive map dashboard |
| Metrics | Prometheus | Time-series metrics collection |
| Dashboards | Grafana | Monitoring visualization |
| Logs | Loki | Centralized log aggregation |
| Containers | Docker + Docker Compose | Deployment & orchestration |
| CI/CD | GitHub Actions | Automated build & test pipeline |

---

## Stop the System

```bash
# Stop all services (keep data)
docker compose down

# Stop and remove all data (clean restart)
docker compose down -v
```

---

## Build & Test (Without Docker)

```bash
# Build individual Java services with Maven
cd backend/telemetry && mvn clean package
cd backend/anomaly && mvn clean package
cd backend/alert && mvn clean package
cd backend/drone-mgmt && mvn clean package
```

---

## CI/CD Pipeline (GitHub Actions)

The project includes a GitHub Actions workflow (`.github/workflows/ci-cd.yml`) that runs on every push:

1. **Checkout** → Clone the repository
2. **Java Setup** → Install JDK 17
3. **Build** → `mvn clean package` for each Java service
4. **Docker Build** → Build Docker images for all services
5. **Deploy** → Ready for deployment

---

## Grafana Access

1. Open http://localhost:3001
2. Login: **admin** / **admin**
3. Navigate to "Swarm Drone Monitoring" dashboard
4. Pre-configured panels show: Active Drones, Anomaly Count, CPU/Memory, HTTP Requests

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

MIT License - See [LICENSE](LICENSE) file for details.
