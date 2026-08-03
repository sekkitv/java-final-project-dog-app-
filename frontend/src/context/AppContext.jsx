/* eslint-disable react-refresh/only-export-components */
import  { createContext, useState } from 'react';

// Create and export the context so useApp.js can import it
export const AppContext = createContext();


export const AppProvider = ({ children }) => {
  // Using a callback (lazy initialization) so localStorage is read ONLY once on initial mount,
  // avoiding synchronous storage reads on every re-render
  const [token, setToken] = useState(() => localStorage.getItem('token') || null);
  const [user, setUser] = useState(null);

  //Current tab
  const [activeTab, setActiveTab] = useState('map');
  // Double negation (!!) explicitly casts the token string/null into a strict boolean
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
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
