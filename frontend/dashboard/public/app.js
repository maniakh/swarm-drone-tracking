const { useState, useEffect, useCallback, useRef } = React;

const TELEMETRY_URL = 'http://localhost:8001';
const ALERT_URL = 'http://localhost:8004';
const ANOMALY_URL = 'http://localhost:8003';

const SEV_CONFIG = {
  critical: { icon: '🚨', color: '#ef4444', label: 'CRITICAL' },
  high:     { icon: '🔴', color: '#f97316', label: 'HIGH' },
  medium:   { icon: '🟡', color: '#eab308', label: 'MEDIUM' },
  low:      { icon: '🔵', color: '#3b82f6', label: 'LOW' },
};
const TYPE_LABELS = {
  SPEED_EXCEEDED: 'Speed Exceeded', LOW_BATTERY: 'Low Battery',
  ALTITUDE_EXCEEDED: 'Altitude Exceeded', ALTITUDE_TOO_LOW: 'Low Altitude',
  GEO_FENCE_VIOLATION: 'Geo-Fence Violation',
};
const TYPE_COLORS = {
  SPEED_EXCEEDED: '#f97316', LOW_BATTERY: '#ef4444',
  ALTITUDE_EXCEEDED: '#a855f7', ALTITUDE_TOO_LOW: '#eab308',
  GEO_FENCE_VIOLATION: '#ec4899',
};

/* ═══════════ LEAFLET MAP ═══════════ */
function LeafletMap({ drones, alerts, selectedDrone, onSelect }) {
  const mapRef = useRef(null);
  const markersRef = useRef({});
  const trailsRef = useRef({});

  useEffect(() => {
    if (mapRef.current) return;
    const map = L.map('leaflet-map', {
      zoomControl: true, attributionControl: false,
    }).setView([41.015, 28.98], 12);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
    }).addTo(map);

    // Geo-fence
    L.circle([41.05, 29.0], {
      radius: 25000, color: '#38bdf855', fillColor: '#38bdf8',
      fillOpacity: 0.02, weight: 1, dashArray: '10,6',
    }).addTo(map);

    mapRef.current = map;
  }, []);

  useEffect(() => {
    if (!mapRef.current) return;
    const map = mapRef.current;

    drones.forEach(drone => {
      const isSel = selectedDrone === drone.droneId;
      const batColor = drone.battery > 50 ? '#22c55e' : drone.battery > 20 ? '#eab308' : '#ef4444';
      const hasAnomaly = alerts.some(a => a.droneId === drone.droneId);
      const ring = hasAnomaly ? `box-shadow:0 0 0 3px ${hasAnomaly ? '#ef444466' : 'transparent'}, 0 0 ${isSel ? 20 : 10}px ${isSel ? '#38bdf866' : '#38bdf822'};` : `box-shadow:0 0 ${isSel ? 20 : 10}px ${isSel ? '#38bdf866' : '#38bdf822'};`;
      const sz = isSel ? 44 : 34;

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

      if (markersRef.current[drone.droneId]) {
        markersRef.current[drone.droneId].setLatLng([drone.lat, drone.lon]);
        markersRef.current[drone.droneId].setIcon(icon);
      } else {
        const marker = L.marker([drone.lat, drone.lon], { icon })
          .addTo(map)
          .on('click', () => onSelect(drone.droneId));
        markersRef.current[drone.droneId] = marker;
      }

      // Trail line
      if (!trailsRef.current[drone.droneId]) {
        trailsRef.current[drone.droneId] = [];
      }
      const trail = trailsRef.current[drone.droneId];
      trail.push([drone.lat, drone.lon]);
      if (trail.length > 30) trail.shift();
    });
  }, [drones, alerts, selectedDrone, onSelect]);

  useEffect(() => {
    if (!mapRef.current || !selectedDrone) return;
    const drone = drones.find(d => d.droneId === selectedDrone);
    if (drone) mapRef.current.flyTo([drone.lat, drone.lon], 14, { duration: 0.8 });
  }, [selectedDrone]);

  return <div id="leaflet-map"></div>;
}

