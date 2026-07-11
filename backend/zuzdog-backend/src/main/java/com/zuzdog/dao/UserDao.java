package com.zuzdog.dao;

import com.zuzdog.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// this annotaion is responsible for making this class a spring bean(it creates an instance of it) , it tells us its responsible for database operations.
// with this when we are using spring boot we can inject this class just using @Autowired annotation instead of creating an instance of it manually.
@Repository
public class UserDao {

    private static final String SELECT_ALL_COLUMNS =
            "SELECT user_id, username, password_hash, salt, email, user_age, " +
                    "description, photo_url, max_distance, lat, lng, created_at, updated_at " +
                    "FROM users";

    // RowMapper is a spring interface - it job it to take one single row from the result and set it up as a java object.
    //in our case we are mapping it to a User object.                    
    // rs is result set, rowNum is the row number
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setEmail(rs.getString("email"));
        int userAge = rs.getInt("user_age");
        u.setUserAge(rs.wasNull() ? null : userAge);
        u.setDescription(rs.getString("description"));
        u.setPhotoUrl(rs.getString("photo_url"));
        u.setMaxDistance(rs.getDouble("max_distance"));
        double lat = rs.getDouble("lat");
        u.setLat(rs.wasNull() ? null : lat);
        double lng = rs.getDouble("lng");
        u.setLng(rs.wasNull() ? null : lng);
        u.setCreatedAt(JdbcMappingUtils.getInstant(rs, "created_at"));
        u.setUpdatedAt(JdbcMappingUtils.getInstant(rs, "updated_at"));
        return u;
    };

    // because of the annotation of @Repository it is connected to the jdbctempalte
    // and it is already connected to the database so we can use it to query the db.
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertActor;


    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertActor = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingColumns(
                        "username",
                        "password_hash",
                        "salt",
                        "email",
                        "user_age",
                        "description",
                        "photo_url",
                        "max_distance",
                        "lat",
                        "lng")
                .usingGeneratedKeyColumns("user_id");
    }

    // All of the methods below are responsible for querying the database and operations that we will do on the user table.

    public Optional<User> findById(long userId) {
        List<User> rows = jdbcTemplate.query(
                SELECT_ALL_COLUMNS + " WHERE user_id = ?",
                USER_ROW_MAPPER,
                userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<User> findByUsername(String username) {
        List<User> rows = jdbcTemplate.query(
                SELECT_ALL_COLUMNS + " WHERE username = ?",
                USER_ROW_MAPPER,
                username);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
    // here we are taking a user object and inserting it into the database via a hashmap.
    public long insert(User user) {
        Map<String, Object> params = new HashMap<>();
        params.put("username", user.getUsername());
        params.put("password_hash", user.getPasswordHash());
        params.put("salt", user.getSalt());
        params.put("email", user.getEmail());
        params.put("user_age", user.getUserAge());
        params.put("description", user.getDescription());
        params.put("photo_url", user.getPhotoUrl());
        params.put("max_distance", user.getMaxDistance());
        params.put("lat", user.getLat());
        params.put("lng", user.getLng());
        Number key = insertActor.executeAndReturnKey(params);
        return key.longValue();
    }
    // this method is responsible for updating the location of a user, it serach by user id.
    public int updateLocation(long userId, Double lat, Double lng) {
        return jdbcTemplate.update(
                "UPDATE users SET lat = ?, lng = ?, updated_at = NOW() WHERE user_id = ?",
                lat, lng, userId);
    }
}
