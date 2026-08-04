import { useApp } from './context/useApp';
import AuthForm from './components/AuthForm';
import SwipesPage from './components/SwipesPage';
import AppNavDock from './components/AppNavDock';
import MessagesPage from './components/MessagePage';
import SettingsPage from './components/SettingsPage';
import MapPage from './components/MapPage';
import Header from './components/Header'

export default function App() {
  const { activeTab, isAuthenticated } = useApp();

  if (!isAuthenticated) {
    return <AuthForm />;
  }

  return (
    <div style={styles.appContainer}>
      <Header />
      <div style={styles.pageContainer}>
        {activeTab === 'map' && <MapPage />}

        {activeTab === 'swipes' && <SwipesPage />}

        {activeTab === 'messages' && <MessagesPage />}

        {activeTab === 'settings' && <SettingsPage />}

        <AppNavDock />
      </div>
    </div>
  );
}

const styles = {
  pageContainer:{
    paddingBottom: '90px', 
    minHeight: '100vh', 
    boxSizing: 'border-box',
    background:'#fff7f2'
  },
  appContainer:{
    minHeight: '100vh', 
    backgroundColor: '#fff7f2' 
  },

}

