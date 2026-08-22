import { useState, useEffect, useRef } from "react";
import { api } from "../services/api";

const DEFAULT_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='50' height='50' viewBox='0 0 24 24' fill='%23ccc'%3E%3Cpath d='M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z'/%3E%3C/svg%3E";

// Live-chat polling cadence. BASE is the steady-state interval; on repeated
// failures the poller doubles up to MAX so a struggling backend gets a break.
const BASE_POLL_MS = 3000;
const MAX_POLL_MS = 24000;

// Cheap fingerprint of a thread, used to skip re-rendering (and re-scrolling)
// when a poll returns exactly what is already on screen.
const signatureOf = (msgs) =>
  `${msgs.length}:${msgs.length ? msgs[msgs.length - 1].messageId : ""}`;
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
  // Guards for the live poller below: skip a tick while a send is in flight,
  // and remember the last thread we rendered so we only re-render on real change.
  const sendingRef = useRef(false);
  const threadSigRef = useRef("");

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
        const normalizedConvs = await Promise.all(
          (activeConvs || []).map(async (c) => {
            let photoUrl = DEFAULT_AVATAR;
            try {
              const photoRes = await api.fetchPhoto(c.otherUserId);
              photoUrl = photoRes?.photoUrl || DEFAULT_AVATAR;
            } catch (e) {
              console.warn(
                `Failed to fetch photo for user ${c.otherUserId}`,
                e,
              );
            }
            return {
              id: c.otherUserId,
              name: c.otherUsername || "User",
              avatarUrl: photoUrl,
              lastMessage: c.lastMessage || "",
              unread: (c.unreadCount || 0) > 0,
              unreadCount: c.unreadCount || 0,
            };
          }),
        );
        // Convert matches without active chats into conversation entries
        const unstartedMatches = (matchesData || []).filter(
          (m) =>
            !activeConvs.some((c) => (c.otherUserId || c.userId) === m.userId),
        );
        // Convert matches without active chats into conversation entries
        const matchConvs = await Promise.all(
          unstartedMatches.map(async (m) => {
            let photoUrl = DEFAULT_AVATAR;
            try {
              const photoRes = await api.fetchPhoto(m.userId);
              photoUrl = photoRes?.photoUrl || DEFAULT_AVATAR;
            } catch (e) {
              console.warn(`Failed to fetch photo for match ${m.userId}`, e);
            }
            return {
              id: m.userId,
              name: m.username,
              avatarUrl: photoUrl || DEFAULT_AVATAR,
              lastMessage: "🎉 New match! Say hi...",
              unread: false,
              unreadCount: 0,
            };
          }),
        );
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
   * Updates the sidebar preview + unread badge for one conversation.
   * Called only when a poll actually saw new messages, so the heavier
   * conversation query is never part of the steady-state polling cost.
   */
  const refreshConversationPreviews = (otherUserId, history) => {
    if (!history.length) return;
    const last = history[history.length - 1];
    const preview = last.sender === "me" ? `You: ${last.body}` : last.body;
    setConversations((prev) => {
      const current = prev.find((c) => c.id === otherUserId);
      if (!current) return prev;
      const rest = prev.filter((c) => c.id !== otherUserId);
      return [{ ...current, lastMessage: preview }, ...rest];
    });
  };

  /**
   * Keeps the open chat live: polls the selected thread so messages the other
   * user sends show up without switching tabs or re-opening the conversation.
   *
   * Four guards keep the request rate down:
   *   1. Runs only while a conversation is actually open.
   *   2. Pauses completely when the browser tab is hidden, and fetches once
   *      immediately when it becomes visible again.
   *   3. Schedules the next tick only after the previous one finishes, so a
   *      slow response delays the next request instead of stacking on it.
   *   4. Backs off on errors (3s -> 6s -> 12s -> 24s) and resets on the first
   *      success, so a struggling backend is not hammered.
   *
   * An idle open chat therefore costs one cheap request every 3 seconds:
   * GET /api/messages/with/{id}, which is a SELECT plus an UPDATE that matches
   * zero rows once the thread is already read.
   */
  useEffect(() => {
    if (!selectedChat) return;
    const otherUserId = selectedChat.id;

    let cancelled = false;
    let timer = null;
    let delay = BASE_POLL_MS;

    const schedule = () => {
      if (cancelled) return;
      timer = setTimeout(tick, delay);
    };

    const tick = async () => {
      if (cancelled) return;
      // Hidden tab: skip the request; the visibility handler resumes polling
      if (document.visibilityState !== "visible") return schedule();
      // A send is mid-flight and refreshes the thread itself when it lands
      if (sendingRef.current) return schedule();

      try {
        const history = await api.fetchMessages(otherUserId);
        if (cancelled) return;
        delay = BASE_POLL_MS;
        if (history) {
          const sig = signatureOf(history);
          // Nothing new: leave state alone so the view does not re-scroll
          if (sig !== threadSigRef.current) {
            threadSigRef.current = sig;
            setMessages(history);
            refreshConversationPreviews(otherUserId, history);
          }
        }
      } catch {
        delay = Math.min(delay * 2, MAX_POLL_MS);
      }
      schedule();
    };

    const onVisibility = () => {
      if (document.visibilityState === "visible") {
        clearTimeout(timer);
        delay = BASE_POLL_MS;
        tick();
      }
    };

    document.addEventListener("visibilitychange", onVisibility);
    schedule();

    return () => {
      cancelled = true;
      clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [selectedChat]);

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
          // Seed the poller signature so its first tick is a no-op
          threadSigRef.current = signatureOf(history);
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

    // Update the last message preview in the sidebar and move conversation to the top
    setConversations((prev) => {
      const currentConv = prev.find((conv) => conv.id === recipientId);
      const remainingConvs = prev.filter((conv) => conv.id !== recipientId);

      if (!currentConv) return prev;

      const updatedConv = {
        ...currentConv,
        lastMessage: `You: ${messageText}`,
      };

      return [updatedConv, ...remainingConvs];
    });
    // Hold the poller off so it cannot overwrite the optimistic message
    sendingRef.current = true;
    try {
      await api.sendMessage(recipientId, messageText);
      const updatedHistory = await api.fetchMessages(recipientId);
      if (updatedHistory) {
        threadSigRef.current = signatureOf(updatedHistory);
        setMessages(updatedHistory);
      }
    } catch (err) {
      console.error("Failed to send message:", err);
    } finally {
      sendingRef.current = false;
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
