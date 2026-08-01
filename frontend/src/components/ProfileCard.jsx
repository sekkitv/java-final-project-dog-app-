import { useState } from 'react';



/**
 * ProfileCard Component
 * Displays user and dog information with a toggle button to switch between views
 */
export default function ProfileCard({ profile }) {
    const [viewMode, setViewMode] = useState('owner');

    if (!profile) return null;

    return (

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
            src={profile.ownerPhotoUrl || '/default-avatar.png'} 
            alt={profile.ownerName} 
            style={styles.image} 
          />
          <h2>{profile.ownerName}, {profile.ownerAge}</h2>
          <p style={styles.subtitle}>📍 {profile.distance} km away</p>
          <p style={styles.bio}>{profile.ownerBio}</p>
        </div>
      ) : (
        <div>
          <img 
            src={profile.dogPhotoUrl || '/default-avatar.png'} 
            alt={profile.dogName} 
            style={styles.image} 
          />
          <h2>{profile.dogName}, {profile.dogAge}</h2>
          <p style={styles.subtitle}>Breed: {profile.dogBreed}</p>
          <p style={styles.bio}>{profile.dogBio}</p>
        </div>
      )}

    </div>

    );
}
const styles = {
    container:{
        border: '2px solid black', 
        borderRadius: '12px', 
        padding: '20px', 
        width: '100%',
        maxWidth: '360px',
        boxSizing: 'border-box',
        minHeight: '520px',
        background: '#fff', 
        backgroundColor: '#fff9f5',
        boxShadow: '0 4px 8px rgba(0,0,0,0.1)' 
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