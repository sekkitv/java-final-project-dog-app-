import { useState, useEffect } from "react";

import { api } from "../services/api";
import { useApp } from "../context/useApp";

/**
 * SettingsPage Component
 * Handles owner & dog profile information, photo uploads, and app preferences
 */
export default function SettingsPage() {
  // Lets ProfileSidebar know the profile changed
  const { refreshProfile } = useApp();
  // Owner State
  const [ownerName, setOwnerName] = useState("");
  const [ownerAge, setOwnerAge] = useState(25);
  const [ownerBio, setOwnerBio] = useState("");
  const [ownerImgUrl, setOwnerImgUrl] = useState("");
  const [ownerFile, setOwnerFile] = useState(null);
  // Dog State
  const [dogName, setDogName] = useState("");
  const [dogAge, setDogAge] = useState(3);
  const [dogBreed, setDogBreed] = useState("");
  const [dogBio, setDogBio] = useState("");
  const [dogImgUrl, setDogImgUrl] = useState("");
  const [dogFile, setDogFile] = useState(null);
  // Preference State
  const [distance, setDistance] = useState(25);
  // Status & Feedback States
  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  //Fetch all user and dog profile details on initial mount
  useEffect(() => {
    async function loadProfile() {
      try {
        const profile = await api.fetchProfile();
        if (profile) {
          //Owner
          if (profile.username) setOwnerName(profile.username);
          if (profile.userAge) setOwnerAge(profile.userAge);
          if (profile.description) setOwnerBio(profile.description);
          if (profile.maxDistance) setDistance(profile.maxDistance);
          if (profile.photoUrl) setOwnerImgUrl(profile.photoUrl);
          //Dog
          if (profile.dogName) setDogName(profile.dogName);
          if (profile.breed) setDogBreed(profile.breed);
          if (profile.dogAge) setDogAge(profile.dogAge);
          if (profile.dogDescription) setDogBio(profile.dogDescription);
          if (profile.dogPhotoUrl) setDogImgUrl(profile.dogPhotoUrl);
        }
      } catch (err) {
        console.error("Failed to load profile:", err);
      }
    }
    loadProfile();
  }, []);

  // Release the temporary preview once the server URL replaces it
  useEffect(() => {
    return () => {
      if (ownerImgUrl.startsWith("blob:")) URL.revokeObjectURL(ownerImgUrl);
    };
  }, [ownerImgUrl]);

  useEffect(() => {
    return () => {
      if (dogImgUrl.startsWith("blob:")) URL.revokeObjectURL(dogImgUrl);
    };
  }, [dogImgUrl]);

  /**
   * Submits updated profile and preference data to the API
   */
  const onSubmit = async (e) => {
    if (e) e.preventDefault();
    try {
      setLoading(true);
      setStatusMessage("");
      if (ownerFile) {
        // Show the URL the server saved, not the local preview
        const uploaded = await api.uploadOwnerPhoto(ownerFile);
        if (uploaded?.photoUrl) setOwnerImgUrl(uploaded.photoUrl);
        setOwnerFile(null);
      }
      if (dogFile) {
        const uploaded = await api.uploadDogPhoto(dogFile);
        if (uploaded?.dogPhotoUrl) setDogImgUrl(uploaded.dogPhotoUrl);
        setDogFile(null);
      }
      const data = {
        description: ownerBio || null,
        maxDistance: distance ? Number(distance) : null,
        dogName: dogName || null,
        breed: dogBreed || null,
        dogAge: dogAge ? parseInt(dogAge, 10) : null,
        dogDescription: dogBio || null,
        userAge: ownerAge ? parseInt(ownerAge, 10) : null,
      };
      const response = await api.updateProfile(data);
      if (response) {
        refreshProfile();
        setStatusMessage("Profile updated successfully! ✨");
      }
    } catch (err) {
      console.error("Error updating profile:", err);
      setStatusMessage("Failed to update profile.");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div style={styles.loading}>Saving changes...</div>;
  }

  return (
    <div style={styles.container}>
      <h1 style={styles.title}>Settings & Profile</h1>
      {statusMessage && (
        <div
          style={
            statusMessage.toLowerCase().includes("failed")
              ? styles.errorBadge
              : styles.statusBadge
          }
        >
          {statusMessage}
        </div>
      )}

      <div style={styles.sectionCard}>
        <h2 style={styles.sectionTitle}>👤 Owner Details</h2>

        <div style={styles.photoContainer}>
          <img
            src={
              ownerImgUrl ||
              "https://placehold.co/80x80/ff7e5f/white?text=Owner"
            }
            alt={ownerName || "Owner"}
            style={styles.avatar}
          />
          <label style={styles.uploadBtn}>
            Upload Photo
            <input
              type="file"
              accept="image/*"
              onChange={(e) => {
                const file = e.target.files[0];
                if (file) {
                  setOwnerFile(file);
                  setOwnerImgUrl(URL.createObjectURL(file));
                }
              }}
              style={{ display: "none" }}
            />
          </label>
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label}>Your Name:</label>
          <input
            type="text"
            value={ownerName}
            onChange={(e) => setOwnerName(e.target.value)}
            placeholder="Your name"
            style={styles.input}
          />
        </div>

        <div style={styles.sliderContainer}>
          <div style={styles.sliderHeader}>
            <label style={styles.label}>Your Age:</label>
            <span style={styles.sliderValue}>{ownerAge} years old</span>
          </div>
          <input
            type="range"
            min="18"
            max="80"
            value={ownerAge}
            onChange={(e) => setOwnerAge(e.target.value)}
            style={styles.rangeInput}
          />
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label}>About You:</label>
          <textarea
            value={ownerBio}
            onChange={(e) => setOwnerBio(e.target.value)}
            placeholder="Tell other dog owners a bit about yourself..."
            style={styles.textarea}
          />
        </div>
      </div>

      <div style={styles.sectionCard}>
        <h2 style={styles.sectionTitle}>🐶 Dog Details</h2>

        <div style={styles.photoContainer}>
          <img
            src={
              dogImgUrl || "https://placehold.co/80x80/ff7e5f/white?text=Dog"
            }
            alt={dogName || "Dog"}
            style={styles.avatar}
          />
          <label style={styles.uploadBtn}>
            Upload Photo
            <input
              type="file"
              accept="image/*"
              onChange={(e) => {
                const file = e.target.files[0];
                if (file) {
                  setDogFile(file);
                  setDogImgUrl(URL.createObjectURL(file));
                }
              }}
              style={{ display: "none" }}
            />
          </label>
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label}>Dog's Name:</label>
          <input
            type="text"
            value={dogName}
            onChange={(e) => setDogName(e.target.value)}
            placeholder="Dog's name"
            style={styles.input}
          />
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label}>Breed:</label>
          <input
            type="text"
            value={dogBreed}
            onChange={(e) => setDogBreed(e.target.value)}
            placeholder="e.g. Golden Retriever"
            style={styles.input}
          />
        </div>

        <div style={styles.sliderContainer}>
          <div style={styles.sliderHeader}>
            <label style={styles.label}>Dog's Age:</label>
            <span style={styles.sliderValue}>
              {dogAge} {dogAge === "1" ? "year" : "years"} old
            </span>
          </div>
          <input
            type="range"
            min="0"
            max="20"
            value={dogAge}
            onChange={(e) => setDogAge(e.target.value)}
            style={styles.rangeInput}
          />
        </div>

        <div style={styles.fieldGroup}>
          <label style={styles.label}>About Your Dog:</label>
          <textarea
            value={dogBio}
            onChange={(e) => setDogBio(e.target.value)}
            placeholder="Energy level, favorite games, friendly with other dogs?"
            style={styles.textarea}
          />
        </div>
      </div>

      <div style={styles.sectionCard}>
        <h2 style={styles.sectionTitle}>⚙️ Preferences</h2>
        <div style={styles.sliderContainer}>
          <div style={styles.sliderHeader}>
            <label style={styles.label}>Search Radius:</label>
            <span style={styles.sliderValue}>{distance} km</span>
          </div>
          <input
            type="range"
            min="1"
            max="50"
            value={distance}
            onChange={(e) => setDistance(e.target.value)}
            style={styles.rangeInput}
          />
        </div>
      </div>

      <button onClick={onSubmit} style={styles.submitBtn}>
        Save Profile
      </button>
    </div>
  );
}
/**
 * Component Styles
 */
