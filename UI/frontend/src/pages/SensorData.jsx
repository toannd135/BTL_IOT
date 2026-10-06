import { useEffect, useState } from 'react';
import { getSensorDataAPI } from '../services/api.service';
import { useFitPageSize } from '../hooks/useFitPageSize';

const UNITS = { temperature: '°C', humidity: '%', light: 'lux' };

const Th = ({ col, label, field, dir, onSort }) => (
  <th data-col={col} onClick={() => onSort(col)} className={field === col ? 'sorted' : ''}>
    {label} <span className="arrow">{field === col ? (dir === 'desc' ? '▼' : '▲') : ''}</span>
  </th>
);

const SensorData = () => {
  const [sensorLog, setSensorLog] = useState([]);
  const [page, setPage] = useState(1);
  const [field, setField] = useState('time');
  const [dir, setDir] = useState('desc');
  const [typeFilter, setTypeFilter] = useState('all');
  const [search, setSearch] = useState('');
  const [rowsInput, setRowsInput] = useState('');

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const res = await getSensorDataAPI({ limit: 1000, sort_by: 'time', sort_dir: 'desc' });
        if (cancelled) return;
        const rows = (res.data || []).map(d => ({
          id: String(d.id),
          type: d.sensor_name,
          value: `${d.value}${UNITS[d.sensor_name] ? ' ' + UNITS[d.sensor_name] : ''}`,
          time: d.time
        }));
        setSensorLog(rows);
      } catch (err) {
        console.error('Failed to load sensor data:', err);
      }
    };
    load();
    const interval = setInterval(load, 10000);
    return () => { cancelled = true; clearInterval(interval); };
  }, []);

  const pad = (n) => n.toString().padStart(2, '0');

  const fmtDate = (ds) => {
    const d = new Date(ds);
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };

  const compareRows = (a, b, f) => {
    if (f === 'time') return new Date(a.time) - new Date(b.time);
    const av = a[f], bv = b[f];
    if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv);
    return (av > bv) - (av < bv);
  };

  const handleSort = (col) => {
    if (field === col) {
      setDir(dir === 'desc' ? 'asc' : 'desc');
    } else {
      setField(col);
      setDir(col === 'time' ? 'desc' : 'asc');
    }
    setPage(1);
  };

  let filtered = sensorLog.filter(row => {
    if (typeFilter !== 'all' && typeFilter !== 'time' && row.type !== typeFilter) return false;
    if (search) {
      if (typeFilter === 'time') {
        if (!fmtDate(row.time).toLowerCase().includes(search)) return false;
      } else if (!(row.id.toLowerCase().includes(search) || row.value.toLowerCase().includes(search))) {
        return false;
      }
    }
    return true;
  });

  filtered.sort((a, b) => dir === 'desc' ? compareRows(b, a, field) : compareRows(a, b, field));

  const [cardRef, autoPageSize] = useFitPageSize([filtered.length]);
  const PAGE_SIZE = rowsInput ? Number(rowsInput) : autoPageSize;

  const handleRowsInputChange = (val) => {
    if (val === '') {
      setRowsInput('');
    } else {
      const n = Math.max(1, parseInt(val, 10) || 1);
      setRowsInput(String(n));
    }
    setPage(1);
  };

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));

  if (page > totalPages && totalPages > 0) setPage(totalPages);

  const start = (page - 1) * PAGE_SIZE;
  const pageRows = filtered.slice(start, start + PAGE_SIZE);

  return (
    <section className="page active" id="page-datasensor">
      <div className="filter-bar">
        <div className="field">
          <input type="text"
            placeholder={typeFilter === 'time' ? 'Search by time...' : 'Search by ID, value...'}
            value={search}
            onChange={e => {setSearch(e.target.value.trim().toLowerCase()); setPage(1);}}
          />
        </div>
        <div className="field">
          <select value={typeFilter} onChange={e => {setTypeFilter(e.target.value); setSearch(''); setPage(1);}}>
            <option value="all">All sensor types</option>
            <option value="temperature">Temperature</option>
            <option value="humidity">Humidity</option>
            <option value="light">Light</option>
            <option value="time">Time</option>
          </select>
        </div>
      </div>
      <div className="card" ref={cardRef}>
        <table>
          <thead>
            <tr>
              <Th col="id" label="ID" field={field} dir={dir} onSort={handleSort} />
              <Th col="type" label="Sensor Type" field={field} dir={dir} onSort={handleSort} />
              <Th col="value" label="Value" field={field} dir={dir} onSort={handleSort} />
              <Th col="time" label="Time" field={field} dir={dir} onSort={handleSort} />
            </tr>
          </thead>
          <tbody>
            {pageRows.length > 0 ? pageRows.map(row => (
              <tr key={row.id}>
                <td className="mono">{row.id}</td>
                <td><span className={`tag ${row.type}`}>{row.type}</span></td>
                <td className="mono">{row.value}</td>
                <td className="mono">{fmtDate(row.time)}</td>
              </tr>
            )) : <tr className="empty"><td colSpan="4">No matching data</td></tr>}
          </tbody>
        </table>
        <div className="table-foot">
          <div className="table-count">
            {filtered.length === 0 ? '0 results' : `Showing ${start + 1}-${Math.min(start + PAGE_SIZE, filtered.length)} of ${filtered.length} results`}
          </div>
          <div className="rows-per-page">
            <label htmlFor="rowsPerPage">Rows/page</label>
            <input
              id="rowsPerPage"
              type="number"
              min="1"
              placeholder={String(autoPageSize)}
              value={rowsInput}
              onChange={e => handleRowsInputChange(e.target.value)}
            />
          </div>
          <div className="pager">
            <button className="page-btn" disabled={page === 1} onClick={() => setPage(page-1)}>‹</button>
            {Array.from({length: totalPages}, (_, i) => (
              <button key={i+1} className={`page-btn ${page === i+1 ? 'active' : ''}`} onClick={() => setPage(i+1)}>{i+1}</button>
            ))}
            <button className="page-btn" disabled={page === totalPages || totalPages === 0} onClick={() => setPage(page+1)}>›</button>
          </div>
        </div>
      </div>
    </section>
  );
};

export default SensorData;
