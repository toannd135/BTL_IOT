import { useEffect, useRef, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import { getLatestSensorAPI, getSensorChartAPI, getDevicesAPI, controlDeviceAPI } from '../services/api.service';
import { subscribeDeviceStatus } from '../services/ws.service';
import imgFreezing from '../assets/weather/freezing.jpg';
import imgCloudy from '../assets/weather/cloudy.jpg';
import imgClear from '../assets/weather/clear.jpg';
import imgHotSun from '../assets/weather/hotSun.jpg';
import imgDesert from '../assets/weather/desert.jpg';
import imgDryCracked from '../assets/weather/dryCracked.jpg';
import imgMistyForest from '../assets/weather/mistyForest.jpg';
import imgRain from '../assets/weather/rain.jpg';
import imgNightStars from '../assets/weather/nightStars.jpg';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

const SENSOR_TYPES = [
  { key: 'temperature', label: 'Temperature', unit: '°C' },
  { key: 'humidity', label: 'Humidity', unit: '%' },
  { key: 'light', label: 'Light', unit: 'lux' }
];

// Temperature (°C) and humidity (%) share the left axis; light (lux) uses its own right axis,
// otherwise the lux range (0-1000) flattens the other two series.
const CHART_META = {
  temperature: { label: 'Temperature (°C)', color: '#f5222d', bg: 'rgba(245,34,45,0.08)', axis: 'y' },
  humidity: { label: 'Humidity (%)', color: '#1677ff', bg: 'rgba(22,119,255,0.08)', axis: 'y' },
  light: { label: 'Light (lux)', color: '#faad14', bg: 'rgba(250,173,20,0.08)', axis: 'y1' }
};

/** A real device answers in a few ms - hold the loading state this long so it stays readable. */
const MIN_LOADING_MS = 1000;

const DEVICE_ICON_THEMES = [
  { bg: 'var(--danger-bg)', color: 'var(--danger)' },
  { bg: 'var(--primary-bg)', color: 'var(--primary)' },
  { bg: 'var(--gold-bg)', color: 'var(--gold)' }
];

function BulbIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 18h6" />
      <path d="M10 21h4" />
      <path d="M12 3a6 6 0 0 0-4 10.5c.6.6 1 1.4 1 2.5h6c0-1.1.4-1.9 1-2.5A6 6 0 0 0 12 3Z" />
    </svg>
  );
}

function SpinnerIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
      <circle cx="12" cy="12" r="9" opacity="0.25" />
      <path d="M21 12a9 9 0 0 0-9-9" />
    </svg>
  );
}

const pad2 = (n) => n.toString().padStart(2, '0');
const formatHMS = (iso) => {
  const d = new Date(iso);
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
};

const CONDITION_IMAGES = {
  temp_freezing: imgFreezing,
  temp_cool: imgCloudy,
  temp_mild: imgClear,
  temp_hot: imgHotSun,
  temp_extreme: imgDesert,
  hum_dry: imgDryCracked,
  hum_normal: imgClear,
  hum_humid: imgMistyForest,
  hum_wet: imgRain,
  light_dark: imgNightStars,
  light_dim: imgCloudy,
  light_bright: imgClear,
  light_intense: imgHotSun
};

