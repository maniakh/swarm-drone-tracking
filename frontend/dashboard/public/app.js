/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║           SWARM TRACKER — LIVE DRONE MONITORING DASHBOARD       ║
 * ╠═══════════════════════════════════════════════════════════════════╣
 * ║  A FlightRadar24-style real-time dashboard built with:          ║
 * ║    • React 18 (loaded via CDN, no npm/webpack needed)           ║
 * ║    • Leaflet.js for interactive map rendering                    ║
 * ║    • Dark theme with glassmorphism UI design                     ║
 * ║                                                                   ║
 * ║  How it works:                                                    ║
 * ║    1. Every 2 seconds, fetches data from 3 backend APIs          ║
 * ║    2. Updates drone markers on the Leaflet map                    ║
 * ║    3. Shows alerts, statistics, and drone details                 ║
 * ║                                                                   ║
 * ║  API calls made:                                                  ║
 * ║    GET http://localhost:8001/telemetry/latest  → drone positions  ║
 * ║    GET http://localhost:8004/alerts?limit=100  → active alerts    ║
 * ║    GET http://localhost:8003/anomalies/stats   → statistics       ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 */

// ─── React hooks (loaded from CDN, not npm) ───
const { useState, useEffect, useCallback, useRef } = React;

// ─── Backend API URLs (these match docker-compose port mappings) ───
const TELEMETRY_URL = 'http://localhost:8001';   // Telemetry Service
const ALERT_URL = 'http://localhost:8004';       // Alert Service
const ANOMALY_URL = 'http://localhost:8003';     // Anomaly Detection Service

// ─── Severity level configuration (icon, color, label for each level) ───
const SEV_CONFIG = {
  critical: { icon: '🚨', color: '#ef4444', label: 'CRITICAL' },
  high:     { icon: '🔴', color: '#f97316', label: 'HIGH' },
  medium:   { icon: '🟡', color: '#eab308', label: 'MEDIUM' },
  low:      { icon: '🔵', color: '#3b82f6', label: 'LOW' },
};

// ─── Anomaly type labels (displayed in alert cards) ───
const TYPE_LABELS = {
  SPEED_EXCEEDED: 'Speed Exceeded', LOW_BATTERY: 'Low Battery',
  ALTITUDE_EXCEEDED: 'Altitude Exceeded', ALTITUDE_TOO_LOW: 'Low Altitude',
  GEO_FENCE_VIOLATION: 'Geo-Fence Violation',
};

// ─── Colors for each anomaly type (used in statistics charts) ───
const TYPE_COLORS = {
  SPEED_EXCEEDED: '#f97316', LOW_BATTERY: '#ef4444',
  ALTITUDE_EXCEEDED: '#a855f7', ALTITUDE_TOO_LOW: '#eab308',
  GEO_FENCE_VIOLATION: '#ec4899',
};

