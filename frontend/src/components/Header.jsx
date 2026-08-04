import NotificationBell from "./NotificationBell";


export default function Header() {

 

    return(
        <div style={styles.headerWrapper}>
            <div style={styles.container}>
                <div />
                <div style={styles.centerGroup}>
                    <h1 style={styles.title}>ZuzDog</h1>
                    <div style={styles.subtitle}>Find friends, events & dog spots near you</div>
                </div>
                <div>
                    <NotificationBell/>
                </div>
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
        height: '2px',
        backgroundColor: '#ffe0cc',
        width: '100%',
    },
   
    
};