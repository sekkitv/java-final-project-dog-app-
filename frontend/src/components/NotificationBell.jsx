import {useState , useEffect} from 'react';
import {api} from '../services/api';


export default function NotificationBell () {

const [notificationBtn, setnotificationBtn] = useState(false);
const [notification, setnotification] = useState([]);

      
      useEffect(() => {
        const loadUserNotifications = async () => {
            try {
                const data = await api.fetchNotifications();
                setnotification(data);
            }
            catch(e){
                console.error('Failed to fetch notifications:' , e);
            }
        }
        loadUserNotifications();
        const intervalId = setInterval(loadUserNotifications, 5000);

        return () => clearInterval(intervalId);
        }, []);

        const getTimeAgo = (dateString) => {
            if (!dateString) return '';
                const diffInMinutes = Math.floor((new Date() - new Date(dateString)) / 1000 / 60);
            
            if (diffInMinutes < 1) return 'now';
            if (diffInMinutes < 60) return `before ${diffInMinutes} minutes`;
                const diffInHours = Math.floor(diffInMinutes / 60);
            if (diffInHours < 24) return `before ${diffInHours} hours`;
            return `before ${Math.floor(diffInHours / 24)} days`;
        };
        const getNotificationIcon = (type) => {
            switch (type) {
                case 'MATCH': return '❤️';
                case 'HANGOUT_JOIN': return '📅';
                case 'MESSAGE': return '💬';
                default: return '🐾';
            }
        };
    
    return (
                <div>
                    <button style={styles.btnContainer} onClick={() => setnotificationBtn(!notificationBtn)} >🔔</button>
                    {
                        notificationBtn && (
                            <div style={styles.notificationContainer}>
                                <h2>notifications</h2>
                                <div style={styles.divider} />
                                    {notification.length === 0 ? 
                                
                                        <div>no notification yet</div>
                                        :
                                        notification.map((item) => (
                                        <div 
                                        key={item.id}
                                        style={{
                                                ...styles.notificationItem,
                                                backgroundColor: item.isRead ? '#ffffff' : '#fff9f5' 
                                                }}>
                                            <span style={styles.itemIcon}>{getNotificationIcon(item.type)}</span>
                                            <div style={styles.itemContent}>
                                                <div style={styles.itemHeader}>
                                                    <span style={styles.itemTitle}>{item.title}</span>
                                                    <span style={styles.itemTime}>{getTimeAgo(item.createdAt)}</span>
                                                </div>
                                            <div style={styles.itemBody}>{item.body}</div>
                                        </div>
                                </div>
                                ))}
                            </div>
                        )
                    }
                </div>

    );
}

const styles = {
    wrapper: {
    position: 'relative',
    display: 'inline-block',
  },
  btnContainer: {
    width: '44px',
    height: '44px',
    borderRadius: '50%',
    background: 'white',
    border: '2px solid #ffe0cc',
    fontSize: '20px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: 'pointer',
    boxShadow: '0 2px 6px rgba(0,0,0,0.06)',
    position: 'relative'
  },
  notificationContainer: {
    position: 'absolute',
    top: '55px',
    right: '0',
    width: '310px',
    backgroundColor: '#ffffff',
    border: '1.5px solid #ffe0cc',
    borderRadius: '16px',
    padding: '14px',
    boxShadow: '0 8px 24px rgba(74, 50, 34, 0.12)',
    zIndex: 1000,
  },
  headerTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: '#4A3222',
    margin: '0 0 10px 0',
  },
  divider: {
    height: '1px',
    backgroundColor: '#ffe0cc',
    width: '100%',
    marginBottom: '8px'
  },
  emptyState: {
    padding: '16px 0',
    textAlign: 'center',
    color: '#8C7A6B',
    fontSize: '14px'
  },
  notificationItem: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '10px',
    padding: '10px',
    borderRadius: '10px',
    marginBottom: '6px',
    transition: 'background-color 0.2s ease',
    cursor: 'pointer'
  },
  itemIcon: {
    fontSize: '18px',
    marginTop: '2px'
  },
  itemContent: {
    flex: 1
  },
  itemHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '4px'
  },
  itemTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#4A3222'
  },
  itemTime: {
    fontSize: '11px',
    color: '#a09083'
  },
  itemBody: {
    fontSize: '13px',
    color: '#6e5e52',
    lineHeight: '1.3'
  }
}