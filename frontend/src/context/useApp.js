import { useContext } from 'react';
import { AppContext } from './AppContext';

/**
 * Custom hook to consume the global AppContext
 * Ensures that components calling this hook are safely wrapped inside an <AppProvider>
 */
export const useApp = () => {
  const context = useContext(AppContext);
  // Safety check: throws a clear error if the hook is called outside of the Provider tree
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};