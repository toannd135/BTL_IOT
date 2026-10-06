import { useEffect, useState } from 'react';
import { getActionHistoryAPI } from '../services/api.service';
import { useFitPageSize } from '../hooks/useFitPageSize';

const Th = ({ col, label, field, dir, onSort }) => (
  <th data-col={col} onClick={() => onSort(col)} className={field === col ? 'sorted' : ''}>
    {label} <span className="arrow">{field === col ? (dir === 'desc' ? '▼' : '▲') : ''}</span>
  </th>
);

const History = () => {
  const [historyLog, setHistoryLog] = useState([]);
  const [page, setPage] = useState(1);
  const [field, setField] = useState('time');
  const [dir, setDir] = useState('desc');
  const [deviceFilter, setDeviceFilter] = useState('all');
  const [actionFilter, setActionFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [timeSearch, setTimeSearch] = useState('');
  const [rowsInput, setRowsInput] = useState('');

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const res = await getActionHistoryAPI({ limit: 1000, sort_by: 'time', sort_dir: 'desc' });
        if (cancelled) return;
        const rows = (res.data || []).map(a => ({
          id: String(a.id),
          device: a.device_name,
          action: a.action,
          performedBy: a.username,
          status: (a.status || '').toLowerCase(),
          time: a.time
        }));
        setHistoryLog(rows);
      } catch (err) {
        console.error('Failed to load action history:', err);
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

  const tq = timeSearch.trim().toLowerCase();

  let filtered = historyLog.filter(row => {
    if (deviceFilter !== 'all' && row.device !== deviceFilter) return false;
    if (actionFilter !== 'all' && row.action !== actionFilter) return false;
    if (statusFilter !== 'all' && row.status !== statusFilter) return false;
    if (tq && !fmtDate(row.time).toLowerCase().includes(tq)) return false;
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
    <section className="page active" id="page-history">
      <div className="filter-bar">
        <div className="field">
          <input
            type="text"
            placeholder="Search by time"
            value={timeSearch}
            onChange={e => {setTimeSearch(e.target.value); setPage(1);}}
            title="Search by time"
          />
        </div>
        <div className="field">
          <select value={deviceFilter} onChange={e => {setDeviceFilter(e.target.value); setPage(1);}}>
            <option value="all">All devices</option>
            <option value="LED 1">LED 1</option>
            <option value="LED 2">LED 2</option>
            <option value="LED 3">LED 3</option>
          </select>
        </div>
        <div className="field">
          <select value={actionFilter} onChange={e => {setActionFilter(e.target.value); setPage(1);}}>
            <option value="all">All actions</option>
            <option value="ON">ON</option>
            <option value="OFF">OFF</option>
          </select>
        </div>
        <div className="field">
          <select value={statusFilter} onChange={e => {setStatusFilter(e.target.value); setPage(1);}}>
            <option value="all">All status</option>
            <option value="success">Success</option>
            <option value="failed">Failed</option>
            <option value="pending">Pending</option>
            <option value="loading">Loading</option>
          </select>
        </div>
      </div>
      <div className="card" ref={cardRef}>
        <table>
          <thead>
            <tr>
              <Th col="id" label="ID" field={field} dir={dir} onSort={handleSort} />
              <Th col="device" label="Device Name" field={field} dir={dir} onSort={handleSort} />
              <Th col="performedBy" label="User ID" field={field} dir={dir} onSort={handleSort} />
              <Th col="action" label="Action" field={field} dir={dir} onSort={handleSort} />
              <Th col="status" label="Status" field={field} dir={dir} onSort={handleSort} />
              <Th col="time" label="Time" field={field} dir={dir} onSort={handleSort} />
            </tr>
          </thead>
          <tbody>
            {pageRows.length > 0 ? pageRows.map(row => (
              <tr key={row.id}>
                <td className="mono">{row.id}</td>
                <td>{row.device}</td>
                <td className="mono">{row.performedBy}</td>
                <td><span className={`tag ${row.action === 'ON' ? 'on' : 'off'}`}>{row.action}</span></td>
                <td><span className={`tag ${row.status}`}>{row.status.charAt(0).toUpperCase() + row.status.slice(1)}</span></td>
                <td className="mono">{fmtDate(row.time)}</td>
              </tr>
            )) : <tr className="empty"><td colSpan="6">No matching data</td></tr>}
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

export default History;
