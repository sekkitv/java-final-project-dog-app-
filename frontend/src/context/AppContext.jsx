/* eslint-disable react-refresh/only-export-components */
import  { createContext, useState ,useEffect} from 'react';
import {api} from '../services/api'
// Create and export the context so useApp.js can import it
export const AppContext = createContext();


/**
 * AppProvider Component
 * Provides global state and actions to child components via AppContext.Provider
 * Encapsulates session persistence, active view tracking, and notification updates
 */
export const AppProvider = ({ children }) => {
  // Lazy initialization ensures localStorage is read only on initial mount
  const [token, setToken] = useState(() => localStorage.getItem('token') || null);
  const [user, setUser] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [activeTab, setActiveTab] = useState('map');

  // Strict boolean indicating user authentication status
  const isAuthenticated = !!token;


  /**
   * Persists authentication token to browser storage and updates global state
   */
  const login = (newToken, userData = null) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    if (userData) {
      setUser(userData);
    }
  };

  /**
   * Clears session data from both storage and state
   */
  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  /**
   * Background polling effect for fetching user notifications
   * Triggers an initial fetch on authentication and polls every 5 seconds
   */
  useEffect(() => {
    if (!isAuthenticated) return;
    const loadUserNotifications = async () => {
        try {
            const data = await api.fetchNotifications();
            setNotifications(data);
        }
        catch(e){
            console.error('Failed to fetch notifications:' , e);
        }
    }
    loadUserNotifications();
    const intervalId = setInterval(loadUserNotifications, 5000);
    return () => clearInterval(intervalId);
  }, [isAuthenticated]);

  /**
   * Marks a specific notification as read in the backend and updates local state
   */
  const markAsRead = async (id) => {
    try {
      await api.markNotificationsRead(id);
      setNotifications((prev) => prev.map((item) => (item.id === id ? { ...item, isRead: true } : item)));
    } catch (e) {
      console.error('Failed to mark read:', e);
    }
  };



  return (
    <AppContext.Provider
      value={{
        token,
        user,
        isAuthenticated,
        login,
        logout,
        activeTab,
        setActiveTab,
        notifications,
        markAsRead,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};