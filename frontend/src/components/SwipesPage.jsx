import  { useState, useEffect } from 'react';
<<<<<<< HEAD
import ProfileCard from ".//ProfileCard";
=======
import ProfileCard from "./ProfileCard";
>>>>>>> feature/ui-complete-pre-api
import { api } from '../services/api';


/**
 * MainApp Component
 * Fetches and displays profiles for swiping (LIKE / PASS). Handles swipe actions,
<<<<<<< HEAD
 * advances the feed index, and refreshes conversations on new matches.
=======
 * advances the feed index, and refreshes conversations on new matches
>>>>>>> feature/ui-complete-pre-api
 */
export default function SwipesPage({ onMatchCreated }) {
  const [feed, setFeed] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  
  
  /**
<<<<<<< HEAD
   * Fetches initial feed data on component mount.
=======
   * Fetches initial feed data on component mount
>>>>>>> feature/ui-complete-pre-api
   */
  useEffect(() => {
    async function loadFeed() {
      try {
        setLoading(true);
        const data = await api.fetchFeed();
<<<<<<< HEAD
        setFeed(data);
=======
        setFeed(data.candidates || data);
>>>>>>> feature/ui-complete-pre-api
      } catch (err) {
        console.error('Failed to fetch feed:', err);
        setError('Failed to load profiles. Please try again later.');
      } finally {
        setLoading(false);
      }
    }

    loadFeed();
  }, []);

  /**
<<<<<<< HEAD
   * Handles user swipe action (UP for LIKE, DOWN for PASS).
   * Advances feed index immediately for responsive UI and submits choice to API.
=======
   * Handles user swipe action (UP for LIKE, DOWN for PASS)
   * Advances feed index immediately for responsive UI and submits choice to API
>>>>>>> feature/ui-complete-pre-api
   */
  const handleSwipe = async (direction) => {
    const currentProfile = feed[currentIndex];

    if (!currentProfile) return;
<<<<<<< HEAD
    const action = direction === 'UP' ? 'LIKE' : 'PASS';

    setCurrentIndex((prev) => prev + 1);
    try {
      const response = await api.postSwipe(currentProfile.id, action);
=======

    setCurrentIndex((prev) => prev + 1);
    try {
      const response = await api.postSwipe(currentProfile.userId, direction);
>>>>>>> feature/ui-complete-pre-api

      // On match action, refresh conversation list for potential matches
      if (response?.isMatch) {
        if (api.fetchConversations) {
          await api.fetchConversations();
        }
        if (onMatchCreated) {
          onMatchCreated(currentProfile);
        }
      }
    } catch (err) {
<<<<<<< HEAD
      console.error(`Error processing ${action} swipe:`, err);
=======
      console.error(`Error processing ${direction} swipe:`, err);
>>>>>>> feature/ui-complete-pre-api
    }
  };
  

  if (loading) {
    return <div style={styles.loadingText}>Loading feed...</div>;
  }

  if (error) {
    return <div style={styles.errorText}>{error}</div>;
  }

  if (currentIndex >= feed.length) {
    return (
      <div style={styles.emptyContainer}>
        <h2>No more profiles</h2>
        <p>Check back later for new dogs and owners near you! 🐾</p>
      </div>
    );
  }

  const currentProfile = feed[currentIndex];

  return (
    <div style={styles.mainContainer}>
<<<<<<< HEAD
      
=======
>>>>>>> feature/ui-complete-pre-api
      <ProfileCard profile={currentProfile} />

     
      <div style={styles.buttonsContainer}>
        <button 
          onClick={() => handleSwipe('DOWN')}
          style={styles.passButton}
        >
          ❌ Pass
        </button>
        <button 
          onClick={() => handleSwipe('UP')}
          style={styles.likeButton}
        >
          ❤️ Like
        </button>
      </div>
    </div>
  );
}

const styles = {
  mainContainer: {
    width: '100%',
    maxWidth: '360px',
    margin: '20px auto',
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  loadingText: {
    textAlign: 'center',
    marginTop: '50px',
    fontSize: '18px',
    color: '#666'
  },
  emptyContainer: {
    textAlign: 'center',
    marginTop: '100px',
    color: '#444'
  },
  buttonsContainer: {
    display: 'flex',
    justifyContent: 'center',
    gap: '20px',
    marginTop: '20px'
  },
  passButton: {
    padding: '12px 24px',
    borderRadius: '25px',
    border: '1px solid #ffd6cc',
    backgroundColor: '#fff',
    cursor: 'pointer',
    fontWeight: 'bold',
    color: '#666',
    transition: 'all 0.2s ease'
  },
  likeButton: {
    padding: '12px 24px',
    borderRadius: '25px',
    border: 'none',
    background: 'linear-gradient(135deg, #ff7e5f, #feb47b)',
    color: '#fff',
    cursor: 'pointer',
    fontWeight: 'bold',
    boxShadow: '0 4px 10px rgba(255, 126, 95, 0.4)',
    transition: 'all 0.2s ease'
<<<<<<< HEAD
  }
=======
  },
  errorText: {
  textAlign: 'center',
  marginTop: '50px',
  fontSize: '18px',
  color: '#e74c3c'
},
>>>>>>> feature/ui-complete-pre-api
};