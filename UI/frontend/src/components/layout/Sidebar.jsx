import { NavLink } from 'react-router-dom';

const Sidebar = ({ isCollapsed, toggleCollapse }) => {
  return (
    <div className={`sider ${isCollapsed ? 'collapsed' : ''}`} id="sider">
      <div className="logo">
        <span className="logo-text logo-text-full">IoT Monitor System</span>
        <span className="logo-text logo-text-short">ITM</span>
        <div className={`nav-toggle ${isCollapsed ? 'collapsed' : ''}`} id="trigger" onClick={toggleCollapse}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </div>
      </div>
      <ul className="menu" id="menu">
        <NavLink to="/" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`} end>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M3 13h4v8H3zM10 3h4v18h-4zM17 8h4v13h-4z" />
          </svg>
          <span className="menu-label">Dashboard</span>
        </NavLink>
        <NavLink to="/datasensor" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
            <ellipse cx="12" cy="5" rx="8" ry="3" />
            <path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3" />
          </svg>
          <span className="menu-label">Sensor Data</span>
        </NavLink>
        <NavLink to="/history" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M3 12a9 9 0 1 0 3-6.7" />
            <path d="M3 4v5h5M12 8v4l3 2" />
          </svg>
          <span className="menu-label">History</span>
        </NavLink>
      </ul>
      <NavLink to="/profile" className={({ isActive }) => `account ${isActive ? 'active' : ''}`} id="accountBlock">
        <div className="account-avatar">T</div>
        <div className="account-meta">
          <div className="account-name">Nguyễn Đức Toàn</div>
          <div className="account-role">toannd.b23cn831@ptit.edu.vn</div>
        </div>
      </NavLink>
    </div>
  );
};

export default Sidebar;
