



export default function Header() {
    return(
        <div style={styles.headerWrapper}>
            <div style={styles.container}>
                <div />
                <div style={styles.centerGroup}>
                    <h1 style={styles.title}>ZuzDog</h1>
                    <div style={styles.subtitle}>Find friends, events & dog spots near you</div>
                </div>
                <button style={styles.btnContainer} >🔔</button>
            </div>
             <div style={styles.divider} />
        </div>
    );
};

const styles = {
    headerWrapper: {
        width: '100%',
        backgroundColor: '#FAF5EE'
    },
    container:{
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent:'space-between',
        padding: '12px 24px',
        boxSizing: 'border-box'
    },
    centerGroup: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center'
    },
    title:{
        fontSize: '40px',
        fontWeight: '800',
        color: '#4A3222',
        textAlign: 'center',
        margin: 0
    },
    subtitle:{
        fontSize: '15px',
        textAlign: 'center',
        color: '#8C7A6B',
        marginTop: '5px'
    },
    divider: {
        height: '1px',
        backgroundColor: '#E2D3C5',
        width: '100%',
    },
    btnContainer:{
        width:'44px',
        height:'44px',
        borderRadius:'50%',
        background:'white',
        border: '2px solid #ffe0cc',
        fontSize:'20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: 'pointer',
        boxShadow: '0 2px 6px rgba(0,0,0,0.06)'
    }
};