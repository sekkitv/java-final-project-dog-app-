
import com.zuzdog.model.Swipe;
import com.zuzdog.model.SwipeAction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SwipeDao {


      // converts a database row into a swipe type object 
    private static final RowMapper<Swipe> SWIPE_ROW_MAPPER = (rs, rowNum) -> {
        Swipe s = new Swipe();
        s.setSenderId(rs.getLong("sender_id"));
        s.setTargetId(rs.getLong("target_id"));
        s.setAction(SwipeAction.valueOf(rs.getString("action")));
        s.setSwipedAt(JdbcMappingUtils.getInstant(rs, "swiped_at"));
        return s;
    };

    private final JdbcTemplate jdbcTemplate;
      
    public SwipeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // add new swipe record take care of if user already swipe on the same traget_id 
    public int insert(long senderId, long targetId, SwipeAction action) {
        String sql = """
                INSERT INTO swipes (sender_id, target_id, action, swiped_at)
                VALUES (?, ?, ?, NOW())
                ON CONFLICT (sender_id, target_id)
                DO UPDATE SET action = EXCLUDED.action, swiped_at = NOW()
                """;
        return jdbcTemplate.update(sql, senderId, targetId, action.name());
    }

    //check if user has already swiped on the other user 
    public boolean hasViewed(long senderId, long targetId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM swipes WHERE sender_id = ? AND target_id = ?",
                Integer.class, senderId, targetId);
        return count != null && count > 0;
    }


    // check if user has already given an UP swipe to the other user 
    public boolean existsUpSwipe(long fromUserId, long toUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM swipes WHERE sender_id = ? AND target_id = ? AND action = 'UP'",
                Integer.class, fromUserId, toUserId);
        return count != null && count > 0;
    }
}