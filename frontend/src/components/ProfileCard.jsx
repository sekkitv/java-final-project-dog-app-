import { useState } from 'react';



/**
 * ProfileCard Component
 * Displays user and dog information with a toggle button to switch between views
 */
export default function ProfileCard({ profile }) {
    const [viewMode, setViewMode] = useState('owner');

    if (!profile) return null;
    {console.log(profile)}
    const defaultAvatar = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80';
    const defaultDogAvatar = 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=300&q=80';

    const formattedDistance = typeof profile.distanceKm === 'number' 
      ? profile.distanceKm.toFixed(0) 
      : profile.distanceKm;

    return (
      <div style={styles.cardStackContainer}>
        <div style={styles.backgroundCard} />
        <div style={styles.container}>
              
              <div style={styles.toggleContainer}>
                <button 
                  onClick={() => setViewMode('owner')}
                  style={{
                            ...styles.toggleButton,
                            background: viewMode === 'owner' ? 'linear-gradient(135deg, #ff7e5f, #feb47b)' : 'transparent',
                            color: viewMode === 'owner' ? '#fff' : '#666',
                            fontWeight: viewMode === 'owner' ? 'bold' : 'normal',
                            boxShadow: viewMode === 'owner' ? '0 4px 10px rgba(255, 126, 95, 0.4)' : 'none'
                  }}
                >
                  Owner 👤
                </button>
                <button 
                  onClick={() => setViewMode('dog')}
                  style={{
                            ...styles.toggleButton,
                            background: viewMode === 'dog' ? 'linear-gradient(135deg, #ff7e5f, #feb47b)' : 'transparent',
                            color: viewMode === 'dog' ? '#fff' : '#666',
                            fontWeight: viewMode === 'dog' ? 'bold' : 'normal',
                            boxShadow: viewMode === 'dog' ? '0 4px 10px rgba(255, 126, 95, 0.4)' : 'none'
                        }}
                >
                  Dog 🐶
                </button>
              </div>

              {viewMode === 'owner' ? (
                <div>
                  <img 
                    src={profile.photoUrl || defaultAvatar} 
                    alt={profile.username} 
                    style={styles.image} 
                  />
                  <h2>{profile.username}</h2>
                  <p style={styles.subtitle}>📍 {formattedDistance} km away</p>
                  <p style={styles.bio}>{profile.description}</p>
                </div>
              ) : (
                <div>
                  <img 
                    src={profile.dogPhotoUrl || defaultDogAvatar} 
                    alt={profile.dogName} 
                    style={styles.image} 
                  />
                  <h2>{profile.dogName}, {profile.dogAge}</h2>
                  <p style={styles.subtitle}>Breed: {profile.breed}</p>
                  <p style={styles.bio}>{profile.description}</p>
                </div>
              )}

            </div>
      </div>

    );
}
const styles = {
  cardStackContainer: {
    position: 'relative',
    width: '100%',
    maxWidth: '380px',
    margin: '0 auto',
  },
  backgroundCard: {
    position: 'absolute',
    top: '10px',
    left: '10px',
    right: '-10px',
    bottom: '-10px',
    backgroundColor: '#FFE0CC',
    borderRadius: '24px',
    border: '2px solid #2D3748',
    zIndex: 1,
  },
  container:{
    position: 'relative',
    zIndex: 2,         
    border: '2px solid #2D3748',
    borderRadius: '24px',
    padding: '20px',
    width: '100%',
    boxSizing: 'border-box',
    minHeight: '520px',
    backgroundColor: '#ffffff',
    boxShadow: '0 8px 24px rgba(74, 50, 34, 0.12)',
  },
  toggleContainer:{
      display: 'flex', 
      marginBottom: '15px', 
      background: '#f0f0f0',
      borderRadius: '8px',
      padding: '4px' 
  },
  toggleButton:{
      flex: 1, 
      padding: '8px', 
      border: 'none', 
      borderRadius: '6px', 
      cursor: 'pointer',   
  },
  image: {
      width: '100%',
      height: '300px',
      objectFit: 'cover',
      borderRadius: '8px'
  },
  title: {
      marginTop: '15px',
      marginBottom: '8px',
      fontSize: '22px',
      color: '#333'
  },
  subtitle: {
      color: '#666',
      marginBottom: '10px',
      fontSize: '14px'
  },
  bio: {
      color: '#444',
      fontSize: '15px',
      lineHeight: '1.4',
      minHeight: '45px'
  }
}