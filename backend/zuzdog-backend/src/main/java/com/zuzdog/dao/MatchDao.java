package com.zuzdog.dao;

import com.zuzdog.model.Match;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MatchDao {

    //Converts each database row into a Match Java object
    private static final RowMapper<Match> MATCH_ROW_MAPPER = (rs, rowNum) -> {
        Match m = new Match();
        m.setUser1Id(rs.getLong("user1_id"));
        m.setUser2Id(rs.getLong("user2_id"));
        m.setMatchDate(JdbcMappingUtils.getInstant(rs, "match_date"));
        return m;
    };

    private final JdbcTemplate jdbcTemplate;

    public MatchDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //  If they  matched befrore the function does nothing else insert to match .
    public int insertMatch(long userA, long userB) {
        long[] pair = Match.pairInOrder(userA, userB) ;    // set the pair in order 
        String sql = """
                INSERT INTO matches (user1_id, user2_id, match_date)
                VALUES (?, ?, NOW())
                ON CONFLICT (user1_id, user2_id) DO NOTHING
                """;
        return jdbcTemplate.update(sql, pair[0], pair[1]);
    }

    //check if ther is a match between two users
    public boolean existsBetween(long userA, long userB) {
        long[] pair = Match.pairInOrder(userA, userB);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matches WHERE user1_id = ? AND user2_id = ?",
                Integer.class, pair[0], pair[1]);
        return count != null && count > 0;
    }

    // Get all matches for a user (ordered newer first) 
    public List<Match> findAllForUser(long userId) {
        String sql = """
                SELECT user1_id, user2_id, match_date FROM matches
                WHERE user1_id = ? OR user2_id = ?
                ORDER BY match_date DESC
                """;
        return jdbcTemplate.query(sql, MATCH_ROW_MAPPER, userId, userId);
    }
}
