export default function EventsList({ hangouts, onSelectHangout }) {
  // Format ISO/date string to local Israeli date and time (DD/MM/YYYY, HH:mm)
  const formatEventTime = (timeString) => {
    if (!timeString) return "N/A";
    if (!timeString.includes("T") && !timeString.includes("-")) {
      return timeString;
    }
    const date = new Date(timeString);
    if (isNaN(date.getTime())) return timeString;

    return date.toLocaleString("he-IL", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };
  const meetups = hangouts.filter((item) => item.activityType === "MEETUP");
  return (
    <div style={styles.container}>
      <h2 style={styles.title}>Events in your area</h2>
      <p style={styles.subtitle}>Upcoming meetups near you</p>
      <div style={styles.divider} />
      <div style={styles.scrollList}>
        {meetups.length === 0 ? (
          <div style={styles.emptyText}>No meetups yet</div>
        ) : (
          meetups.map((item, index) => {
            const hangoutId = item.hangoutId || index;
            const desc = item.description || "No description provided";
            const time = item.eventTime || "N/A";

            return (
              <div
                key={hangoutId}
                onClick={() => onSelectHangout && onSelectHangout(item)}
                style={styles.card}
              >
                <div style={styles.cardTitle}>{item.title}</div>
                <div style={styles.cardText}>{desc}</div>
                <div style={styles.cardText}>{formatEventTime(time)}</div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

const styles = {
  container: {
    width: "280px",
    backgroundColor: "#FAF5EE",
    borderRadius: "20px",
    padding: "16px",
    border: "1.5px solid #E2D3C5",
    boxSizing: "border-box",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  title: {
    fontSize: "18px",
    fontWeight: "700",
    color: "#5C3E21",
    margin: 0,
  },
  subtitle: {
    fontSize: "12px",
    color: "#8C7A6B",
    margin: 0,
  },
  divider: {
    height: "1px",
    backgroundColor: "#E2D3C5",
    margin: "4px 0",
  },
  emptyText: {
    fontSize: "13px",
    color: "#8C7A6B",
    padding: "8px 0",
  },
  card: {
    backgroundColor: "#FFFFFF",
    padding: "10px 12px",
    borderRadius: "12px",
    border: "1px solid #E2D3C5",
    cursor: "pointer",
    display: "flex",
    flexDirection: "column",
    gap: "4px",
  },
  cardTitle: {
    fontWeight: "bold",
    fontSize: "14px",
    color: "#5C3E21",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
  },
  cardText: {
    fontSize: "12px",
    color: "#666",
  },
  scrollList: {
    maxHeight: "575px",
    overflowY: "auto",
    paddingRight: "6px",
  },
};
