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

    if (loading) {
        return <div style={styles.loading}>Loading sign-ups...</div>;
    }

    return (
        <div style={styles.container}>
     
            <h2 style = {styles.title}>Events I'm signed up for</h2>
            <p style = {styles.subtitle}>Your upcoming meetups</p>
            <div style={styles.divider} />
            
            {myHangouts.length === 0 ? 
                (<div style = {styles.emptyText}>No sign ups yet. pick an event from the map above!</div>)
                :
                (myHangouts.map((item) => (
                <div 
                key={item.id}
                style={styles.card}
                > 
                    <div style={styles.cardContent}>
                        <div style ={styles.cardTitle}>title: {item.title}</div>
                        <div style ={styles.cardText}>location: {item.locationName}</div>
                        <div style ={styles.cardText}>time: {item.dateTime}</div>
                    </div>
                    <button
                        onClick={() => handleCancelSignup(item.id, item.title)}
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
  }
};