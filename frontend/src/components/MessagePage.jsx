import { useState, useEffect } from 'react';
import { api } from '../services/api';

/**
 * MessagesPage Component
 * displays active conversations and handles chat threads with matched profiles.
 */
export default function MessagesPage() {
  const [conversations, setConversations] = useState([]);
  const [selectedChat, setSelectedChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(false);

  //Loading the conversations
  useEffect(() => {
    async function loadConversations() {
      try {
        setLoading(true);
        
         const data = await api.fetchConversations();
        if (data) {
            setConversations(data);
        }
      } catch (err) {
        console.error('Error loading conversations:', err);
      } finally {
        setLoading(false);
      }
    }
    loadConversations();
  }, []);

  
  const handleSelectConversation = async (conv) => {
    setSelectedChat(conv);
    try {
        const history = await api.fetchMessages(conv.id);
      if (history) {  
        setMessages(history);
      } 
    } catch (err) {
      console.error('Error fetching messages:', err);
    }
  };


  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedChat) return;

    const messageText = newMessage;
    setNewMessage('');

    const tempMsg = { id: Date.now(), sender: 'me', text: messageText };
    setMessages((prev) => [...prev, tempMsg]);

    try {
      if (api.sendMessage) {
        await api.sendMessage(selectedChat.id, messageText);
      }
    } catch (err) {
      console.error('Failed to send message:', err);
    }
  };

  if (loading) {
    return <div style={styles.loading}>Loading messages...</div>;
  }

  return (
    <div style={styles.container}>
      {!selectedChat ? (
        <div style={styles.listSection}>
          <h2 style={styles.header}>Messages</h2>
          {conversations.length === 0 ? (
            <div style={styles.empty}>
              <p>No matches yet. Keep swiping to start conversations! 🐾</p>
            </div>
          ) : (
            <div style={styles.conversationsList}>
              {conversations.map((conv) => (
                <div
                  key={conv.id}
                  onClick={() => handleSelectConversation(conv)}
                  style={styles.conversationCard}
                >
                  <img
                    src={conv.avatarUrl || 'https://via.placeholder.com/50'}
                    alt={conv.name}
                    style={styles.avatar}
                  />
                  <div style={styles.convInfo}>
                    <div style={styles.convName}>{conv.name}</div>
                    <div style={styles.lastMessage}>{conv.lastMessage || 'Click to chat'}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div style={styles.chatSection}>
          <div style={styles.chatHeader}>
            <button onClick={() => setSelectedChat(null)} style={styles.backButton} aria-label="Back">
             <svg 
                 width="22" 
                 height="22" 
                 viewBox="0 0 24 24" 
                 fill="none" 
                 stroke="#ff7e5f" 
                 strokeWidth="3.5" 
                 strokeLinecap="round" 
                 strokeLinejoin="round"
             >
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
             </svg>
            </button>

            <span style={styles.chatHeaderName}>{selectedChat.name}</span>
          </div>

          <div style={styles.messagesList}>
            {messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  ...styles.messageBubble,
                  alignSelf: msg.sender === 'me' ? 'flex-end' : 'flex-start',
                  backgroundColor: msg.sender === 'me' ? '#ff7e5f' : '#f0f0f0',
                  color: msg.sender === 'me' ? '#fff' : '#333',
                }}
              >
                {msg.text}
              </div>
            ))}
          </div>

          <form onSubmit={handleSendMessage} style={styles.inputForm}>
            <input
              type="text"
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              placeholder="Type a message..."
              style={styles.input}
            />
            <button type="submit" style={styles.sendButton}>
              Send
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

const styles = {
  container: {
    maxWidth: '500px',
    margin: '0 auto',
    padding: '16px',
    height: 'calc(100vh - 120px)',
    display: 'flex',
    flexDirection: 'column',
  },
  loading: {
    textAlign: 'center',
    marginTop: '40px',
    color: '#666',
  },
  header: {
    marginBottom: '20px',
    color: '#333',
  },
  empty: {
    textAlign: 'center',
    marginTop: '40px',
    color: '#888',
  },
  listSection: {
    flex: 1,
  },
  conversationsList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  conversationCard: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px',
    borderRadius: '12px',
    backgroundColor: '#fff',
    border: '1px solid #eee',
    cursor: 'pointer',
    boxShadow: '0 2px 5px rgba(0,0,0,0.03)',
  },
  avatar: {
    width: '50px',
    height: '50px',
    borderRadius: '50%',
    objectFit: 'cover',
  },
  convInfo: {
    display: 'flex',
    flexDirection: 'column',
  },
  convName: {
    fontWeight: 'bold',
    fontSize: '16px',
  },
  lastMessage: {
    fontSize: '13px',
    color: '#777',
    marginTop: '4px',
  },
  chatSection: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
  },
  chatHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    paddingBottom: '12px',
    borderBottom: '1px solid #eee',
  },
  backButton: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    padding: '6px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: '50%',
  },
  chatHeaderName: {
    fontWeight: 'bold',
    fontSize: '18px',
  },
  messagesList: {
    flex: 1,
    overflowY: 'auto',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    padding: '12px 0',
  },
  messageBubble: {
    padding: '10px 14px',
    borderRadius: '16px',
    maxWidth: '75%',
    wordBreak: 'break-word',
    fontSize: '14px',
  },
  inputForm: {
    display: 'flex',
    gap: '8px',
    paddingTop: '12px',
  },
  input: {
    flex: 1,
    padding: '10px 14px',
    borderRadius: '20px',
    border: '1px solid #ccc',
    outline: 'none',
  },
  sendButton: {
    padding: '10px 18px',
    borderRadius: '20px',
    border: 'none',
    backgroundColor: '#ff7e5f',
    color: '#fff',
    fontWeight: 'bold',
    cursor: 'pointer',
  },
};