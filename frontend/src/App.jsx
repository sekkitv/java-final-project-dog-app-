import { useApp } from './context/useApp';
import AuthForm from './components/AuthForm';

function App() {
  const { isAuthenticated, logout, user } = useApp();

  if (!isAuthenticated) {
    return <AuthForm />;
  }

  return (
   <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #ccc', paddingBottom: '10px' }}>
        <h1>ZuzDog App</h1>
        <div>
          <span style={{ marginRight: '15px' }}>
            Hello, {user?.email || 'User'}
          </span>
          <button
            onClick={logout}
            style={{ padding: '6px 12px', cursor: 'pointer' }}
          >
            Logout
          </button>
        </div>
      </header>

      <main style={{ marginTop: '20px' }}>
        <p>Welcome to the protected area of the application!</p>
      </main>
    </div>
  );
}

export default App;