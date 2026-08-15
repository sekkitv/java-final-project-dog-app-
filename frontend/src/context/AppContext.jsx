/* eslint-disable react-refresh/only-export-components */
<<<<<<< HEAD
import  { createContext, useState ,useEffect} from 'react';
import {api} from '../services/api'
// Create and export the context so useApp.js can import it
export const AppContext = createContext();


=======
import { createContext, useState, useEffect } from "react";
import { api } from "../services/api";
// Create and export the context so useApp.js can import it
export const AppContext = createContext();

>>>>>>> feature/ui-complete-pre-api
/**
 * AppProvider Component
 * Provides global state and actions to child components via AppContext.Provider
 * Encapsulates session persistence, active view tracking, and notification updates
 */
export const AppProvider = ({ children }) => {
  // Lazy initialization ensures localStorage is read only on initial mount
<<<<<<< HEAD
  const [token, setToken] = useState(() => localStorage.getItem('token') || null);
  const [user, setUser] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [activeTab, setActiveTab] = useState('map');
=======
  const [token, setToken] = useState(
    () => localStorage.getItem("token") || null,
  );
  const [user, setUser] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [activeTab, setActiveTab] = useState("map");
>>>>>>> feature/ui-complete-pre-api

  // Strict boolean indicating user authentication status
  const isAuthenticated = !!token;

<<<<<<< HEAD

=======
>>>>>>> feature/ui-complete-pre-api
  /**
   * Persists authentication token to browser storage and updates global state
   */
  const login = (newToken, userData = null) => {
<<<<<<< HEAD
    localStorage.setItem('token', newToken);
=======
    localStorage.setItem("token", newToken);
>>>>>>> feature/ui-complete-pre-api
    setToken(newToken);
    if (userData) {
      setUser(userData);
    }
  };

  /**
   * Clears session data from both storage and state
   */
  const logout = () => {
<<<<<<< HEAD
    localStorage.removeItem('token');
=======
    localStorage.removeItem("token");
>>>>>>> feature/ui-complete-pre-api
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
<<<<<<< HEAD
        try {
            const data = await api.fetchNotifications();
            setNotifications(data);
        }
        catch(e){
            console.error('Failed to fetch notifications:' , e);
        }
    }
=======
      try {
        const data = await api.fetchNotifications();
        setNotifications(data.notifications || []);
      } catch (e) {
        console.error("Failed to fetch notifications:", e);
      }
    };
>>>>>>> feature/ui-complete-pre-api
    loadUserNotifications();
    const intervalId = setInterval(loadUserNotifications, 5000);
    return () => clearInterval(intervalId);
  }, [isAuthenticated]);

  /**
   * Marks a specific notification as read in the backend and updates local state
   */
<<<<<<< HEAD
  const markAsRead = async (id) => {
    try {
      await api.markNotificationsRead(id);
      setNotifications((prev) => prev.map((item) => (item.id === id ? { ...item, isRead: true } : item)));
    } catch (e) {
      console.error('Failed to mark read:', e);
    }
  };


=======
  const markAsRead = async () => {
    try {
      const response = await api.markNotificationsRead();
      setNotifications(response.notifications);
    } catch (e) {
      console.error("Failed to mark read:", e);
    }
  };
>>>>>>> feature/ui-complete-pre-api

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