function tempCondition(v) {
  if (v < 0)
    return { img: CONDITION_IMAGES.temp_freezing, label: 'Freezing' };
  if (v < 15) return { img: CONDITION_IMAGES.temp_cool, label: 'Cool' };
  if (v < 30) return { img: CONDITION_IMAGES.temp_mild, label: 'Mild' };
  if (v < 40) return { img: CONDITION_IMAGES.temp_hot, label: 'Hot' };
  return { img: CONDITION_IMAGES.temp_extreme, label: 'Extreme heat' };
}
function humidityCondition(v) {
  if (v < 30) return { img: CONDITION_IMAGES.hum_dry, label: 'Dry' };
  if (v < 60) return { img: CONDITION_IMAGES.hum_normal, label: 'Comfortable' };
  if (v < 80) return { img: CONDITION_IMAGES.hum_humid, label: 'Humid' };
  return { img: CONDITION_IMAGES.hum_wet, label: 'Very humid / rain' };
}
function lightCondition(v) {
  if (v < 100) return { img: CONDITION_IMAGES.light_dark, label: 'Dark / night' };
  if (v < 400) return { img: CONDITION_IMAGES.light_dim, label: 'Dim / overcast' };
  if (v < 700) return { img: CONDITION_IMAGES.light_bright, label: 'Bright daylight' };
  return { img: CONDITION_IMAGES.light_intense, label: 'Very bright' };
}
function conditionFor(key, v) {
  if (key === 'temperature') return tempCondition(v);
  if (key === 'humidity') return humidityCondition(v);
  return lightCondition(v);
}

function getOrCreateTooltipEl(chart) {
  let el = chart.canvas.parentNode.querySelector('.chart-tooltip');
  if (!el) {
    el = document.createElement('div');
    el.className = 'chart-tooltip';
    chart.canvas.parentNode.appendChild(el);
  }
  return el;
}

function renderChartTooltip(context) {
  const { chart, tooltip } = context;
  const el = getOrCreateTooltipEl(chart);

  if (tooltip.opacity === 0) {
    el.style.opacity = 0;
    return;
  }

  const rows = tooltip.dataPoints.map(dp => {
    const ds = dp.dataset;
    const cond = ds.sensorKey ? conditionFor(ds.sensorKey, dp.parsed.y) : null;
    return `
      <div class="chart-tooltip-row">
        <span class="chart-tooltip-dot" style="background:${ds.borderColor}"></span>
        <span class="chart-tooltip-name">${ds.label}</span>
        <span class="chart-tooltip-value">${dp.parsed.y}${ds.unit || ''}</span>
      </div>
      ${cond ? `<div class="chart-tooltip-condition">${cond.label}</div>` : ''}
    `;
  }).join('');

  el.innerHTML = `
    <div class="chart-tooltip-title">${tooltip.title?.[0] || ''}</div>
    ${rows}
  `;

  el.style.opacity = 1;
  el.style.left = tooltip.caretX + 'px';
  el.style.top = tooltip.caretY + 'px';
}

