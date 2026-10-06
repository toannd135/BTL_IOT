const Profile = () => {
  return (
    <section className="page active" id="page-profile">
      <div className="profile-grid">
        <div className="card profile-card">
          <div className="avatar">T<span className="avatar-status" title="Online"></span></div>
          <div className="profile-name" style={{marginBottom: '2px'}}>Nguyễn Đức Toàn</div>
          <div className="profile-role">IoT &amp; Embedded Developer</div>
          <div style={{width: '100%', display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '24px'}}>
            <button className="btn btn-primary" style={{width: '100%', justifyContent: 'center', fontWeight: 500}}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{width: '13px', height: '13px', marginRight: '2px'}}>
                <path d="M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
              </svg>
              Edit Profile
            </button>
            <button className="btn" style={{width: '100%', justifyContent: 'center', fontWeight: 500}}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{width: '13px', height: '13px', marginRight: '2px'}}>
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              Change Password
            </button>
          </div>
        </div>
        <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
          <div className="card">
            <div className="card-head">About Me</div>
            <div className="card-pad" style={{fontSize: '13.5px', lineHeight: 1.6, color: 'var(--text-secondary)'}}>
              Building and monitoring the campus IoT sensor network — focused on embedded systems, real-time data pipelines, and reliable device automation.
            </div>
          </div>
          <div className="card">
            <div className="card-head">Personal Information</div>
            <div className="info-list">
              <div className="info-item">
                <div className="info-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <circle cx="12" cy="8" r="3.5" />
                    <path d="M4.5 20c1.5-4 5-5.5 7.5-5.5s6 1.5 7.5 5.5" />
                  </svg>
                </div>
                <div>
                  <div className="info-label">Full Name</div>
                  <div className="info-value">Nguyễn Đức Toàn</div>
                </div>
              </div>
              <div className="info-item">
                <div className="info-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M6 2h9l5 5v15H6z" />
                    <path d="M15 2v5h5M9 13h6M9 17h6M9 9h2" />
                  </svg>
                </div>
                <div>
                  <div className="info-label">Student ID</div>
                  <div className="info-value">B23DCCN831</div>
                </div>
              </div>
              <div className="info-item">
                <div className="info-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M3 6h18v12H3z" />
                    <path d="M3 7l9 6 9-6" />
                  </svg>
                </div>
                <div>
                  <div className="info-label">Email</div>
                  <div className="info-value">toannd.b23cn831@ptit.edu.vn</div>
                </div>
              </div>
              <div className="info-item">
                <div className="info-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M3 21h18M6 21V9l6-4 6 4v12M10 21v-6h4v6" />
                  </svg>
                </div>
                <div>
                  <div className="info-label">School</div>
                  <div className="info-value">Post and Telecommunication Institute of Technology</div>
                </div>
              </div>
              <div className="info-item">
                <div className="info-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M3 13h4v8H3zM10 3h4v18h-4zM17 8h4v13h-4z" />
                  </svg>
                </div>
                <div>
                  <div className="info-label">Project</div>
                  <div className="info-value">IoT Monitoring System</div>
                </div>
              </div>
            </div>
          </div>
          <div className="card">
            <div className="card-head">Project Links</div>
            <div className="link-row vertical">
              <a className="link-btn full-width" href="https://github.com/">
                <span style={{display:'flex', alignItems:'center', gap:'8px'}}>
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 .5C5.7.5.7 5.5.7 11.8c0 5 3.2 9.2 7.7 10.7.6.1.8-.2.8-.6v-2.2c-3.1.7-3.8-1.5-3.8-1.5-.5-1.3-1.2-1.7-1.2-1.7-1-.7.1-.7.1-.7 1.1.1 1.7 1.1 1.7 1.1 1 1.7 2.6 1.2 3.2.9.1-.7.4-1.2.7-1.5-2.5-.3-5.1-1.2-5.1-5.5 0-1.2.4-2.2 1.1-3-.1-.3-.5-1.4.1-2.9 0 0 .9-.3 3 1.1a10 10 0 0 1 5.4 0c2.1-1.4 3-1.1 3-1.1.6 1.5.2 2.6.1 2.9.7.8 1.1 1.8 1.1 3 0 4.3-2.6 5.2-5.1 5.5.4.4.8 1.1.8 2.2v3.3c0 .4.2.7.8.6 4.5-1.5 7.7-5.7 7.7-10.7C23.3 5.5 18.3.5 12 .5z" />
                  </svg> GitHub
                </span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{width:'14px', height:'14px', color:'var(--text-muted)'}}>
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3" />
                </svg>
              </a>
              <a className="link-btn full-width" href="#">
                <span style={{display:'flex', alignItems:'center', gap:'8px'}}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M12 2a5 5 0 0 1 0 10 5 5 0 0 1 0 10 5 5 0 0 1-5-5v-5H7a5 5 0 1 1 5-5z" />
                  </svg> Figma
                </span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{width:'14px', height:'14px', color:'var(--text-muted)'}}>
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3" />
                </svg>
              </a>
              <a className="link-btn full-width" href="#">
                <span style={{display:'flex', alignItems:'center', gap:'8px'}}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <circle cx="12" cy="12" r="9" />
                    <path d="M8 12l2.5 2.5L16 9" />
                  </svg> Postman
                </span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{width:'14px', height:'14px', color:'var(--text-muted)'}}>
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3" />
                </svg>
              </a>
              <a className="link-btn full-width" href="#">
                <span style={{display:'flex', alignItems:'center', gap:'8px'}}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M6 2h9l5 5v15H6z" />
                    <path d="M15 2v5h5M9 13h6M9 17h6M9 9h2" />
                  </svg> PDF Report
                </span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{width:'14px', height:'14px', color:'var(--text-muted)'}}>
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3" />
                </svg>
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Profile;
