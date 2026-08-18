
import { useState , useEffect} from 'react';
import { api } from '../services/api';
import { useApp } from '../context/useApp';


export default function ProfileSidebar() {
    const [ownerImgUrl, setOwnerImgUrl] = useState('');
    const [dogImgUrl, setDogImgUrl] = useState('');
    const [loading, setLoading] = useState(true);
    const [name, setName] = useState('');
    const [dogName, setDogName] = useState('');
    const {logout , setActiveTab} = useApp();

    useEffect(() => {
        const loadUser = async () => {
            try {
                setLoading(true);
                const data = await api.fetchProfile(); 
                if (data) {
                    setName(data.name || data.username);
                    setOwnerImgUrl(data.photoUrl);
                    setDogImgUrl(data.dogPhotoUrl);
                    setDogName(data.dogName);
                }
            } catch (err) {
                console.error('Failed to load user hangouts:', err);
            } finally {
                setLoading(false);
            }
        };

        loadUser();
    }, []);

    if (loading) {
        return <div style={styles.loading}>Loading sign-ups...</div>;
    }
    return(
        <div style={styles.container}>
            <h2 style={styles.title}>My PawProfile</h2>
               <div style={styles.divider} />
               <div style={styles.avatarsRow}>
                    <div style={styles.avatarWrapper}>
                        <img   
                            src={ownerImgUrl || 'https://placehold.co/80x80/ff7e5f/white?text=Owner'}
                            alt={name || 'Owner'}
                            style={styles.ownerAvatar} 
                        />
                        <span style={styles.avatarLabel}>You</span>
                    </div>
                    <div style={styles.avatarWrapper}>
                        <img   
                            src={dogImgUrl || 'https://placehold.co/80x80/ff7e5f/white?text=dog'}
                            alt={dogName || 'dog'}
                            style={styles.dogAvatar} 
                        />
                        <span style={styles.avatarLabel}>Your dog</span>
                    </div>
               </div>
               <div style={styles.nameContainer}>@{name || 'user'}</div>
               <div style={styles.textContainer}> tell other dog lovers a bit about you in Settings</div>
               <button onClick={() => setActiveTab('settings')} style={{...styles.editButton , background: 'linear-gradient(135deg, #ff7e5f, #feb47b)' }}>Edit profile</button>
               <button onClick ={() => logout()} style={styles.signOutBtn}>Sign out</button>
        </div>


    );
};

const styles ={
   container: {
    width: '280px',
    height: '100%',
    backgroundColor: '#FFFFFF', 
    borderRadius: '20px',
    padding: '20px 16px',
    border: '2px solid #2D3748',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '12px',
    flexShrink: 0
  },
  title: {
    fontSize: '20px',
    fontWeight: '800',
    color: '#4A3222',
    margin: 0,
    alignSelf: 'flex-start'
  },
  divider: {
    height: '1px',
    backgroundColor: '#E2D3C5',
    width: '100%',
    margin: '0 0 4px 0'
  },
  loading: {
    textAlign: 'center',
    marginTop: '40px',
    color: '#666'
  },
  avatarsRow: {
    display: 'flex',
    justifyContent: 'center',
    gap: '16px',
    width: '100%',
    marginTop: '4px'
  },
  avatarWrapper: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '4px'
  },
  avatarLabel: {
    fontSize: '11px',
    color: '#8C7A6B',
    fontWeight: '600'
  },
  ownerAvatar: {
    width: '100px',
    height: '100px',
    borderRadius: '50%',
    border: '3px solid #ffb88a',
    objectFit: 'cover'
  },
  dogAvatar: {
    width: '100px',
    height: '100px',
    borderRadius: '50%',
    border: '3px solid #7dd3a8',
    objectFit: 'cover'
  },
  nameContainer: {
    textAlign: 'center',
    color: '#4A3222',
    fontSize: '18px',
    fontWeight: '700'
  },
  textContainer: {
    textAlign: 'center',
    fontSize: '12px',
    color: '#8C7A6B',
    lineHeight: '1.4'
  },
  buttonsStack: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    width: '100%',
    marginTop: '8px'
  },
  editButton: {
    width: '100%',
    padding: '10px 0',
    borderRadius: '20px',
    cursor: 'pointer',
    border: 'none',
    color: 'white',
    fontWeight: 'bold',
    fontSize: '13px',
    background: 'linear-gradient(135deg, #ff7e5f, #feb47b)',
    boxShadow: '0 4px 10px rgba(255, 126, 95, 0.2)',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
  },
  signOutBtn: {
    width: '100%',
    padding: '10px 0',
    borderRadius: '20px',
    cursor: 'pointer',
    border: '1.5px solid #ffd8cc',
    color: '#ff6b6b',
    fontWeight: 'bold',
    fontSize: '13px',
    backgroundColor: '#fff5ec',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
  }
};