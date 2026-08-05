import {useState } from 'react';
import { useApp } from '../context/useApp';

/**
 * NotificationBell Component
 * Displays a header bell icon with an unread notification badge
 * Toggles a dropdown panel showing recent user notifications
 * Handles marking items as read and navigating to relevant screens based on notification type
 */
export default function NotificationBell () {

  const [notificationBtn, setNotificationBtn] = useState(false);
  const { notifications = [], markAsRead, setActiveTab } = useApp();
        
          
  const unreadCount = notifications.filter((item) => !item.isRead).length;


  /**
  * Formats an ISO date string into a relative time representation
  */
  const getTimeAgo = (dateString) => {
      if (!dateString) return '';
          const diffInMinutes = Math.floor((new Date() - new Date(dateString)) / 1000 / 60);
      if (diffInMinutes < 1) return 'now';
      if (diffInMinutes < 60) return `before ${diffInMinutes} minutes`;
          const diffInHours = Math.floor(diffInMinutes / 60);
      if (diffInHours < 24) return `before ${diffInHours} hours`;
      return `before ${Math.floor(diffInHours / 24)} days`;
  };

  /**
  * Returns a matching emoji icon based on the notification category
  */
  const getNotificationIcon = (type) => {
      switch (type) {
          case 'MATCH': return '❤️';
          case 'HANGOUT_JOIN': return '📅';
          case 'MESSAGE': return '💬';
          default: return '🐾';
      }
  };

  /**
   * Handles user click on an individual notification item
   * Marks item as read, closes the dropdown, and routes to the associated view
   */
  const handleNotificationClick = async (item) => {
    if (markAsRead) {
      markAsRead(item.id);
    }
    setNotificationBtn(false);
    if (item.type === 'MATCH' || item.type === 'MESSAGE') {
      setActiveTab('messages');
    } else if (item.type === 'HANGOUT_JOIN') {
      setActiveTab('map');
    }
  };
      
  return (
      <div>
          <button style={styles.btnContainer} onClick={() => setNotificationBtn(!notificationBtn)} >🔔
          { unreadCount > 0 && (
            <span style={styles.badge}>
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
          </button>
          {
              notificationBtn && (
                  <div style={styles.notificationContainer}>
                      <h2>notifications</h2>
                      <div style={styles.divider} />
                          {notifications.length === 0 ? 
                      
                              <div>no notification yet</div>
                              :
                              notifications.map((item) => (
                              <div 
                              key={item.id}
                              onClick={() => handleNotificationClick(item)}
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
  },
  badge: {
  position: 'absolute',
  top: '-2px',
  right: '-2px',
  backgroundColor: '#ff4d4f',
  color: 'white',
  borderRadius: '50%',
  padding: '4px 4px',
  fontSize: '11px',
  fontWeight: 'bold',
  minWidth: '10px',
  height: '10px',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
  }
}