-- Swarm Drone Tracking - Database Initialization

CREATE TABLE IF NOT EXISTS telemetry (
    id SERIAL PRIMARY KEY,
    drone_id VARCHAR(50) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    altitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION NOT NULL,
    battery DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telemetry_drone_id ON telemetry(drone_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_timestamp ON telemetry(timestamp);

CREATE TABLE IF NOT EXISTS drones (
    id SERIAL PRIMARY KEY,
    drone_id VARCHAR(50) UNIQUE NOT NULL,
    model VARCHAR(100) NOT NULL,
    mission VARCHAR(200),
    team VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    max_speed DOUBLE PRECISION DEFAULT 40.0,
    max_altitude DOUBLE PRECISION DEFAULT 350.0,
    battery_capacity DOUBLE PRECISION DEFAULT 100.0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS alerts (
    id SERIAL PRIMARY KEY,
    drone_id VARCHAR(50) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT,
    value DOUBLE PRECISION,
    threshold DOUBLE PRECISION,
    acknowledged BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alerts_drone_id ON alerts(drone_id);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON alerts(severity);
