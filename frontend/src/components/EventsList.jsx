


export default function EventsList({ hangouts , onSelectHangout}) {
  return (
    <div style={styles.container}>
     
      <h2 style = {styles.title}>Events in your area</h2>
      <p style = {styles.subtitle}>Upcoming meetups near you</p>
      <div style={styles.divider} />
       {hangouts.length === 0 ? 
        (<div style = {styles.emptyText}>No meetups yet</div>)
        :
        (hangouts.map((item) => (
          <div 
          key={item.id}
          onClick={() => onSelectHangout && onSelectHangout(item)}
          style={styles.card}
          > 
            <div style = {styles.cardTitle}>title: {item.title}</div>
            <div style = {styles.cardText}>location: {item.locationName}</div>
            <div style = {styles.cardText}>time: {item.dateTime}</div>
          </div>))
        )
      }
      

      
    </div>
  );
}

const styles = {
  container: {
    width: '280px',
    backgroundColor: '#FAF5EE', 
    borderRadius: '20px',
    padding: '16px',
    border: '1.5px solid #E2D3C5',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  title: {
    fontSize: '18px',
    fontWeight: '700',
    color: '#5C3E21',
    margin: 0
  },
  subtitle: {
    fontSize: '12px',
    color: '#8C7A6B',
    margin: 0
  },
  divider: {
    height: '1px',
    backgroundColor: '#E2D3C5',
    margin: '4px 0'
  },
  emptyText: {
    fontSize: '13px',
    color: '#8C7A6B',
    padding: '8px 0'
  },
  card: {
    backgroundColor: '#FFFFFF',
    padding: '10px 12px',
    borderRadius: '12px',
    border: '1px solid #E2D3C5',
    cursor: 'pointer',
    display: 'flex',
    flexDirection: 'column',
    gap: '4px'
  },
  cardTitle: {
    fontWeight: 'bold',
    fontSize: '14px',
    color: '#5C3E21'
  },
  cardText: {
    fontSize: '12px',
    color: '#666'
  }
};