/* ═══════════════════════════════════════════════════
   LEAFLET MAP COMPONENT
   ═══════════════════════════════════════════════════
   Renders an interactive map using Leaflet.js with:
   - Dark-themed map tiles (CartoDB Dark Matter)
   - Drone markers with battery percentage display
   - Geo-fence boundary circle
   - Click-to-select drone interaction
   - Auto-fly-to when a drone is selected
*/
function LeafletMap({ drones, alerts, selectedDrone, onSelect }) {
  const mapRef = useRef(null);        // Reference to the Leaflet map instance
  const markersRef = useRef({});      // References to drone markers (keyed by droneId)
  const trailsRef = useRef({});       // Flight trail coordinates per drone

  // ─── Initialize the map (runs once on mount) ───
  useEffect(() => {
    if (mapRef.current) return;  // Already initialized

    // Create Leaflet map centered on Istanbul
    const map = L.map('leaflet-map', {
      zoomControl: true, attributionControl: false,
    }).setView([41.015, 28.98], 12);  // Istanbul coordinates, zoom level 12

    // Add dark-themed map tiles from CartoDB
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
    }).addTo(map);

    // Draw geo-fence boundary (the allowed flight area)
    L.circle([41.05, 29.0], {
      radius: 25000,                  // 25km radius
      color: '#38bdf855',             // Semi-transparent blue border
      fillColor: '#38bdf8',
      fillOpacity: 0.02,              // Nearly transparent fill
      weight: 1,
      dashArray: '10,6',              // Dashed line style
    }).addTo(map);

    mapRef.current = map;
  }, []);

  // ─── Update drone markers whenever data changes ───
  useEffect(() => {
    if (!mapRef.current) return;
    const map = mapRef.current;

    drones.forEach(drone => {
      const isSel = selectedDrone === drone.droneId;  // Is this drone selected?

      // Battery color: green (>50%), yellow (20-50%), red (<20%)
      const batColor = drone.battery > 50 ? '#22c55e' : drone.battery > 20 ? '#eab308' : '#ef4444';

      // Check if this drone has any active alerts
      const hasAnomaly = alerts.some(a => a.droneId === drone.droneId);

      // Glow effect: red glow for anomaly, blue glow for selected
      const ring = hasAnomaly ? `box-shadow:0 0 0 3px ${hasAnomaly ? '#ef444466' : 'transparent'}, 0 0 ${isSel ? 20 : 10}px ${isSel ? '#38bdf866' : '#38bdf822'};` : `box-shadow:0 0 ${isSel ? 20 : 10}px ${isSel ? '#38bdf866' : '#38bdf822'};`;
      const sz = isSel ? 44 : 34;  // Selected drones are larger

      // Create custom HTML icon for the drone marker
      const icon = L.divIcon({
        className: '',
        html: `<div style="
          width:${sz}px;height:${sz}px;
          background:radial-gradient(circle at 30% 30%, #1e3a5f, #0f172a);
          border:2px solid ${isSel ? '#38bdf8' : hasAnomaly ? '#ef4444' : '#334155'};
          border-radius:50%;display:flex;align-items:center;justify-content:center;
          font-size:${isSel ? 20 : 15}px;${ring}position:relative;
          transition:all 0.3s;
        ">✈️
          <div style="position:absolute;bottom:-6px;left:50%;transform:translateX(-50%);
            background:#0f172a;border:1px solid ${batColor}33;border-radius:3px;
            padding:0px 4px;font-size:9px;font-weight:700;color:${batColor};
            font-family:'JetBrains Mono',monospace;white-space:nowrap;
          ">${Math.round(drone.battery)}%</div>
          ${isSel ? `<div style="position:absolute;top:-8px;left:50%;transform:translateX(-50%);
            background:#38bdf8;border-radius:3px;padding:0 5px;
            font-size:8px;font-weight:800;color:#0f172a;white-space:nowrap;
            font-family:'JetBrains Mono',monospace;
          ">${drone.droneId}</div>` : ''}
        </div>`,
        iconSize: [sz, sz + 12],
        iconAnchor: [sz / 2, sz / 2],
      });

      // Update existing marker or create a new one
      if (markersRef.current[drone.droneId]) {
        // Marker exists → update position and icon
        markersRef.current[drone.droneId].setLatLng([drone.lat, drone.lon]);
        markersRef.current[drone.droneId].setIcon(icon);
      } else {
        // First time → create marker and add click handler
        const marker = L.marker([drone.lat, drone.lon], { icon })
          .addTo(map)
          .on('click', () => onSelect(drone.droneId));
        markersRef.current[drone.droneId] = marker;
      }

      // Track flight trail (last 30 positions)
      if (!trailsRef.current[drone.droneId]) {
        trailsRef.current[drone.droneId] = [];
      }
      const trail = trailsRef.current[drone.droneId];
      trail.push([drone.lat, drone.lon]);
      if (trail.length > 30) trail.shift();
    });
  }, [drones, alerts, selectedDrone, onSelect]);

  // ─── Fly to selected drone when selection changes ───
  useEffect(() => {
    if (!mapRef.current || !selectedDrone) return;
    const drone = drones.find(d => d.droneId === selectedDrone);
    if (drone) mapRef.current.flyTo([drone.lat, drone.lon], 14, { duration: 0.8 });
  }, [selectedDrone]);

  return <div id="leaflet-map"></div>;
}

/* ═══════════════════════════════════════════════════
   MAIN APP COMPONENT
   ═══════════════════════════════════════════════════
   The root component that orchestrates the entire dashboard:
   - Fetches data from all 3 backend services every 2 seconds
   - Renders the top bar, map, side panels, and bottom bar
   - Manages view state (Live Map / Alerts / Statistics)
   - Handles drone selection
*/
function App() {
  // ─── State variables ───
  const [drones, setDrones] = useState([]);           // Current drone positions
  const [alerts, setAlerts] = useState([]);            // Active alerts
  const [stats, setStats] = useState(null);            // Anomaly statistics
  const [selectedDrone, setSelectedDrone] = useState(null); // Currently selected drone ID
  const [view, setView] = useState('live');            // Current view: 'live', 'alerts', or 'stats'
  const [connected, setConnected] = useState(false);   // Backend connection status
  const [time, setTime] = useState('');                // Current time display
  const [showLeft, setShowLeft] = useState(true);      // Left panel visibility

  /**
   * Fetch data from all 3 backend services simultaneously.
   * Uses Promise.allSettled() so that if one service is down, others still work.
   * Called every 2 seconds via setInterval.
   */
  const fetchData = useCallback(async () => {
    try {
      const [tRes, aRes, sRes] = await Promise.allSettled([
        fetch(`${TELEMETRY_URL}/telemetry/latest`),  // Drone positions
        fetch(`${ALERT_URL}/alerts?limit=100`),       // Alerts
        fetch(`${ANOMALY_URL}/anomalies/stats`),      // Statistics
      ]);

      // Update drones if telemetry service responded OK
      if (tRes.status === 'fulfilled' && tRes.value.ok) { setDrones(await tRes.value.json()); setConnected(true); }
      else setConnected(false);

      // Update alerts (reverse to show newest first)
      if (aRes.status === 'fulfilled' && aRes.value.ok) setAlerts((await aRes.value.json()).reverse());

      // Update statistics
      if (sRes.status === 'fulfilled' && sRes.value.ok) setStats(await sRes.value.json());
    } catch { setConnected(false); }
  }, []);

  // ─── Set up polling interval and clock ───
  useEffect(() => {
    fetchData();  // Initial fetch
    const iv = setInterval(fetchData, 2000);  // Poll every 2 seconds
    const clock = setInterval(() => setTime(new Date().toLocaleTimeString('en-US')), 1000);
    return () => { clearInterval(iv); clearInterval(clock); };
  }, [fetchData]);

  // ─── Computed values ───
  const selDrone = drones.find(d => d.droneId === selectedDrone);  // Selected drone data
  const selAlerts = alerts.filter(a => a.droneId === selectedDrone); // Alerts for selected drone
  const batClass = b => b > 50 ? 'green' : b > 20 ? 'yellow' : 'red';
  const spdClass = s => s > 40 ? 'red' : s > 30 ? 'yellow' : 'green';
  const fmtTime = ts => { try { return new Date(ts).toLocaleTimeString('en-US'); } catch { return ts; } };
  const totalAnomalies = stats?.total || 0;
  const unackAlerts = alerts.filter(a => !a.acknowledged).length;

  return (
    <div className="app">
      {/* ═══ TOP BAR — Brand name, live stats, clock ═══ */}
      <div className="topbar">
        <div className="topbar-left">
          <div className="brand">
            <div className="brand-icon">✈️</div>
            <span className="brand-text">SWARM TRACKER</span>
          </div>
          <div className="topbar-stats">
            <div className="top-stat">
              <span className="dot dot-green"></span>
              <span className="top-stat-val">{drones.length}</span> drones
            </div>
            <div className="top-stat">
              <span className="dot dot-yellow"></span>
              <span className="top-stat-val">{totalAnomalies}</span> anomalies
            </div>
            <div className="top-stat">
              <span className="dot dot-red"></span>
              <span className="top-stat-val">{unackAlerts}</span> alerts
            </div>
          </div>
        </div>
        <div className="topbar-right">
          <div className="live-badge"><span className="live-dot"></span> LIVE</div>
          <div className="time-display">{time}</div>
        </div>
      </div>

      {/* ═══ VIEW TOGGLE — Switch between Live Map, Alerts, Statistics ═══ */}
      <div className="toggle-btns">
        <button className={`toggle-btn ${view === 'live' ? 'active' : ''}`} onClick={() => setView('live')}>🗺️ Live Map</button>
        <button className={`toggle-btn ${view === 'alerts' ? 'active' : ''}`} onClick={() => setView('alerts')}>🔔 Alerts</button>
        <button className={`toggle-btn ${view === 'stats' ? 'active' : ''}`} onClick={() => setView('stats')}>📊 Statistics</button>
      </div>

      {/* ═══ MAP — Full-screen Leaflet map (always rendered, behind panels) ═══ */}
      <div className="map-full">
        <LeafletMap drones={drones} alerts={alerts} selectedDrone={selectedDrone} onSelect={id => setSelectedDrone(id === selectedDrone ? null : id)} />
      </div>

      {/* ═══ LEFT PANEL — Drone list + Recent alerts (visible in Live view) ═══ */}
      {view === 'live' && (
        <div className={`left-panel ${showLeft ? '' : 'hidden'}`}>
          {/* Drone list header */}
          <div className="panel-header">
            <span className="panel-title">Active Drones</span>
            <span className="panel-count">{drones.length} / {drones.length}</span>
          </div>

          {/* Drone list - each item shows ID, position, battery, speed */}
          {drones.map(d => (
            <div key={d.droneId}
              className={`drone-item ${selectedDrone === d.droneId ? 'selected' : ''}`}
              onClick={() => setSelectedDrone(selectedDrone === d.droneId ? null : d.droneId)}
            >
              <div className="drone-icon-wrap">✈️</div>
              <div className="drone-info">
                <div className="drone-name">{d.droneId}</div>
                <div className="drone-sub">{d.lat.toFixed(4)}, {d.lon.toFixed(4)} · {d.altitude.toFixed(0)}m</div>
              </div>
              <div className="drone-right">
                <div className={`drone-bat`} style={{color: d.battery > 50 ? '#22c55e' : d.battery > 20 ? '#eab308' : '#ef4444'}}>
                  {d.battery.toFixed(0)}%
                </div>
                <div className="drone-bat-bar">
                  <div className="drone-bat-fill" style={{width:`${d.battery}%`, background: d.battery > 50 ? '#22c55e' : d.battery > 20 ? '#eab308' : '#ef4444'}}></div>
                </div>
                <div className="drone-speed" style={{color: d.speed > 40 ? '#ef4444' : '#64748b'}}>{d.speed.toFixed(1)} m/s</div>
              </div>
            </div>
          ))}

          {/* Recent alerts section */}
          <div className="panel-header" style={{marginTop: 8}}>
            <span className="panel-title">Recent Alerts</span>
            <span className="panel-count">{alerts.length}</span>
          </div>
          {alerts.slice(0, 10).map((a, i) => {
            const s = SEV_CONFIG[a.severity] || SEV_CONFIG.low;
            return (
              <div key={a.id || i} className="alert-item" style={{margin:'0 8px 0 8px'}}>
                <div className="alert-sev" style={{background: s.color}}></div>
                <div className="alert-body">
                  <div className="alert-type-label" style={{color: s.color}}>
                    {TYPE_LABELS[a.type] || a.type} <span style={{opacity:0.5, fontWeight:400}}>· {a.droneId}</span>
                  </div>
                  <div className="alert-msg">{a.message}</div>
                  <div className="alert-meta">{fmtTime(a.timestamp)}</div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ═══ DETAIL PANEL (right side) — Shows when a drone is selected ═══ */}
      {selDrone && view === 'live' && (
        <div className="detail-panel">
          <div className="detail-header">
            <div>
              <div className="detail-drone-id">✈️ {selDrone.droneId}</div>
              <div className="detail-drone-model">Active · Istanbul Region</div>
            </div>
            <button className="close-btn" onClick={() => setSelectedDrone(null)}>✕</button>
          </div>

          {/* 2x3 grid showing all telemetry values */}
          <div className="detail-grid">
            <div className="detail-cell">
              <div className="detail-label">Latitude</div>
              <div className="detail-value" style={{color:'var(--accent)', fontSize:15}}>{selDrone.lat.toFixed(5)}</div>
            </div>
            <div className="detail-cell">
              <div className="detail-label">Longitude</div>
              <div className="detail-value" style={{color:'var(--accent)', fontSize:15}}>{selDrone.lon.toFixed(5)}</div>
            </div>
            <div className="detail-cell">
              <div className="detail-label">Altitude</div>
              <div className="detail-value" style={{color: selDrone.altitude > 350 ? 'var(--red)' : 'var(--text-primary)'}}>
                {selDrone.altitude.toFixed(0)}<span className="detail-unit">m</span>
              </div>
            </div>
            <div className="detail-cell">
              <div className="detail-label">Speed</div>
              <div className="detail-value" style={{color: selDrone.speed > 40 ? 'var(--red)' : selDrone.speed > 30 ? 'var(--yellow)' : 'var(--green)'}}>
                {selDrone.speed.toFixed(1)}<span className="detail-unit">m/s</span>
              </div>
            </div>
            <div className="detail-cell">
              <div className="detail-label">Battery</div>
              <div className="detail-value" style={{color: selDrone.battery > 50 ? 'var(--green)' : selDrone.battery > 20 ? 'var(--yellow)' : 'var(--red)'}}>
                {selDrone.battery.toFixed(0)}<span className="detail-unit">%</span>
              </div>
            </div>
            <div className="detail-cell">
              <div className="detail-label">Last Update</div>
              <div className="detail-value" style={{fontSize:13, color:'var(--text-secondary)'}}>{fmtTime(selDrone.timestamp)}</div>
            </div>
          </div>

          {/* Alerts specific to this drone */}
          <div className="detail-section">
            <div className="detail-section-title">🔔 Drone Alerts ({selAlerts.length})</div>
            {selAlerts.length === 0 && <div style={{fontSize:12,color:'var(--text-muted)',textAlign:'center',padding:16}}>No alerts for this drone ✅</div>}
            {selAlerts.slice(0, 8).map((a, i) => {
              const s = SEV_CONFIG[a.severity] || SEV_CONFIG.low;
              return (
                <div key={a.id || i} className="alert-item">
                  <div className="alert-sev" style={{background: s.color}}></div>
                  <div className="alert-body">
                    <div className="alert-type-label" style={{color: s.color}}>{TYPE_LABELS[a.type] || a.type}</div>
                    <div className="alert-msg">{a.message}</div>
                    <div className="alert-meta">{fmtTime(a.timestamp)} · Value: {a.value?.toFixed(1)}</div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ═══ ALERTS FULL VIEW — Shows all alerts in a modal overlay ═══ */}
      {view === 'alerts' && (
        <div className="filter-panel">
          <div className="filter-card">
            <div className="filter-title">🔔 All Alerts <span style={{fontSize:14,color:'var(--text-muted)',fontWeight:400}}>({alerts.length} records)</span></div>
            <div style={{maxHeight:'60vh', overflowY:'auto'}}>
              {alerts.map((a, i) => {
                const s = SEV_CONFIG[a.severity] || SEV_CONFIG.low;
                return (
                  <div key={a.id || i} className="alert-item">
                    <div className="alert-sev" style={{background: s.color}}></div>
                    <div className="alert-body">
                      <div className="alert-type-label" style={{color: s.color}}>
                        {s.icon} {TYPE_LABELS[a.type] || a.type}
                        <span style={{opacity:0.5, fontWeight:400, marginLeft:8}}>{a.droneId}</span>
                      </div>
                      <div className="alert-msg">{a.message}</div>
                      <div className="alert-meta">{fmtTime(a.timestamp)} · Value: {a.value?.toFixed(1)} · Threshold: {a.threshold}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* ═══ STATISTICS VIEW — System-wide statistics with charts ═══ */}
      {view === 'stats' && (
        <div className="filter-panel">
          <div className="filter-card">
            <div className="filter-title">📊 System Statistics</div>

            {/* Summary cards */}
            <div className="stats-row">
              <div className="mini-stat"><div className="mini-stat-val accent">{drones.length}</div><div className="mini-stat-label">Active Drones</div></div>
              <div className="mini-stat"><div className="mini-stat-val yellow">{totalAnomalies}</div><div className="mini-stat-label">Total Anomalies</div></div>
              <div className="mini-stat"><div className="mini-stat-val red">{unackAlerts}</div><div className="mini-stat-label">Active Alerts</div></div>
              <div className="mini-stat"><div className="mini-stat-val green">{drones.length > 0 ? (drones.reduce((s,d) => s+d.battery, 0)/drones.length).toFixed(0) : 0}%</div><div className="mini-stat-label">Avg. Battery</div></div>
              <div className="mini-stat"><div className="mini-stat-val" style={{color: '#f97316'}}>{drones.length > 0 ? (drones.reduce((s,d) => s+d.speed, 0)/drones.length).toFixed(1) : 0}</div><div className="mini-stat-label">Avg. Speed (m/s)</div></div>
              <div className="mini-stat"><div className="mini-stat-val" style={{color: '#a855f7'}}>{drones.length > 0 ? (drones.reduce((s,d) => s+d.altitude, 0)/drones.length).toFixed(0) : 0}m</div><div className="mini-stat-label">Avg. Altitude</div></div>
            </div>

            {/* Bar charts: Anomaly Type + Severity breakdown */}
            <div className="chart-row">
              <div className="chart-box">
                <div className="chart-box-title">📊 By Anomaly Type</div>
                {Object.entries(stats?.by_type || {}).map(([t, c]) => {
                  const mx = Math.max(...Object.values(stats?.by_type || {}), 1);
                  return (
                    <div className="bar-row" key={t}>
                      <span className="bar-label">{TYPE_LABELS[t] || t}</span>
                      <div className="bar-track"><div className="bar-fill" style={{width:`${(c/mx)*100}%`,background:TYPE_COLORS[t]||'#6366f1'}}>{c}</div></div>
                    </div>
                  );
                })}
              </div>
              <div className="chart-box">
                <div className="chart-box-title">🎯 By Severity</div>
                {Object.entries(stats?.by_severity || {}).map(([s, c]) => {
                  const mx = Math.max(...Object.values(stats?.by_severity || {}), 1);
                  const colors = {critical:'#ef4444',high:'#f97316',medium:'#eab308',low:'#3b82f6'};
                  return (
                    <div className="bar-row" key={s}>
                      <span className="bar-label">{s.toUpperCase()}</span>
                      <div className="bar-track"><div className="bar-fill" style={{width:`${(c/mx)*100}%`,background:colors[s]||'#6366f1'}}>{c}</div></div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Battery status chart per drone */}
            <div className="chart-box">
              <div className="chart-box-title">🔋 Drone Battery Status</div>
              {drones.map(d => (
                <div className="bar-row" key={d.droneId}>
                  <span className="bar-label">{d.droneId}</span>
                  <div className="bar-track">
                    <div className="bar-fill" style={{width:`${d.battery}%`,background:d.battery>50?'#22c55e':d.battery>20?'#eab308':'#ef4444'}}>
                      {d.battery.toFixed(0)}%
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ═══ BOTTOM BAR — Quick status for each drone ═══ */}
      <div className="bottom-bar">
        {drones.map(d => (
          <div key={d.droneId} className="bottom-stat"
            style={{cursor:'pointer', borderColor: selectedDrone === d.droneId ? 'var(--accent)' : undefined}}
            onClick={() => setSelectedDrone(selectedDrone === d.droneId ? null : d.droneId)}
          >
            <span className="bottom-stat-icon">✈️</span>
            <span className="bottom-stat-val">{d.droneId}</span>
            <span style={{color: d.speed > 40 ? 'var(--red)' : 'var(--text-muted)', fontFamily:'JetBrains Mono', fontSize:10}}>{d.speed.toFixed(0)}m/s</span>
            <span style={{color: d.battery > 50 ? 'var(--green)' : d.battery > 20 ? 'var(--yellow)' : 'var(--red)', fontFamily:'JetBrains Mono', fontSize:10, fontWeight:700}}>{d.battery.toFixed(0)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Mount the React app to the DOM ───
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
