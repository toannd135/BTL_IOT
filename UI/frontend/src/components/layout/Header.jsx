import { useLocation } from 'react-router-dom';

const Header = () => {
  const location = useLocation();
  let title = 'Dashboard';
  if (location.pathname.includes('/datasensor')) title = 'Sensor Data';
  else if (location.pathname.includes('/history')) title = 'Device History';
  else if (location.pathname.includes('/profile')) title = 'Profile';

  return (
    <div className="header">
      <div className="header-title">
        <span className="header-title-bar"></span>
        <span id="headerTitle">{title}</span>
      </div>
    </div>
  );
};

export default Header;
