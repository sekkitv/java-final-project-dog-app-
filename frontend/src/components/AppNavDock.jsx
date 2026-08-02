import { useApp } from '../context/useApp';

/**
 * AppNavDock Component
 */
export default function AppNavDock() {
  const { activeTab, setActiveTab } = useApp();

  const tabs = [
    { id: 'map', label: 'Map', icon: '🗺️' },
    { id: 'swipes', label: 'Swipes', icon: '🐾' },
    { id: 'messages', label: 'Messages', icon: '💬' },
    { id: 'settings', label: 'Settings', icon: '⚙️' }
  ];

  return (
    <nav style={styles.dockContainer}>
      <div style={styles.buttonsWrapper}>
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                ...styles.tabButton,
                background: isActive 
                  ? 'linear-gradient(135deg, #ff7e5f, #feb47b)' 
                  : '#ffffff',
                color: isActive ? '#ffffff' : '#4a3b32',
                border: isActive ? 'none' : '1px solid #ffd8cc',
                boxShadow: isActive ? '0 4px 12px rgba(255, 126, 95, 0.35)' : 'none',
              }}
            >
              <span style={styles.icon}>{tab.icon}</span>
              <span style={{ 
                ...styles.label, 
                fontWeight: isActive ? 'bold' : '600' 
              }}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}

const styles = {
  dockContainer: {
    position: 'fixed',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'white',
    borderTop: '2px solid #ffedd8',
    padding: '12px 16px',
    zIndex: 1000,
    boxSizing: 'border-box'
  },
  buttonsWrapper: {
    display: 'flex',
    gap: '12px',
    maxWidth: '800px',
    margin: '0 auto',
    justify: 'space-between',
    alignItems: 'center'
  },
  tabButton: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justify: 'center',
    padding: '10px 8px',
    borderRadius: '16px',
    cursor: 'pointer',
    transition: 'all 0.25s ease',
    outline: 'none'
  },
  icon: {
    fontSize: '20px',
    marginBottom: '4px'
  },
  label: {
    fontSize: '13px'
  }
};