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
<<<<<<< HEAD
    //Auth
    register: (username, password) => apiRequest('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),
    login: (username, password) => apiRequest('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
    
    //Feed & Swipes
    fetchFeed: (limit) => apiRequest(`/api/feed${limit ? `?limit=${limit}` : ''}`),
    postSwipe: (targetId, action) => apiRequest('/api/swipe', { method: 'POST', body: JSON.stringify({ targetId, action }) }),
    fetchMatches: () => apiRequest('/api/matches'),
    
    //Profile
    fetchProfile: () => apiRequest('/api/profile'),
    updateProfile: (data) => apiRequest('/api/profile', { method: 'PUT', body: JSON.stringify(data) }),
    uploadOwnerPhoto: (file) => {
        const formData = new FormData();
        formData.append('photo', file);
        return apiRequest('/api/profile/photos/owner', { method: 'POST', body: formData });
    },
    uploadDogPhoto: (file) => {
        const formData = new FormData();
        formData.append('photo', file);
        return apiRequest('/api/profile/photos/dog', { method: 'POST', body: formData });
    },
    
    //Messages 
<<<<<<< Updated upstream
    fetchConversations: () => apiRequest('/api/messages/conversations'),
    fetchMessages: (otherUserId) => apiRequest(`/api/messages/with/${otherUserId}`),
    sendMessage: (otherUserId, body) => apiRequest(`/api/messages/with/${otherUserId}`, { method: 'POST', body: JSON.stringify({ body }) }),
=======
    //fetchConversations: () => apiRequest('/api/messages/conversations'),\
    //MOCK
    fetchConversations: async () => {
       return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
       {
          id: 'conv1',
          name: 'Luna & Sarah',
          avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100',
          lastMessage: 'Want to meet at the dog park tomorrow?',
          unread: true, 
          unreadCount: 2,
        },
        {
          id: 'conv2',
          name: 'Rex & David',
          avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100',
          lastMessage: 'Sounds good! See you then.',
          unread: false,
          unreadCount: 0,
        },
        ]);
      }, 300);
    });
  },
    //fetchMessages: (otherUserId) => apiRequest(`/api/messages/with/${otherUserId}`),
    //MOCK
    async fetchMessages() {
        return new Promise((resolve) => {
            setTimeout(() => {
             resolve([
                { id: 'm1', sender: 'them', text: 'Hey there! How is your dog doing?' },
                { id: 'm2', sender: 'me', text: 'Great! Always full of energy 🐶' },
                { id: 'm3', sender: 'them', text: 'Want to meet at the dog park tomorrow?' }
             ]);
             }, 200);
        });
    },
    //sendMessage: (otherUserId, body) => apiRequest(`/api/messages/with/${otherUserId}`, { method: 'POST', body: JSON.stringify({ body }) }),
    //MOCK
    async sendMessage(conversationId, text) {
        return new Promise((resolve) => {
            setTimeout(() => {
             resolve({ success: true, id: Date.now(), text, sender: 'me' });
            }, 100);
        });
    },

>>>>>>> Stashed changes
=======
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
>>>>>>> feature/ui-complete-pre-api

  //Feed & Swipes
  fetchFeed: (limit) =>
    apiRequest(`/api/feed${limit ? `?limit=${limit}` : ""}`),
  postSwipe: (targetId, action) =>
    apiRequest("/api/swipe", {
      method: "POST",
      body: JSON.stringify({ targetId, action }),
    }),
  fetchMatches: () => apiRequest("/api/matches"),

<<<<<<< HEAD
    //Helper
<<<<<<< Updated upstream
    mediaUrl: (url) => url ? `${BASE_URL}${url}` : '/default-avatar.png'
=======
    mediaUrl: (url) => url ? `${BASE_URL}${url}` : '/default-avatar.png',

    //fetchNotifications: () => apiRequest('/api/notifications'),
    //MOCK
    fetchNotifications: async () => {
     return [
        {
            id: '1',
            type: 'MATCH',
            title: 'new match',
            body: 'roki and dannys dog matching',
            createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
            isRead: false
        },
        {
            id: '2',
            type: 'HANGOUT_JOIN',
            title: 'new meeting around',
            body: 'omer joined meeting',
            createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
            isRead: true
        }
    ];
    },
    //markNotificationsRead:(id) => apiRequest(`/api/notifications/${id}` ,{ method: 'POST' });
    //MOCK
    markNotificationsRead: async (id) => {
        try {
            return await apiRequest(`/api/notifications/${id}/read`, { method: 'POST' });
        } catch (e) {
            return { success: true };
        }
    }


   
>>>>>>> Stashed changes
};
=======
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
>>>>>>> feature/ui-complete-pre-api
