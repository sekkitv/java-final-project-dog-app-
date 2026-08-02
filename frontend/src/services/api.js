const BASE_URL = '';

const getToken = () => localStorage.getItem('token');

/**
 * Centralized HTTP request handler
 * Automatically attaches authentication headers, formats JSON payloads,
 * handles FormData for file uploads, and throws structured errors on failure
 */
async function apiRequest(endpoint, options = {}) {
    const headers = { ...options.headers };

   // Set default Content-Type to JSON unless sending FormData (file uploads require browser-generated boundaries)
    if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }

    // Automatically inject Bearer token if session exists
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(`${BASE_URL}${endpoint}`, {
            ...options,
            headers,
        }); 
        
        // Safely parse JSON only if the server explicitly returned a JSON Content-Type
        const isJson = response.headers.get('content-type')?.includes('application/json');
        const data = isJson ? await response.json() : null;

        if (!response.ok) {
            const errorMessage = data?.message || response.statusText || 'Server Error';
            throw new Error(errorMessage);
        }

        return data;
    } catch (error) {
        console.error(`API Error [${options.method || 'GET'} ${endpoint}]:`, error.message);
        throw error;
    }
}

export const api = {
    //Auth
    register: (username, password) => apiRequest('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),
    login: (username, password) => apiRequest('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
 
    //Feed & Swipes
    //fetchFeed: (limit) => apiRequest(`/api/feed${limit ? `?limit=${limit}` : ''}`),
    //MOCK
   fetchFeed: async () => [
      {
            id: 3,
            username: 'maya_tlv',
            ownerName: 'Maya',
            ownerAge: 28,
            ownerPhotoUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb',
            ownerBio: 'Coffee lover & beach enthusiast!',
            distance: 2.5,
            
            dogName: 'Charlie',
            dogAge: 2,
            dogBreed: 'Corgi',
            dogPhotoUrl: 'https://images.unsplash.com/photo-1552053831-71594a27632d',
            dogBio: 'Loves chasing frisbees and getting belly rubs!'
        },
        {
            id: 4,
            username: 'yoni_dad',
            ownerName: 'Yoni',
            ownerAge: 32,
            ownerPhotoUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d',
            ownerBio: 'Always up for weekend park walks.',
            distance: 1.2,
            
            dogName: 'Blake',
            dogAge: 3,
            dogBreed: 'Golden Retriever',
            dogPhotoUrl: 'https://images.unsplash.com/photo-1537151625747-768eb6cf92b2',
            dogBio: 'Super friendly, loves playdates and treats.'
        }
    ],
    //postSwipe: (targetId, action) => apiRequest('/api/swipe', { method: 'POST', body: JSON.stringify({ targetId, action }) }),
    //MOCK
    postSwipe: async (targetId, action) => {
        console.log(`[Mock API] Swiped ${action} on targetId: ${targetId}`);
        return {
            success: true,
            isMatch: action === 'LIKE' 
        };
    },
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
    //fetchConversations: () => apiRequest('/api/messages/conversations'),\
    //MOCK
    fetchConversations: async () => {
       return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
          {
            id: 'conv-1',
            name: 'Luna & Rex',
            avatarUrl: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=150',
            lastMessage: 'Hey! Want to meet at the dog park tomorrow?',
            updatedAt: '10:42 AM'
          },
          {
            id: 'conv-2',
            name: 'Bella',
            avatarUrl: 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=150',
            lastMessage: 'Awesome, see you there! 🐾',
            updatedAt: 'Yesterday'
          }
        ]);
      }, 300);
    });
  },
    //fetchMessages: (otherUserId) => apiRequest(`/api/messages/with/${otherUserId}`),
    //MOCK
    async fetchMessages(conversationId) {
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


    //Hangouts
    //fetchHangouts: () => apiRequest('/api/hangouts'),
    //MOCK
    fetchHangouts: async () => {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve([
                    {
                        id: 'h1',
                        title: 'Golden Retrievers Playdate 🎾',
                        locationName: 'HaYarkon Dog Park',
                        dateTime: 'Today, 17:00',
                        organizer: 'Alex & Max',
                        lat: 32.0853,
                        lng: 34.7818,
                        participantsCount: 4,
                        description: 'Casual afternoon playdate for energetic dogs!'
                    },
                    {
                        id: 'h2',
                        title: 'Small Dogs Chill & Walk 🐾',
                        locationName: 'Meir Park',
                        dateTime: 'Tomorrow, 10:00',
                        organizer: 'Sarah & Luna',
                        lat: 32.0735,
                        lng: 34.7731,
                        participantsCount: 2,
                        description: 'Gentle morning walk for smaller breeds.'
                    }
                ]);
            }, 200);
        });
    },
    createHangout: (data) => apiRequest('/api/hangouts', { method: 'POST', body: JSON.stringify(data) }),
    signupHangout: (id) => apiRequest(`/api/hangouts/${id}/signup`, { method: 'POST' }),
    cancelHangoutSignup: (id) => apiRequest(`/api/hangouts/${id}/signup`, { method: 'DELETE' }),

    //Helper
    mediaUrl: (url) => url ? `${BASE_URL}${url}` : '/default-avatar.png'



   
};