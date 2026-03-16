# 🚁 Swarm Drone Tracking & DevOps Microservices

A full-scale microservices project that monitors swarm UAV/drone telemetry data, detects anomalies, generates alerts, and visualizes everything through an interactive dashboard.

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Java](https://img.shields.io/badge/Java_17-Spring_Boot_3-6DB33F?logo=spring)
![React](https://img.shields.io/badge/React-Dashboard-61DAFB?logo=react)
![Prometheus](https://img.shields.io/badge/Prometheus-Monitoring-E6522C?logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-Dashboard-F46800?logo=grafana)

## 📋 Project Overview

| Feature | Description |
|---------|-------------|
| 🛰️ Telemetry Simulation | Real-time position, speed, altitude, battery data for 5 drones |
| 🔍 Anomaly Detection | Speed limit, low battery, altitude limit, geo-fence violation |
| 🔔 Alert System | Console/webhook alert notifications with severity levels |
| 🗺️ Dashboard | React + Leaflet interactive map & monitoring panel (FlightRadar-style) |
| 📊 Monitoring | Prometheus + Grafana + Loki observability stack |
| 🚀 CI/CD | GitHub Actions automated build/test/deploy pipeline |

## 🏗️ Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Telemetry   │────▶│   Anomaly    │────▶│    Alert     │
│   Service    │     │  Detection   │     │   Service    │
│ (Spring Boot)│     │ (Spring Boot)│     │ (Spring Boot)│
│  Port: 8001  │     │  Port: 8003  │     │  Port: 8004  │
└──────┬───────┘     └──────────────┘     └──────────────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐
│  PostgreSQL  │◀────│ Drone Mgmt   │
│   Database   │     │ (Spring Boot)│
│  Port: 5432  │     │  Port: 8002  │
└──────────────┘     └──────────────┘

┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Dashboard   │     │  Prometheus  │     │   Grafana    │
│   (React)    │     │  Port: 9090  │     │  Port: 3001  │
│  Port: 3000  │     └──────────────┘     └──────────────┘
└──────────────┘     ┌──────────────┐
                     │     Loki     │
                     │  Port: 3100  │
                     └──────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Git

### Run with a Single Command

```bash
# Clone the repository
git clone https://github.com/<username>/swarm-drone-tracking.git
cd swarm-drone-tracking

# Start the entire system
docker compose up -d --build

# Watch the logs
docker compose logs -f
```

### Service Access

| Service | URL | Description |
|---------|-----|-------------|
| 🗺️ Dashboard | http://localhost:3000 | Main monitoring panel |
| 🛰️ Telemetry API | http://localhost:8001 | Telemetry service |
| 🚁 Drone Mgmt API | http://localhost:8002 | Drone management service |
| 🔍 Anomaly API | http://localhost:8003 | Anomaly detection service |
| 🔔 Alert API | http://localhost:8004 | Alert service |
| 📊 Grafana | http://localhost:3001 | Monitoring dashboard (admin/admin) |
| 📈 Prometheus | http://localhost:9090 | Metrics collection |
| 📝 Loki | http://localhost:3100 | Log aggregation |

## 📁 Project Structure

```
swarm-drone-tracking/
├── backend/
│   ├── telemetry/          # Java + Spring Boot - Telemetry service
│   ├── drone-mgmt/         # Java + Spring Boot - Drone management
│   ├── anomaly/            # Java + Spring Boot - Anomaly detection
│   └── alert/              # Java + Spring Boot - Alert service
├── frontend/
│   └── dashboard/          # React + Leaflet - Monitoring panel
├── infra/
│   ├── docker/             # DB init scripts
│   ├── prometheus/         # Prometheus config
│   ├── grafana/            # Grafana dashboards & datasources
│   └── loki/               # Loki config
├── .github/
│   └── workflows/          # CI/CD pipeline
├── docker-compose.yml      # Single-file deployment
└── README.md
```

## 🔧 API Examples

### Send Telemetry Data
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
    "timestamp": "2026-03-15T12:00:00Z"
  }'
```

### Get Latest Telemetry
```bash
curl http://localhost:8001/telemetry/latest
```

### List Anomalies
```bash
curl http://localhost:8003/anomalies?limit=20
```

### List Alerts
```bash
curl http://localhost:8004/alerts?limit=50
```

### Drone List
```bash
curl http://localhost:8002/api/drones
```

## 📊 Anomaly Rules

| Rule | Threshold | Severity |
|------|-----------|----------|
| Speed Exceeded | > 40 m/s | Medium / High |
| Low Battery | < 15% | High / Critical |
| Altitude Exceeded | > 350m | High |
| Too Low Altitude | < 5m | Medium |
| Geo-Fence Violation | Outside defined zone | Critical |

## 🛑 Stop the System

```bash
docker compose down

# Also remove volumes (clean data)
docker compose down -v
```

## 🧪 Tests

```bash
# Build and test Java services
cd backend/telemetry && mvn clean package
cd backend/anomaly && mvn clean package
cd backend/alert && mvn clean package
cd backend/drone-mgmt && mvn clean package
```

## 🔔 Alert Configuration

For Slack or webhook integration, update `docker-compose.yml`:

```yaml
alert:
  environment:
    SLACK_WEBHOOK_URL: "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
    WEBHOOK_URL: "https://your-webhook-endpoint.com"
```

## 📈 Grafana

Access Grafana at `http://localhost:3001`:
- **Username:** admin
- **Password:** admin
- A pre-configured "Swarm Drone Monitoring" dashboard is available.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

MIT License - See [LICENSE](LICENSE) file for details.