const styles = {
  container: {
    maxWidth: "480px",
    margin: "20px auto",
    padding: "24px 20px",
    backgroundColor: "#ffffff",
    borderRadius: "20px",
    boxShadow: "0 8px 24px rgba(255, 126, 95, 0.12)",
    border: "2px solid #ffd8cc",
    display: "flex",
    flexDirection: "column",
    gap: "24px",
    boxSizing: "border-box",
  },
  title: {
    fontSize: "26px",
    fontWeight: "bold",
    color: "#2d3748",
    marginBottom: "4px",
    textAlign: "center",
  },
  loading: {
    textAlign: "center",
    marginTop: "40px",
    color: "#666",
  },
  statusBadge: {
    padding: "12px",
    borderRadius: "12px",
    backgroundColor: "#e6fffa",
    color: "#2e7d32",
    border: "1px solid #b2f5ea",
    textAlign: "center",
    fontWeight: "600",
    fontSize: "14px",
  },
  errorBadge: {
    padding: "12px",
    borderRadius: "12px",
    backgroundColor: "#ffebee",
    color: "#c62828",
    border: "1px solid #ffcdd2",
    textAlign: "center",
    fontWeight: "600",
    fontSize: "14px",
  },

  sectionCard: {
    backgroundColor: "#fffcfb",
    borderRadius: "16px",
    padding: "18px",
    border: "1.5px solid #ffedd8",
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  sectionTitle: {
    fontSize: "18px",
    fontWeight: "700",
    color: "#2d3748",
    margin: "0 0 6px 0",
    borderBottom: "2px solid #ffe3d1",
    paddingBottom: "6px",
  },

  photoContainer: {
    display: "flex",
    alignItems: "center",
    gap: "16px",
    marginBottom: "8px",
  },
  avatar: {
    width: "80px",
    height: "80px",
    borderRadius: "50%",
    objectFit: "cover",
    border: "3px solid #ff7e5f",
    boxShadow: "0 4px 10px rgba(255, 126, 95, 0.25)",
  },
  uploadBtn: {
    padding: "8px 16px",
    borderRadius: "20px",
    backgroundColor: "#fff0eb",
    color: "#ff7e5f",
    border: "1.5px solid #ffd8cc",
    fontSize: "13px",
    fontWeight: "600",
    cursor: "pointer",
    display: "inline-flex",
    alignItems: "center",
    gap: "6px",
    transition: "all 0.2s ease",
    boxShadow: "0 2px 6px rgba(255, 126, 95, 0.1)",
  },

  fieldGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    marginBottom: "4px",
  },
  label: {
    fontSize: "14px",
    fontWeight: "600",
    color: "#4a5568",
  },
  input: {
    padding: "12px 14px",
    borderRadius: "12px",
    border: "1.5px solid #ffd8cc",
    fontSize: "15px",
    outline: "none",
    backgroundColor: "#ffffff",
    transition: "border-color 0.2s ease",
    boxSizing: "border-box",
    width: "100%",
  },
  textarea: {
    padding: "12px 14px",
    borderRadius: "12px",
    border: "1.5px solid #ffd8cc",
    fontSize: "15px",
    outline: "none",
    backgroundColor: "#ffffff",
    minHeight: "85px",
    fontFamily: "inherit",
    resize: "vertical",
    boxSizing: "border-box",
    width: "100%",
  },

  sliderContainer: {
    display: "flex",
    flexDirection: "column",
    gap: "10px",
    backgroundColor: "#ffffff",
    padding: "14px",
    borderRadius: "12px",
    border: "1.5px solid #ffedd8",
  },
  sliderHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  },
  sliderValue: {
    color: "#ff7e5f",
    fontWeight: "bold",
    fontSize: "15px",
  },
  rangeInput: {
    accentColor: "#ff7e5f",
    cursor: "pointer",
    width: "100%",
  },

  submitBtn: {
    padding: "14px",
    borderRadius: "24px",
    border: "none",
    background: "linear-gradient(135deg, #ff7e5f, #feb47b)",
    color: "#fff",
    fontSize: "16px",
    fontWeight: "bold",
    cursor: "pointer",
    marginTop: "8px",
    boxShadow: "0 4px 14px rgba(255, 126, 95, 0.35)",
    transition: "transform 0.1s ease",
  },
};
