import { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup ,useMap, useMapEvents} from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { api } from '../services/api';
import EventsList from './EventsList'
import UserEvents from './UserEvents'
import ProfileSidebar from './ProfileSidebar'


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
 * Controller component to fly map view to target coordinates
 */
function MapController({ center, trigger }) {
  const map = useMap(); 
  const lat = center?.[0];
  const lng = center?.[1];
  useEffect(() => {
    if (lat && lng) {
       map.invalidateSize();
      const timer = setTimeout(() => {
        map.flyTo([lat, lng], 14, {
          animate: true,
          duration: 1.2,
        });
      }, 50);

      return () => clearTimeout(timer);
    }
  }, [lat, lng, trigger, map]);

  return null;
}

/**
 * Captures map clicks and passes coordinates to parent when selection mode is active
 */
function LocationPicker({ onLocationSelect }) {
  useMapEvents({
    click(e) {
      onLocationSelect(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

// Available event categories for map creation
const EVENT_TYPES = [
  { id: 'Meetup', label: 'Meetup', icon: '🐕' },
  { id: 'Business', label: 'Dog-friendly business', icon: '☕' },
  { id: 'Water', label: 'Water spot', icon: '💧' },
  { id: 'Bags', label: 'Poop bags', icon: '🧴' },
];

/**
 * MapPage Component
 * Renders interactive Leaflet map displaying active dog hangouts, geolocation controls,
 * and forms to pick map points and publish new events
 */
export default function MapPage() {
  const [userLocation, setUserLocation] = useState([32.0853, 34.7818]);
  const [hangouts, setHangouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedHangout, setSelectedHangout] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isSelectingLocation, setIsSelectingLocation] = useState(false);
  const [myHangouts, setMyHangouts] = useState([]);
  const [centerTrigger, setCenterTrigger] = useState(0);
  const [formData, setFormData] = useState({
    type: 'Meetup',
    title: '',
    organizer: '',
    dateTime: '',
    lat: '32.182177917609735',
    lng: '34.93094514609965',
    description: ''
  });

  // Request browser geolocation on initial mount
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

 
  // Update specific form field in state
  const handleInputChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  // Submit new event form to API and add him to myhangout list
  const handleSaveEvent = async (e) => {
    e.preventDefault();
    if (!formData.title.trim()) {
      alert('Please enter a title');
      return;
    }

    try {
      const newEvent = {
        title: formData.title,
        type: formData.type,
        organizer: formData.organizer || 'Anonymous',
        dateTime: formData.dateTime || 'Soon',
        locationName: `Location: ${Number(formData.lat).toFixed(4)}, ${Number(formData.lng).toFixed(4)}`,
        lat: parseFloat(formData.lat),
        lng: parseFloat(formData.lng),
        description: formData.description
      };

      const savedEvent =await api.createHangout(newEvent);
      const eventWithId = { ...newEvent, id: savedEvent?.id || Date.now().toString() };
      setHangouts((prev) => [...prev, { ...newEvent, id: Date.now().toString() }]);
      setMyHangouts((prev) => [...prev, eventWithId]);
      alert('Event added successfully! 🎉');
      setIsCreateModalOpen(false);

      // Submit new event form to API
      setFormData({
        type: 'Meetup',
        title: '',
        organizer: '',
        dateTime: '',
        lat: userLocation[0].toString(),
        lng: userLocation[1].toString(),
        description: ''
      });
    } catch (err) {
      console.error('Failed to save event:', err);
      alert('Failed to save event. Please try again.');
    }
  };
  

    // Center map on user's current GPS location
    const myAreaClicked = () => {
      setCenterTrigger((prev) => prev + 1);
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
    };

    // Update form coordinates after user clicks on map and reopen modal
    const handleMapClick = (lat, lng) => {
      setFormData((prev) => ({
        ...prev,
        lat: lat.toString(),
        lng: lng.toString(),
      }));
      setIsSelectingLocation(false);
      setIsCreateModalOpen(true);
    };

  // Temporarily hide modal to allow clicking a position on the map
  const startSelectingOnMap = () => {
    setIsCreateModalOpen(false);
    setIsSelectingLocation(true);
  };



  if (loading) {
    return <div style={styles.loading}>Loading map & hangouts...</div>;
  }

  return (
  
        <div style = {styles.mainPageWrapper}>
          <div style={styles.leftMainColumn}> 
                <div style={styles.container}>
                  {/* Location Picker Instruction Banner */}
                  {isSelectingLocation && (
                    <div style={styles.selectingBanner}>
                      📍 Click anywhere on the map to set the event location
                    </div>
                  )}
                  <div style={styles.contentLayout}>
                    <EventsList hangouts={hangouts} onSelectHangout={setSelectedHangout} />

                    <div style={styles.rightSection}>
                        {/* Page Header */}
                        <div style={styles.headerRow}>
                            <div style={styles.titleGroup}>
                              <h1 style={styles.title}>PawMap</h1>
                              <span style={styles.subtitle}>click the map to plan a new hangout</span>
                            </div>

                            <div style={styles.headerButtons}>
                              <button style={styles.myAreaBtn} title="My area" onClick={myAreaClicked}>
                                📍 My area
                              </button>
                              <button 
                                style={styles.addEventBtn} 
                                title="Add event" 
                                onClick={() => setIsCreateModalOpen(true)}
                              >
                              + Add event
                              </button>
                            </div>
                        </div> 

                        {/* Main Interactive Map */}
                        <div style={styles.mapWrapper}>
                          <MapContainer 
                            center={userLocation}
                            zoom={13}
                            scrollWheelZoom={true}
                            style={styles.map}
                          >
                            <MapController center={userLocation} trigger={centerTrigger} />
                            <TileLayer
                              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                            />
                            <LocationPicker 
                              active={isSelectingLocation} 
                              onLocationSelect={handleMapClick}
                            />

                            {/* Marker for selected event location */}
                            {formData.lat && formData.lng && (
                              <Marker position={[parseFloat(formData.lat), parseFloat(formData.lng)]} icon={customIcon}>
                                <Popup>Selected Location 📍</Popup>
                              </Marker>
                            )}
                            {/* Current User Location Marker */}
                            <Marker position={userLocation} icon={customIcon}>
                              <Popup>
                                <strong>You are here!📍</strong>
                              </Popup>
                            </Marker>

                            {/* Existing Hangout Markers */}
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
                      </div>
                  </div>
                  <UserEvents 
                        myHangouts={myHangouts} 
                        setMyHangouts={setMyHangouts}      
                  />
                  {/* Add Event Modal */}
                  {isCreateModalOpen && (
                    <div style={styles.modalOverlay}>
                      <div style={styles.createModalCard}>
                        <div style={styles.modalHeader}>
                          <h2 style={styles.createModalTitle}>Add to the map</h2>
                          <button 
                            onClick={() => setIsCreateModalOpen(false)} 
                            style={styles.closeIconBtn}
                          >
                            ✕
                          </button>
                        </div>

                        <div style={styles.locationBannerRow}>
                          <div style={styles.locationText}>
                            📍 Location: {Number(formData.lat).toFixed(4)}, {Number(formData.lng).toFixed(4)}
                          </div>
                          <button 
                            type="button" 
                            onClick={startSelectingOnMap} 
                            style={styles.pickOnMapBtn}
                          >
                            Choose on Map 🗺️
                          </button>
                        </div>

                        <form onSubmit={handleSaveEvent} style={styles.formStack}>
                          <label style={styles.label}>What type?</label>
                          <div style={styles.typeGrid}>
                            {EVENT_TYPES.map((typeObj) => {
                              const isSelected = formData.type === typeObj.id;
                              return (
                                <button
                                  key={typeObj.id}
                                  type="button"
                                  onClick={() => handleInputChange('type', typeObj.id)}
                                  style={{
                                    ...styles.typeBtn,
                                    ...(isSelected ? styles.typeBtnSelected : {})
                                  }}
                                >
                                  <span>{typeObj.icon}</span>
                                  <span>{typeObj.label}</span>
                                </button>
                              );
                            })}
                          </div>

                          <label style={styles.label}>Title</label>
                          <input
                            type="text"
                            placeholder="e.g. Sunday Park Fetch Club"
                            value={formData.title}
                            onChange={(e) => handleInputChange('title', e.target.value)}
                            style={styles.input}
                          />

                          <label style={styles.label}>Your name</label>
                          <input
                            type="text"
                            value={formData.organizer}
                            onChange={(e) => handleInputChange('organizer', e.target.value)}
                            style={styles.input}
                          />

                          <label style={styles.label}>When?</label>
                          <input
                            type="datetime-local"
                            value={formData.dateTime}
                            onChange={(e) => handleInputChange('dateTime', e.target.value)}
                            style={styles.input}
                          />

                          <label style={styles.label}>Description (optional)</label>
                          <textarea
                            placeholder="Leash rules, treats, dog sizes welcome..."
                            value={formData.description}
                            onChange={(e) => handleInputChange('description', e.target.value)}
                            style={styles.textarea}
                            rows={3}
                          />

                          <div style={styles.createModalActions}>
                            <button
                              type="button"
                              onClick={() => setIsCreateModalOpen(false)}
                              style={styles.cancelBtn}
                            >
                              Cancel
                            </button>
                            <button type="submit" style={styles.saveBtn}>
                              Save
                            </button>
                          </div>
                        </form>
                    </div>
                </div>
              )}
            
            {/* Hangout Details Modal */}
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
                            setMyHangouts((prev) => {
                              const exists = prev.some(item => item.id === selectedHangout.id);
                              return exists ? prev : [...prev, selectedHangout];
                            });
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
          </div>
          <div style={styles.rightMainColumn}>
            <ProfileSidebar />
          </div>
      </div>
  );
}

const styles = {
  mainPageWrapper: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'stretch',
    justifyContent: 'center',
    gap: '20px',
    maxWidth: '1800px',
    margin: '16px auto',
    width: '100%',
    boxSizing: 'border-box'
  },
  leftMainColumn: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0,
  },

  container: {
    maxWidth: '100%',
    padding: '24px 20px',
    backgroundColor: '#ffffff',
    borderRadius: '20px',
    boxShadow: '0 8px 24px rgba(255, 126, 95, 0.12)',
    border: '2px solid #2D3748',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
    boxSizing: 'border-box'
      
  },
  contentLayout: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'stretch',
    gap: '16px',
    width: '100%'
  },
  rightSection: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    minWidth: 0,
    backgroundColor: '#FAF5EE',
    borderRadius: '20px',
    padding: '16px',
    border: '1.5px solid #E2D3C5',
    boxSizing: 'border-box'
  },
  headerRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    paddingBottom: '8px'
  },
  titleGroup: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    gap: '2px'
  },
  title: {
    fontSize: '20px',
    fontWeight: '800',
    color: '#4A3222',
    margin: 0
  },
  subtitle: {
    fontSize: '12px',
    color: '#8C7A6B',
    fontWeight: '400'
  },
  headerButtons: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  myAreaBtn: {
    backgroundColor: 'white', 
    color: '#4A3222',
    border: '1.5px solid #2D3748',
    borderRadius: '20px',
    padding: '8px 16px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '6px'
  },
  addEventBtn: {
    backgroundColor: '#FF7A65', 
    color: '#FFFFFF',
    border: '1.5px solid #2D3748',
    borderRadius: '20px',
    padding: '8px 18px',
    fontSize: '13px',
    fontWeight: 'bold',
    cursor: 'pointer',
    boxShadow: '0 4px 10px rgba(255, 122, 101, 0.25)'
  },
  loading: {
    textAlign: 'center',
    marginTop: '40px',
    color: '#666'
  },
  mapWrapper: {
    height: '580px',
    width: '100%',
    borderRadius: '16px',
    overflow: 'hidden',
    border: '1px solid #E2D3C5'
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
  },
  createModalCard: {
    backgroundColor: '#FAF5EE',
    padding: '16px',
    borderRadius: '24px',
    maxWidth: '400px',
    width: '100%',
    maxHeight: '90vh',
    overflowY: 'auto',
    boxShadow: '0 10px 30px rgba(0,0,0,0.15)',
    boxSizing: 'border-box'
  },
  modalHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px'
  },
  createModalTitle: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#5C3E21',
    margin: 0
  },
  closeIconBtn: {
    border: '1.5px solid #2d3748',
    backgroundColor: 'transparent',
    borderRadius: '50%',
    width: '28px',
    height: '28px',
    fontSize: '12px',
    fontWeight: 'bold',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  locationBanner: {
    backgroundColor: '#F3EAE1',
    color: '#5C3E21',
    padding: '6px 10px',
    borderRadius: '10px',
    fontSize: '12px',
    fontWeight: '600',
    marginBottom: '8px'
  },
  formStack: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  label: {
    fontSize: '12px',
    fontWeight: '700',
    color: '#5C3E21',
    marginTop: '2px'
  },
  typeGrid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '6px'
  },
  typeBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '6px',
    padding: '6px 8px',
    borderRadius: '12px',
    border: '1.5px solid #E2D3C5',
    backgroundColor: '#FFF',
    fontSize: '11px',
    fontWeight: '600',
    color: '#5C3E21',
    cursor: 'pointer'
  },
  typeBtnSelected: {
    border: '2px solid #FF9F76',
    backgroundColor: '#FFF3EB'
  },
  input: {
    padding: '6px 10px',
    borderRadius: '10px',
    border: '1.5px solid #E2D3C5',
    backgroundColor: '#FFF',
    fontSize: '12px',
    outline: 'none'
  },
  textarea: {
    padding: '6px 10px',
    borderRadius: '10px',
    border: '1.5px solid #E2D3C5',
    backgroundColor: '#FFF',
    fontSize: '12px',
    outline: 'none',
    resize: 'none'
  },
  createModalActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '8px',
    marginTop: '6px'
  },
  saveBtn: {
    padding: '8px 20px',
    borderRadius: '14px',
    border: 'none',
    backgroundColor: '#FF7A65',
    color: '#FFF',
    fontWeight: 'bold',
    fontSize: '13px',
    cursor: 'pointer'
  },
  modalTitle: {
    color: '#2d3748',
    marginBottom: '8px',
    fontSize: '20px',
    fontWeight: '700'
  },
  selectingBanner: {
    backgroundColor: '#FF7A65',
    color: '#FFF',
    padding: '8px 12px',
    borderRadius: '12px',
    textAlign: 'center',
    fontWeight: 'bold',
    fontSize: '13px',
    boxShadow: '0 4px 12px rgba(255,122,101,0.3)'
  },
  locationBannerRow: {
    backgroundColor: '#F3EAE1',
    padding: '8px 12px',
    borderRadius: '12px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
    gap: '8px'
  },
  locationText: {
    color: '#5C3E21',
    fontSize: '12px',
    fontWeight: '600'
  },
  pickOnMapBtn: {
    backgroundColor: '#5C3E21',
    color: '#FFF',
    border: 'none',
    borderRadius: '8px',
    padding: '5px 10px',
    fontSize: '11px',
    fontWeight: 'bold',
    cursor: 'pointer',
    whiteSpace: 'nowrap'
  }
 
 
  
};