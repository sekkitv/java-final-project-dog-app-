import { useState, useEffect, useRef } from "react";
import { api } from "../services/api";

const DEFAULT_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='50' height='50' viewBox='0 0 24 24' fill='%23ccc'%3E%3Cpath d='M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z'/%3E%3C/svg%3E";
/**
 * MessagesPage Component
 * Manages conversation list and real-time chat interactions with matched users
 */
export default function MessagesPage() {
  const [conversations, setConversations] = useState([]);
  const [selectedChat, setSelectedChat] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  // Auto scroll to the bottom of the chat when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  /**
   * Fetches active conversations and new matches on mount,
   * normalizing them into a unified list without duplicates
   */
  useEffect(() => {
    async function loadAllChats() {
      try {
        setLoading(true);
        const activeConvs = await api.fetchConversations();
        const matchesData = await api.fetchMatches();
        // Normalize active conversation records from backend
        const normalizedConvs = (activeConvs || []).map((c) => ({
          id: c.otherUserId,
          name: c.otherUsername || "User",
          avatarUrl: c.photoUrl || DEFAULT_AVATAR,
          lastMessage: c.lastMessage || "",
          unread: (c.unreadCount || 0) > 0,
          unreadCount: c.unreadCount || 0,
        }));
        // Convert matches without active chats into conversation entries
        const matchConvs = matchesData
          .filter(
            (m) =>
              !activeConvs.some(
                (c) => (c.otherUserId || c.userId) === m.userId,
              ),
          )
          .map((m) => ({
            id: m.userId,
            name: m.username,
            avatarUrl: m.photoUrl || DEFAULT_AVATAR,
            lastMessage: "🎉 New match! Say hi...",
            unread: false,
            unreadCount: 0,
          }));
        setConversations([...matchConvs, ...normalizedConvs]);
      } catch (err) {
        console.error("Error loading conversations:", err);
      } finally {
        setLoading(false);
      }
    }
    loadAllChats();
  }, []);

  /**
   * Handles conversation selection, marks it as read, and fetches chat history
   */
  const handleSelectConversation = async (conv) => {
    setSelectedChat(conv);
    // Reset unread indicators locally
    setConversations((prev) =>
      prev.map((c) =>
        c.id === conv.id ? { ...c, unread: false, unreadCount: 0 } : c,
      ),
    );
    const targetId = conv.id;
    if (targetId) {
      try {
        const history = await api.fetchMessages(targetId);
        if (history) {
          setMessages(history);
        }
      } catch (err) {
        console.error("Error fetching messages:", err);
      }
    }
  };

  /**
   * Sends a message with optimistic UI updates and synchronizes with the server
   */
  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedChat) return;
    const recipientId = selectedChat.id;
    const messageText = newMessage;
    setNewMessage("");

    // Optimistic message update for instant UI feedback
    const tempId = `temp-${Date.now()}`;
    const tempMsg = {
      messageId: tempId,
      sender: "me",
      body: messageText,
      sentAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempMsg]);

    // Update the last message preview in the sidebar
    setConversations((prev) =>
      prev.map((conv) => {
        const convId = conv.id;
        if (convId === recipientId) {
          return {
            ...conv,
            lastMessage: `You: ${messageText}`,
          };
        }
        return conv;
      }),
    );
    try {
      await api.sendMessage(recipientId, messageText);
      const updatedHistory = await api.fetchMessages(recipientId);
      if (updatedHistory) {
        setMessages(updatedHistory);
      }
    } catch (err) {
      console.error("Failed to send message:", err);
    }
  };

  if (loading) {
    return <div style={styles.loading}>Loading messages...</div>;
  }

  return (
    <div style={styles.mainLayout}>
      {/* Sidebar: Conversation List */}
      <div style={styles.sidebarSection}>
        <h2 style={styles.header}>Messages</h2>
        {conversations.length === 0 ? (
          <div style={styles.empty}>
            <p>No matches yet. Keep swiping to start conversations! 🐾</p>
          </div>
        ) : (
          <div style={styles.conversationsList}>
            {conversations.map((conv) => {
              const convKey = conv.id;
              const isSelected = selectedChat?.id === convKey;
              return (
                <div
                  key={convKey}
                  onClick={() => handleSelectConversation(conv)}
                  style={{
                    ...styles.conversationCard,
                    backgroundColor: isSelected
                      ? "#FFE8D6"
                      : conv.unread
                        ? "#fff9f5"
                        : "#ffffff",
                    borderColor: isSelected ? "#FF7A65" : "#eee",
                  }}
                >
                  <div style={styles.avatarWrapper}>
                    <img
                      src={conv.avatarUrl || DEFAULT_AVATAR}
                      alt={conv.name}
                      style={styles.avatar}
                    />
                    {conv.unread && <span style={styles.unreadDot} />}
                  </div>
                  <div style={styles.convInfo}>
                    <div
                      style={{
                        ...styles.convName,
                        fontWeight: conv.unread || isSelected ? "700" : "600",
                      }}
                    >
                      {conv.name}
                    </div>
                    <div
                      style={{
                        ...styles.lastMessage,
                        color: conv.unread ? "#4A3222" : "#777",
                        fontWeight: conv.unread ? "600" : "400",
                      }}
                    >
                      {conv.lastMessage || "Click to chat"}
                    </div>
                  </div>
                  {conv.unreadCount > 0 && (
                    <span style={styles.unreadBadge}>{conv.unreadCount}</span>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Main Chat Area */}
      <div style={styles.chatSection}>
        {selectedChat ? (
          <>
            <div style={styles.chatHeader}>
              <img
                src={selectedChat.avatarUrl || DEFAULT_AVATAR}
                alt={selectedChat.name}
                style={styles.chatHeaderAvatar}
              />
              <span style={styles.chatHeaderName}>{selectedChat.name}</span>
            </div>

            <div style={styles.messagesList}>
              {messages.map((msg) => {
                const targetId = selectedChat.id;
                const isMe = msg.sender === "me" || msg.senderId !== targetId;
                return (
                  <div
                    key={msg.messageId}
                    style={{
                      ...styles.messageBubble,
                      alignSelf: isMe ? "flex-end" : "flex-start",
                      backgroundColor: isMe ? "#FF7A65" : "#f0f0f0",
                      color: isMe ? "#fff" : "#333",
                    }}
                  >
                    {msg.body}
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
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
          </>
        ) : (
          <div style={styles.noChatSelected}>
            <p>Select a conversation to start chatting 💬</p>
          </div>
        )}
      </div>
    </div>
  );
}

const styles = {
  mainLayout: {
    display: "grid",
    gridTemplateColumns: "320px 1fr",
    gap: "20px",
    maxWidth: "1200px",
    margin: "10px auto 90px auto",
    width: "95%",
    height: "calc(100vh - 220px)",
    minHeight: "480px",
    backgroundColor: "#ffffff",
    borderRadius: "24px",
    border: "2px solid #2D3748",
    padding: "20px",
    boxSizing: "border-box",
    boxShadow: "0 8px 24px rgba(0,0,0,0.05)",
  },
  sidebarSection: {
    borderRight: "1.5px solid #eee",
    paddingRight: "16px",
    display: "flex",
    flexDirection: "column",
    overflowY: "auto",
  },
  loading: {
    textAlign: "center",
    marginTop: "40px",
    color: "#666",
  },
  header: {
    marginBottom: "20px",
    color: "#333",
  },
  empty: {
    textAlign: "center",
    marginTop: "40px",
    color: "#888",
  },
  conversationsList: {
    display: "flex",
    flexDirection: "column",
    gap: "12px",
  },
  conversationCard: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
    padding: "12px",
    borderRadius: "16px",
    backgroundColor: "#fff",
    border: "1.5px solid #eee",
    cursor: "pointer",
    boxShadow: "0 2px 8px rgba(0,0,0,0.03)",
    position: "relative",
    transition: "all 0.2s ease",
    height: "75px",
    boxSizing: "border-box",
  },
  avatarWrapper: {
    position: "relative",
    display: "inline-block",
  },
  avatar: {
    width: "50px",
    height: "50px",
    borderRadius: "50%",
    objectFit: "cover",
  },
  convInfo: {
    display: "flex",
    flex: 1,
    flexDirection: "column",
  },
  convName: {
    fontWeight: "bold",
    fontSize: "16px",
  },
  lastMessage: {
    fontSize: "13px",
    color: "#777",
    marginTop: "4px",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
    maxWidth: "180px",
  },
  chatSection: {
    display: "flex",
    flexDirection: "column",
    height: "100%",
    overflow: "hidden",
  },
  chatHeader: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
    paddingBottom: "12px",
    borderBottom: "1px solid #eee",
  },
  chatHeaderName: {
    fontWeight: "bold",
    fontSize: "18px",
  },
  messagesList: {
    flex: 1,
    overflowY: "auto",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    padding: "12px 0",
  },
  messageBubble: {
    padding: "10px 14px",
    borderRadius: "16px",
    maxWidth: "75%",
    wordBreak: "break-word",
    fontSize: "14px",
  },
  inputForm: {
    display: "flex",
    gap: "8px",
    paddingTop: "12px",
  },
  input: {
    flex: 1,
    padding: "10px 14px",
    borderRadius: "20px",
    border: "1px solid #ccc",
    outline: "none",
  },
  sendButton: {
    padding: "10px 18px",
    borderRadius: "20px",
    border: "none",
    backgroundColor: "#ff7e5f",
    color: "#fff",
    fontWeight: "bold",
    cursor: "pointer",
  },
  unreadDot: {
    position: "absolute",
    top: "2px",
    right: "2px",
    width: "12px",
    height: "12px",
    backgroundColor: "#ff4d4f",
    borderRadius: "50%",
    border: "2px solid white",
  },
  unreadBadge: {
    backgroundColor: "#ff7e5f",
    color: "white",
    borderRadius: "12px",
    padding: "2px 8px",
    fontSize: "12px",
    fontWeight: "bold",
  },
  chatHeaderAvatar: {
    width: "40px",
    height: "40px",
    borderRadius: "50%",
    objectFit: "cover",
  },
  noChatSelected: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    height: "100%",
    color: "#888",
    fontSize: "16px",
  },
};
