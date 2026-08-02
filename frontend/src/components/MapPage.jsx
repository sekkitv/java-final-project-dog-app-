import { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { api } from '../services/api';

// Assets & Icon configuration for Leaflet in React bundlers
import markerIconPng from 'leaflet/dist/images/marker-icon.png';
import markerShadowPng from 'leaflet/dist/images/marker-shadow.png';

const customIcon = new L.Icon({
  iconUrl: markerIconPng,
  shadowUrl: markerShadowPng,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
});

/**
 * MapPage Component
 * Displays an interactive map with active dog hangouts and signup interactions.
 */
export default function MapPage() {
  const [userLocation, setUserLocation] = useState([32.0853, 34.7818]);
  const [hangouts, setHangouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedHangout, setSelectedHangout] = useState(null);

  // Request browser geolocation on mount
  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation([position.coords.latitude, position.coords.longitude]);
        },
        (error) => {
          console.warn('Geolocation error or denied:', error);
        }
      );
    }
  }, []);

  // Fetch active hangouts from API
  useEffect(() => {
    const loadHangouts = async () => {
      try {
        setLoading(true);
        const data = await api.fetchHangouts();
        setHangouts(data || []);
      } catch (err) {
        console.error('Failed to fetch hangouts:', err);
      } finally {
        setLoading(false);
      }
    };

    loadHangouts();
  }, []);

  if (loading) {
    return <div style={styles.loading}>Loading map & hangouts...</div>;
  }

  return (
    <div style={styles.container}>
      <h1 style={styles.title}>Dog Hangouts & Events 🐾</h1>

      <div style={styles.mapWrapper}>
        <MapContainer 
          center={userLocation} 
          zoom={13} 
          scrollWheelZoom={true} 
          style={styles.map}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <Marker position={userLocation} icon={customIcon}>
            <Popup>
              <strong>You are here!📍</strong>
            </Popup>
          </Marker>

          {hangouts.map((item) => (
            <Marker 
              key={item.id} 
              position={[item.lat, item.lng]} 
              icon={customIcon}
            >
              <Popup>
                <div style={styles.popupContent}>
                  <h3 style={styles.popupTitle}>{item.title}</h3>
                  <p style={styles.popupText}>📍 {item.locationName}</p>
                  <p style={styles.popupText}>⏰ {item.dateTime}</p>
                  <button 
                    onClick={() => setSelectedHangout(item)}
                    style={styles.detailsBtn}
                  >
                    View Details & Join 🦴
                  </button>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>

    
      {selectedHangout && (
        <div style={styles.modalOverlay}>
          <div style={styles.modalCard}>
            <h2 style={styles.modalTitle}>{selectedHangout.title}</h2>
            <p style={styles.modalText}><b>Organizer:</b> {selectedHangout.organizer}</p>
            <p style={styles.modalText}><b>When:</b> {selectedHangout.dateTime}</p>
            <p style={styles.modalText}><b>Location:</b> {selectedHangout.locationName}</p>
            <p style={styles.modalDescription}>"{selectedHangout.description}"</p>
            
            <div style={styles.modalActions}>
              <button 
                onClick={async () => {
                  try {
                    await api.signupHangout(selectedHangout.id);
                    alert(`Awesome! You are signed up for ${selectedHangout.title} 🎉`);
                  } catch {
                    alert('Failed to sign up. Please try again.');
                  }
                  setSelectedHangout(null);
                }}
                style={styles.confirmBtn}
              >
                Join Hangout ✋
              </button>
              <button 
                onClick={() => setSelectedHangout(null)}
                style={styles.cancelBtn}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  container: {
    maxWidth: '480px',
    margin: '20px auto',
    padding: '24px 20px',
    backgroundColor: '#ffffff',
    borderRadius: '20px',
    boxShadow: '0 8px 24px rgba(255, 126, 95, 0.12)',
    border: '2px solid #ffd8cc',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
    boxSizing: 'border-box'
  },
  title: {
    fontSize: '24px',
    fontWeight: 'bold',
    color: '#2d3748',
    textAlign: 'center',
    margin: 0
  },
  loading: {
    textAlign: 'center',
    marginTop: '40px',
    color: '#666'
  },
  mapWrapper: {
    height: '380px',
    width: '100%',
    borderRadius: '16px',
    overflow: 'hidden',
    border: '1.5px solid #ffedd8'
  },
  map: {
    height: '100%',
    width: '100%'
  },
  popupContent: {
    textAlign: 'center',
    padding: '4px'
  },
  popupTitle: {
    margin: '0 0 4px 0',
    fontSize: '16px',
    color: '#2d3748'
  },
  popupText: {
    margin: '2px 0',
    fontSize: '12px',
    color: '#4a5568'
  },
  detailsBtn: {
    padding: '6px 12px',
    borderRadius: '12px',
    border: 'none',
    backgroundColor: '#ff7e5f',
    color: '#fff',
    fontWeight: 'bold',
    cursor: 'pointer',
    marginTop: '6px'
  },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000
  },
  modalCard: {
    backgroundColor: '#fff',
    padding: '24px',
    borderRadius: '20px',
    maxWidth: '320px',
    width: '90%',
    textAlign: 'center',
    boxShadow: '0 10px 25px rgba(0,0,0,0.2)'
  },
  modalText: {
    margin: '4px 0',
    color: '#666',
    fontSize: '14px'
  },
  modalDescription: {
    margin: '8px 0',
    fontSize: '14px',
    fontStyle: 'italic',
    color: '#4a5568'
  },
  modalActions: {
    display: 'flex',
    gap: '10px',
    justifyContent: 'center',
    marginTop: '16px'
  },
  confirmBtn: {
    padding: '10px 16px',
    borderRadius: '16px',
    border: 'none',
    backgroundColor: '#ff7e5f',
    color: '#fff',
    fontWeight: 'bold',
    cursor: 'pointer'
  },
  cancelBtn: {
    padding: '10px 16px',
    borderRadius: '16px',
    border: '1.5px solid #cbd5e0',
    backgroundColor: '#fff',
    color: '#4a5568',
    cursor: 'pointer'
  }
};