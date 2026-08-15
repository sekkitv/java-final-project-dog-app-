const BASE_URL = "";

const getToken = () => localStorage.getItem("token");

/**
 * Centralized HTTP request handler
 * Automatically attaches authentication headers, formats JSON payloads,
 * handles FormData for file uploads, and throws structured errors on failure
 */
async function apiRequest(endpoint, options = {}) {
  const headers = { ...options.headers };

  // Set default Content-Type to JSON unless sending FormData (file uploads require browser-generated boundaries)
  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers["Content-Type"]
  ) {
    headers["Content-Type"] = "application/json";
  }

  // Automatically inject Bearer token if session exists
  const token = getToken();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    // Safely parse JSON only if the server explicitly returned a JSON Content-Type
    const isJson = response.headers
      .get("content-type")
      ?.includes("application/json");
    const data = isJson ? await response.json() : null;

    if (!response.ok) {
      const errorMessage =
        data?.message || response.statusText || "Server Error";
      throw new Error(errorMessage);
    }

    return data;
  } catch (error) {
    console.error(
      `API Error [${options.method || "GET"} ${endpoint}]:`,
      error.message,
    );
    throw error;
  }
}

export const api = {
  //Auth
  register: (userData) =>
    apiRequest("/auth/register", {
      method: "POST",
      body: JSON.stringify(userData),
    }),
  login: (username, password, lat = null, lng = null) =>
    apiRequest("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password, lat, lng }),
    }),

  //Feed & Swipes
  fetchFeed: (limit) =>
    apiRequest(`/api/feed${limit ? `?limit=${limit}` : ""}`),
  postSwipe: (targetId, action) =>
    apiRequest("/api/swipe", {
      method: "POST",
      body: JSON.stringify({ targetId, action }),
    }),
  fetchMatches: () => apiRequest("/api/matches"),

  //Profile
  fetchProfile: () => apiRequest("/api/profile"),
  updateProfile: (data) =>
    apiRequest("/api/profile", { method: "PUT", body: JSON.stringify(data) }),
  uploadOwnerPhoto: (file) => {
    const formData = new FormData();
    formData.append("photo", file);
    return apiRequest("/api/profile/photos/owner", {
      method: "POST",
      body: formData,
    });
  },
  uploadDogPhoto: (file) => {
    const formData = new FormData();
    formData.append("photo", file);
    return apiRequest("/api/profile/photos/dog", {
      method: "POST",
      body: formData,
    });
  },

  //Messages
  fetchConversations: () => apiRequest("/api/messages/conversations"),
  fetchMessages: (otherUserId) =>
    apiRequest(`/api/messages/with/${otherUserId}`),
  sendMessage: (otherUserId, body) =>
    apiRequest(`/api/messages/with/${otherUserId}`, {
      method: "POST",
      body: JSON.stringify({ body }),
    }),

  //Hangouts
  fetchHangouts: () => apiRequest("/api/hangouts"),
  createHangout: (data) =>
    apiRequest("/api/hangouts", { method: "POST", body: JSON.stringify(data) }),
  signupHangout: (id) =>
    apiRequest(`/api/hangouts/${id}/signup`, { method: "POST" }),
  cancelHangoutSignup: (id) =>
    apiRequest(`/api/hangouts/${id}/signup`, { method: "DELETE" }),
  userHangouts: () => apiRequest("/api/hangouts/mine"),

  //Notifications
  fetchNotifications: () => apiRequest("/api/notifications"),
  markNotificationsRead: () =>
    apiRequest("/api/notifications/read", { method: "POST" }),
};