/* ═══════════ APP ═══════════ */
function App() {
  const [drones, setDrones] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [stats, setStats] = useState(null);
  const [selectedDrone, setSelectedDrone] = useState(null);
  const [view, setView] = useState('live');  // live, stats, alerts
  const [connected, setConnected] = useState(false);
  const [time, setTime] = useState('');
  const [showLeft, setShowLeft] = useState(true);

  const fetchData = useCallback(async () => {
    try {
      const [tRes, aRes, sRes] = await Promise.allSettled([
        fetch(`${TELEMETRY_URL}/telemetry/latest`),
        fetch(`${ALERT_URL}/alerts?limit=100`),
        fetch(`${ANOMALY_URL}/anomalies/stats`),
      ]);
      if (tRes.status === 'fulfilled' && tRes.value.ok) { setDrones(await tRes.value.json()); setConnected(true); }
      else setConnected(false);
      if (aRes.status === 'fulfilled' && aRes.value.ok) setAlerts((await aRes.value.json()).reverse());
      if (sRes.status === 'fulfilled' && sRes.value.ok) setStats(await sRes.value.json());
    } catch { setConnected(false); }
  }, []);

  useEffect(() => {
    fetchData();
    const iv = setInterval(fetchData, 2000);
    const clock = setInterval(() => setTime(new Date().toLocaleTimeString('en-US')), 1000);
    return () => { clearInterval(iv); clearInterval(clock); };
  }, [fetchData]);

  const selDrone = drones.find(d => d.droneId === selectedDrone);
  const selAlerts = alerts.filter(a => a.droneId === selectedDrone);
  const batClass = b => b > 50 ? 'green' : b > 20 ? 'yellow' : 'red';
  const spdClass = s => s > 40 ? 'red' : s > 30 ? 'yellow' : 'green';
  const fmtTime = ts => { try { return new Date(ts).toLocaleTimeString('en-US'); } catch { return ts; } };
  const totalAnomalies = stats?.total || 0;
  const unackAlerts = alerts.filter(a => !a.acknowledged).length;

  return (
    <div className="app">
      {/* ═══ TOP BAR ═══ */}
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

      {/* ═══ TOGGLE BUTTONS ═══ */}
      <div className="toggle-btns">
        <button className={`toggle-btn ${view === 'live' ? 'active' : ''}`} onClick={() => setView('live')}>🗺️ Live Map</button>
        <button className={`toggle-btn ${view === 'alerts' ? 'active' : ''}`} onClick={() => setView('alerts')}>🔔 Alerts</button>
        <button className={`toggle-btn ${view === 'stats' ? 'active' : ''}`} onClick={() => setView('stats')}>📊 Statistics</button>
      </div>

      {/* ═══ MAP ═══ */}
      <div className="map-full">
        <LeafletMap drones={drones} alerts={alerts} selectedDrone={selectedDrone} onSelect={id => setSelectedDrone(id === selectedDrone ? null : id)} />
      </div>

      {/* ═══ LEFT PANEL - Drone List ═══ */}
      {view === 'live' && (
        <div className={`left-panel ${showLeft ? '' : 'hidden'}`}>
          <div className="panel-header">
            <span className="panel-title">Active Drones</span>
            <span className="panel-count">{drones.length} / {drones.length}</span>
          </div>
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

          {/* Recent alerts in left panel */}
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

      {/* ═══ DETAIL PANEL (right) ═══ */}
      {selDrone && view === 'live' && (
        <div className="detail-panel">
          <div className="detail-header">
            <div>
              <div className="detail-drone-id">✈️ {selDrone.droneId}</div>
              <div className="detail-drone-model">Active · Istanbul Region</div>
            </div>
            <button className="close-btn" onClick={() => setSelectedDrone(null)}>✕</button>
          </div>

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

      {/* ═══ ALERTS FULL VIEW ═══ */}
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

      {/* ═══ STATS VIEW ═══ */}
      {view === 'stats' && (
        <div className="filter-panel">
          <div className="filter-card">
            <div className="filter-title">📊 System Statistics</div>
            <div className="stats-row">
              <div className="mini-stat"><div className="mini-stat-val accent">{drones.length}</div><div className="mini-stat-label">Active Drones</div></div>
              <div className="mini-stat"><div className="mini-stat-val yellow">{totalAnomalies}</div><div className="mini-stat-label">Total Anomalies</div></div>
              <div className="mini-stat"><div className="mini-stat-val red">{unackAlerts}</div><div className="mini-stat-label">Active Alerts</div></div>
              <div className="mini-stat"><div className="mini-stat-val green">{drones.length > 0 ? (drones.reduce((s,d) => s+d.battery, 0)/drones.length).toFixed(0) : 0}%</div><div className="mini-stat-label">Avg. Battery</div></div>
              <div className="mini-stat"><div className="mini-stat-val" style={{color: '#f97316'}}>{drones.length > 0 ? (drones.reduce((s,d) => s+d.speed, 0)/drones.length).toFixed(1) : 0}</div><div className="mini-stat-label">Avg. Speed (m/s)</div></div>
              <div className="mini-stat"><div className="mini-stat-val" style={{color: '#a855f7'}}>{drones.length > 0 ? (drones.reduce((s,d) => s+d.altitude, 0)/drones.length).toFixed(0) : 0}m</div><div className="mini-stat-label">Avg. Altitude</div></div>
            </div>

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

      {/* ═══ BOTTOM BAR ═══ */}
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

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
