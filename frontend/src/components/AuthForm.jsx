import { useState } from "react";
import { useApp } from "../context/useApp";
import { api } from "../services/api";


export default function AuthForm() {
  const [isLogin, setIsLogin] = useState(true);
  const [password, setPassword] = useState("");
  const [userName, setUserName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { login } = useApp();

  const DEFAULT_LOCATION = {
    lat: 32.0853,
    lng: 34.7818,
  };
  /**
   * Fetches the user's current GPS coordinates.
   * Resolves to null coordinates if permission is denied or unavailable,
   * ensuring the authentication flow never blocks due to location errors
   */
  const getBrowserLocation = () => {
    return new Promise((resolve) => {
      if (!navigator.geolocation) {
        resolve(DEFAULT_LOCATION);
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          });
        },
        () => {
          console.warn("Location permission denied, using default location (Tel Aviv)");
          resolve(DEFAULT_LOCATION);
        },
      );
    });
  };

  const handleSubmit = async (e) => {
    // Prevent default browser form refresh
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const location = await getBrowserLocation();
      // API service handles JSON formatting and throws automatically on !response.ok
      const data = isLogin
        ? await api.login(userName, password, location.lat, location.lng)
        : await api.register({
            username: userName,
            password: password,
            lat: location.lat,
            lng: location.lng,
          });

      // Save token and user session
      login(data.token, data.user || { username: userName });
    } catch (err) {
      setError(
        err.message || "An unexpected error occurred. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.pageWrapper}>
      <div style={styles.container}>
        <h1 style={styles.title}>ZuzDog</h1>
        <p style={styles.subtitle}>
          {isLogin
            ? "Sign in to meet dogs and owners nearby."
            : "Create an account to meet dogs nearby."}
        </p>

        {error && <div style={styles.errorText}>{error}</div>}

        <form onSubmit={handleSubmit} style={styles.form}>
          {
            <div style={styles.inputGroup}>
              <label style={styles.label}>User Name</label>
              <input
                type="text"
                placeholder="Username"
                value={userName}
                onChange={(e) => setUserName(e.target.value)}
                required
                style={styles.input}
              />
            </div>
          }
          <div style={styles.inputGroup}>
            <label style={styles.label}>Password</label>
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              style={styles.input}
            />
          </div>

          <button type="submit" disabled={loading} style={styles.submitBtn}>
            {loading ? "Loading..." : isLogin ? "Login" : "Register"}
          </button>
        </form>

        <div style={styles.switchContainer}>
          <button
            type="button"
            onClick={() => {
              setIsLogin(!isLogin);
              setError("");
            }}
            style={styles.switchBtn}
          >
            {isLogin ? "Create an account" : "Already have an account? Log in"}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  pageWrapper: {
    minHeight: "100vh",
    width: "100%",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    background:
      "linear-gradient(135deg, #fceeed 0%, #fef8e8 50%, #e2f8ec 100%)",
    padding: "20px",
  },
  container: {
    width: "100%",
    maxWidth: "380px",
    backgroundColor: "#1c212c",
    borderRadius: "16px",
    padding: "36px 32px",
    boxShadow: "0 20px 40px rgba(0, 0, 0, 0.15)",
    textAlign: "left",
    color: "#ffffff",
  },
  title: {
    fontSize: "24px",
    fontWeight: "700",
    margin: "0 0 6px 0",
    color: "#ffffff",
  },
  subtitle: {
    fontSize: "13px",
    color: "#8a93a6",
    margin: "0 0 24px 0",
  },
  errorText: {
    color: "#ff6b6b",
    backgroundColor: "rgba(255, 107, 107, 0.1)",
    padding: "10px",
    borderRadius: "8px",
    marginBottom: "16px",
    fontSize: "13px",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  inputGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  label: {
    fontSize: "12px",
    color: "#8a93a6",
  },
  input: {
    backgroundColor: "#12151d",
    border: "1px solid #282f3f",
    borderRadius: "8px",
    padding: "12px 14px",
    fontSize: "14px",
    color: "#ffffff",
    outline: "none",
  },
  submitBtn: {
    backgroundColor: "#ff7c5c",
    color: "#ffffff",
    border: "none",
    borderRadius: "24px",
    padding: "12px",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    marginTop: "10px",
    transition: "opacity 0.2s",
  },
  switchContainer: {
    marginTop: "20px",
    textAlign: "left",
  },
  switchBtn: {
    background: "none",
    border: "none",
    color: "#ff7c5c",
    cursor: "pointer",
    fontSize: "13px",
    padding: "0",
  },
};
