import { useApp } from './context/useApp';
import AuthForm from './components/AuthForm';
import SwipesPage from './components/SwipesPage';
import AppNavDock from './components/AppNavDock';
import MessagesPage from './components/MessagePage';
import SettingsPage from './components/SettingsPage';
import MapPage from './components/MapPage';

export default function App() {
  const { activeTab, isAuthenticated } = useApp();

  if (!isAuthenticated) {
    return <AuthForm />;
  }

  return (
    <div style={{ paddingBottom: '90px', minHeight: '100vh', boxSizing: 'border-box' ,background:'#fff7f2'}}>
      {activeTab === 'map' && <MapPage />}

      {activeTab === 'swipes' && <SwipesPage />}

      {activeTab === 'messages' && <MessagesPage />}

      {activeTab === 'settings' && <SettingsPage />}

      <AppNavDock />
    </div>
  );
}

