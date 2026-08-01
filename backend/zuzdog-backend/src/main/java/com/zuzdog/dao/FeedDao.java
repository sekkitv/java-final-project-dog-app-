package com.zuzdog.dao;

import com.zuzdog.dto.FeedCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FeedDao {

    private static final double EARTH_RADIUS_KM = 6371.0;

    // formula for calculating distance between two points 
    private static final String HAVERSINE_KM = """
            (%f * acos(LEAST(1.0, GREATEST(-1.0,
                cos(radians(?)) * cos(radians(u.lat)) * cos(radians(u.lng) - radians(?))
                + sin(radians(?)) * sin(radians(u.lat))
            ))))
            """.formatted(EARTH_RADIUS_KM);


    //sql query that look for ids that we wont and then join them with there dogs 
    private final String feedSql = """
            SELECT u.user_id, u.username, u.description, u.photo_url, distance_km,
                   d.dog_id, d.dog_name, d.breed, d.dog_age, d.photo_url AS dog_photo_url
            FROM (
                SELECT user_id, username, description, photo_url, %s AS distance_km
                FROM users u
                WHERE user_id <> ? AND lat IS NOT NULL AND lng IS NOT NULL  
                  AND NOT EXISTS (
                      SELECT 1 FROM swipes s
                      WHERE s.sender_id = ? AND s.target_id = u.user_id
                  )
            ) u
            LEFT JOIN LATERAL (
                SELECT dog_id, dog_name, breed, dog_age, photo_url
                FROM dogs d2 WHERE d2.user_id = u.user_id ORDER BY dog_id LIMIT 1
            ) d ON TRUE
            WHERE distance_km <= ?
            ORDER BY distance_km ASC
            LIMIT ?
            """.formatted(HAVERSINE_KM);



    //gets rows from db and build a feedcandidate object
    private static final RowMapper<FeedCandidate> FEED_ROW_MAPPER = (rs, rowNum) -> {
        FeedCandidate c = new FeedCandidate();
        c.setUserId(rs.getLong("user_id"));
        c.setUsername(rs.getString("username"));
        c.setDescription(rs.getString("description"));
        c.setPhotoUrl(rs.getString("photo_url"));
        c.setDistanceKm(rs.getDouble("distance_km"));

        long dogId = rs.getLong("dog_id");
        if (!rs.wasNull()) {
            c.setDogId(dogId);
            c.setDogName(rs.getString("dog_name"));
            c.setBreed(rs.getString("breed"));
            int dogAge = rs.getInt("dog_age");
            c.setDogAge(rs.wasNull() ? null : dogAge);
            c.setDogPhotoUrl(rs.getString("dog_photo_url"));
        }
        return c;
    };

    private final JdbcTemplate jdbcTemplate;

    public FeedDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    //  give a list of candidates for the distance setting 
    public List<FeedCandidate> findFeed(long viewerId, double viewerLat, double viewerLng,
                                         double maxDistanceKm, int limit) {
        return jdbcTemplate.query(feedSql, FEED_ROW_MAPPER,
                viewerLat, viewerLng, viewerLat,  
                viewerId, viewerId,                 
                maxDistanceKm, limit);
    }
}
