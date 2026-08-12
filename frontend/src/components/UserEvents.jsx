import { useState , useEffect } from 'react';
import { api } from '../services/api';


export default function UserEvents({ myHangouts, setMyHangouts }) {
    const [loading, setLoading] = useState(true);
    
    useEffect(() => {
        const loadUserHangouts = async () => {
            try {
                setLoading(true);
                const data = await api.userHangouts(); 
                if (data && data.length > 0) {
                    setMyHangouts(data);
                }
            } catch (err) {
                console.error('Failed to load user hangouts:', err);
            } finally {
                setLoading(false);
            }
        };

        loadUserHangouts();
    }, [setMyHangouts]);

    const handleCancelSignup = async (id, title) => {
        if (!window.confirm(`Are you sure you want to cancel your signup for "${title}"?`)) {
            return;
        }
        try {
            await api.cancelHangoutSignup(id);
            setMyHangouts((prev) => prev.filter((item) => item.id !== id));
            alert('Signup canceled successfully');
        } catch (err) {
            console.error('Failed to cancel hangout signup:', err);
            alert('Failed to cancel signup. Please try again.');
        }
    };

     const formatEventTime = (timeString) => {
      if (!timeString) return 'N/A';
      if (!timeString.includes('T') && !timeString.includes('-')) {
        return timeString;
      }
      const date = new Date(timeString);
      if (isNaN(date.getTime())) return timeString;

      return date.toLocaleString('he-IL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
   };

    if (loading) {
        return <div style={styles.loading}>Loading sign-ups...</div>;
    }

    return (
        <div style={styles.container}>
     
            <h2 style = {styles.title}>Events I'm signed up for</h2>
            <p style = {styles.subtitle}>Your upcoming meetups</p>
            <div style={styles.divider} />
            <div style={styles.scrollList}>
                {myHangouts.length === 0 ? 
                    (<div style = {styles.emptyText}>No sign ups yet. pick an event from the map above!</div>)
                    :
                    (myHangouts.map((item) => (
                    <div 
                    key={item.hangoutId}
                    style={styles.card}
                    > 
                        <div style={styles.cardContent}>
                            <div style ={styles.cardTitle}> {item.title}</div>
                            <div style ={styles.cardText}>description: {item.description}</div>
                            <div style ={styles.cardText}>time: {formatEventTime(item.eventTime)}</div>
                        </div>
                        <button
                            onClick={() => handleCancelSignup(item.hangoutId, item.title)}
                            style={styles.cancelBtn}
                            title="Cancel signup"
                        >
                        ✕
                        </button>
                    </div>
                    ))
                    )
                }
            </div>
        </div>
    );
}
const styles = {
  container: {
    width: '100%',
    backgroundColor: '#FAF5EE', 
    borderRadius: '20px',
    padding: '16px',
    border: '1.5px solid #E2D3C5',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  title: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#5C3E21',
    margin: 0
  },
  subtitle: {
    fontSize: '12px',
    color: '#8C7A6B',
    margin: 0
  },
  divider: {
    height: '1px',
    backgroundColor: '#E2D3C5',
    margin: '4px 0'
  },
  emptyText: {
    fontSize: '13px',
    color: '#8C7A6B',
    padding: '8px 0'
  },
  card: {
    backgroundColor: '#FFFFFF',
    padding: '10px 12px',
    borderRadius: '12px',
    border: '1px solid #E2D3C5',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '8px'
  },
  cardContent: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px'
  },
  cardTitle: {
    fontWeight: 'bold',
    fontSize: '14px',
    color: '#5C3E21'
  },
  loading: {
    textAlign: 'center',
    marginTop: '40px',
    color: '#666',
  },
  cardText: {
    fontSize: '12px',
    color: '#666'
  },
  cancelBtn: {
    backgroundColor: 'transparent',
    border: '1px solid #E2D3C5',
    borderRadius: '50%',
    width: '26px',
    height: '26px',
    color: '#8C7A6B',
    fontSize: '12px',
    fontWeight: 'bold',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
    transition: 'all 0.2s ease'
  },
  scrollList: {
  maxHeight: '250px',
  overflowY: 'auto',
  paddingRight: '8px',
}
};