const Dashboard = () => {
  const [chartType, setChartType] = useState('all');
  const [isSpinning, setIsSpinning] = useState(false);
  const [liveValues, setLiveValues] = useState({ temperature: 0, humidity: 0, light: 0 });
  const [chartPoints, setChartPoints] = useState([]);
  const [devices, setDevices] = useState([]);

  const fetchLatest = async () => {
    try {
      const res = await getLatestSensorAPI();
      const d = res.data || {};
      setLiveValues({
        temperature: d.temperature ?? 0,
        humidity: d.humidity ?? 0,
        light: d.light ?? 0
      });
    } catch (err) {
      console.error('Failed to load latest sensor data:', err);
    }
  };

  // A device keeps its loading look for MIN_LOADING_MS; the real device usually answers within
  // milliseconds, so that status is buffered and applied when the hold ends.
  const loadingIds = useRef(new Set());
  const holdTimers = useRef({});
  const buffered = useRef({});

  const isLoading = (id) => loadingIds.current.has(id);

  const applyPatch = (id, patch) =>
    setDevices(prev => prev.map(d => d.id === id ? { ...d, ...patch } : d));

  const patchDevice = (id, patch) => {
    if (isLoading(id)) buffered.current[id] = { ...buffered.current[id], ...patch };
    else applyPatch(id, patch);
  };

  const startLoading = (id) => {
    loadingIds.current.add(id);
    clearTimeout(holdTimers.current[id]);
    holdTimers.current[id] = setTimeout(() => {
      loadingIds.current.delete(id);
      const patch = buffered.current[id];
      delete buffered.current[id];
      if (patch) applyPatch(id, patch);
    }, MIN_LOADING_MS);
  };

  useEffect(() => {
    const timers = holdTimers.current;
    return () => Object.values(timers).forEach(clearTimeout);
  }, []);

  const fetchDevices = async () => {
    try {
      const res = await getDevicesAPI();
      const fresh = res.data || [];
      // Don't let the poll end the loading state early.
      setDevices(prev => fresh.map(d => (isLoading(d.id) ? prev.find(p => p.id === d.id) || d : d)));
    } catch (err) {
      console.error('Failed to load devices:', err);
    }
  };

  const fetchChart = async (showSpinner = false) => {
    if (showSpinner) setIsSpinning(true);
    try {
      const params = chartType === 'all' ? {} : { type: chartType };
      const res = await getSensorChartAPI(params);
      setChartPoints(res.data || []);
    } catch (err) {
      console.error('Failed to load chart data:', err);
    } finally {
      if (showSpinner) setTimeout(() => setIsSpinning(false), 600);
    }
  };

  useEffect(() => {
    let cancelled = false;
    const tick = async () => {
      if (cancelled) return;
      await Promise.all([fetchLatest(), fetchDevices(), fetchChart(false)]);
    };
    tick();
    const interval = setInterval(tick, 5000);
    return () => { cancelled = true; clearInterval(interval); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chartType]);

  // Live device ON/OFF/PENDING/FAILED updates pushed the instant the real device answers
  // (or the pending-timeout job gives up) - no polling delay.
  const deviceIds = devices.map(d => d.id).join(',');
  useEffect(() => {
    if (!deviceIds) return undefined;
    const ids = deviceIds.split(',').map(Number);
    const unsubscribe = subscribeDeviceStatus(ids, (msg) => {
      const patch = {
        last_status: msg.status,
        last_action_id: msg.action_id,
        updated_at: msg.timestamp
      };
      if (msg.status === 'SUCCESS') patch.state = msg.action;
      patchDevice(msg.device_id, patch);
    });
    return unsubscribe;
    // patchDevice is intentionally not a dependency: it changes every render and would
    // tear the WebSocket down and back up each time.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deviceIds]);

  const toggleDevice = async (dev) => {
    if (dev.last_status === 'PENDING') return;
    const nextAction = dev.state === 'ON' ? 'OFF' : 'ON';
    startLoading(dev.id);
    applyPatch(dev.id, { last_status: 'PENDING' });
    try {
      await controlDeviceAPI(dev.id, nextAction);
    } catch (err) {
      console.error('Device control failed:', err);
      patchDevice(dev.id, { last_status: 'FAILED' });
    }
  };

  const reloadChart = () => {
    fetchChart(true);
  };

  const chartKeys = chartType === 'all' ? ['temperature', 'humidity', 'light'] : [chartType];

  const getDatasets = () => chartKeys.map(key => {
    const meta = CHART_META[key];
    const sensorType = SENSOR_TYPES.find(t => t.key === key);
    return {
      label: meta.label,
      data: chartPoints.map(p => p[key]),
      borderColor: meta.color,
      backgroundColor: meta.bg,
      borderWidth: 2,
      pointRadius: 2.5,
      pointBackgroundColor: meta.color,
      tension: 0.35,
      fill: chartKeys.length === 1,
      yAxisID: meta.axis,
      sensorKey: key,
      unit: sensorType ? sensorType.unit : ''
    };
  });

  const chartData = {
    labels: chartPoints.map(p => formatHMS(p.timestamp)),
    datasets: getDatasets()
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'nearest', intersect: true },
    elements: {
      point: {
        hitRadius: 6
      }
    },
    plugins: {
      legend: {
        display: chartType === 'all',
        position: 'top',
        align: 'end',
        labels: { boxWidth: 10, boxHeight: 10, font: { size: 11 } }
      },
      tooltip: {
        enabled: false,
        external: renderChartTooltip
      }
    },
    scales: {
      x: {
        ticks: {
          color: '#8c8c8c',
          font: { size: 10 },
          autoSkip: true,
          maxTicksLimit: 8,
          maxRotation: 0,
          minRotation: 0
        },
        grid: { color: '#f0f0f0' }
      },
      y: {
        display: chartKeys.some(k => CHART_META[k].axis === 'y'),
        position: 'left',
        min: 0,
        suggestedMax: 100,
        title: { display: chartType === 'all', text: '°C / %', color: '#8c8c8c', font: { size: 10 } },
        ticks: { color: '#8c8c8c', font: { size: 10 } },
        grid: { color: '#f0f0f0' }
      },
      y1: {
        display: chartKeys.some(k => CHART_META[k].axis === 'y1'),
        position: 'right',
        min: 0,
        grace: '10%',
        title: { display: chartType === 'all', text: 'lux', color: '#8c8c8c', font: { size: 10 } },
        ticks: { color: '#8c8c8c', font: { size: 10 } },
        grid: { drawOnChartArea: false }
      }
    }
  };

  return (
    <section className="page active" id="page-dashboard" style={{display: 'flex', flexDirection: 'column', gap: '16px', flex: 1, minHeight: 0}}>
      <div className="row row-3" id="statRow">
        {SENSOR_TYPES.map(t => {
          const val = liveValues[t.key] ?? 0;
          const cond = conditionFor(t.key, val);
          return (
            <div className="card stat-card" key={t.key}>
              <img className="stat-img-bg" src={cond.img} alt={cond.label} title={cond.label} />
              <div className="stat-overlay"></div>
              <div className="stat-content">
                <div className="stat-label">{t.label}</div>
                <div className="stat-value">{val}<span className="unit">{t.unit}</span></div>
                <div className="stat-condition">{cond.label}</div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="dash-top-row">
        <div className="card">
          <div className="card-head">
            Sensor Chart
            <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
              <select className="chart-type-select" value={chartType} onChange={e => setChartType(e.target.value)}>
                <option value="all">All sensors</option>
                <option value="temperature">Temperature</option>
                <option value="humidity">Humidity</option>
                <option value="light">Light</option>
              </select>
              <button className={`btn ${isSpinning ? 'spin' : ''}`} onClick={reloadChart}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21 12a9 9 0 1 1-3-6.7M21 4v5h-5" />
                </svg>
                Reload chart
              </button>
            </div>
          </div>
          <div className="card-body chart-body">
            <Line data={chartData} options={chartOptions} />
          </div>
        </div>

        <div className="card">
          <div className="card-head">Device Control</div>
          <div className="ctrl-panel-body" id="deviceCtrlPanel">
            {devices.map((dev, i) => {
              const on = dev.state === 'ON';
              const pending = dev.last_status === 'PENDING';
              const failed = dev.last_status === 'FAILED';
              const theme = DEVICE_ICON_THEMES[i % DEVICE_ICON_THEMES.length];
              return (
                <div className={`ctrl-item ${pending ? 'loading' : ''}`} key={dev.id}>
                  <div className="ctrl-left">
                    <div
                      className={`ctrl-icon ${pending ? 'spinning' : ''}`}
                      style={{
                        background: pending || on ? theme.bg : '#fafafa',
                        color: pending || on ? theme.color : 'var(--text-muted)'
                      }}
                    >
                      {pending ? <SpinnerIcon /> : <BulbIcon />}
                    </div>
                    <div>
                      <div className="ctrl-name">{dev.name}</div>
                      {pending ? (
                        <span className="tag pending">Pending</span>
                      ) : failed ? (
                        <span className="tag failed">Failed</span>
                      ) : (
                        <span className={`tag ${on ? 'on' : 'off'}`}>{on ? 'ON' : 'OFF'}</span>
                      )}
                    </div>
                  </div>
                  <label className="switch">
                    <input type="checkbox" checked={on} disabled={pending} onChange={() => toggleDevice(dev)} />
                    <span className="slider"></span>
                  </label>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
};

export default Dashboard;
