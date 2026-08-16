package com.zuzdog.dao;

import com.zuzdog.model.Dog;
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
public class DogDao {

    private static final String SELECT_ALL_COLUMNS =
            "SELECT dog_id, user_id, dog_name, breed, dog_age, traits, " +
                    "description, photo_url, created_at, updated_at " +
                    "FROM dogs";
    // rowmapper is a spring interface - it job it to take one single row from the result and set it up as a java object.
    private static final RowMapper<Dog> DOG_ROW_MAPPER = (rs, rowNum) -> {
        Dog d = new Dog();
        d.setDogId(rs.getLong("dog_id"));
        d.setUserId(rs.getLong("user_id"));
        d.setDogName(rs.getString("dog_name"));
        d.setBreed(rs.getString("breed"));
        int dogAge = rs.getInt("dog_age");
        d.setDogAge(rs.wasNull() ? null : dogAge);
        d.setTraits(rs.getString("traits"));
        d.setDescription(rs.getString("description"));
        d.setPhotoUrl(rs.getString("photo_url"));
        d.setCreatedAt(JdbcMappingUtils.getInstant(rs, "created_at"));
        d.setUpdatedAt(JdbcMappingUtils.getInstant(rs, "updated_at"));
        return d;
    };

    
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertActor;

    public DogDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertActor = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("dogs")
                .usingColumns(
                        "user_id",
                        "dog_name",
                        "breed",
                        "dog_age",
                        "traits",
                        "description",
                        "photo_url")
                .usingGeneratedKeyColumns("dog_id");
    }

    // All of the methods below are responsible for querying the database and operations that we will do on the dog table.



    public Optional<Dog> findPrimaryByUserId(long userId) {
        List<Dog> rows = jdbcTemplate.query(
                SELECT_ALL_COLUMNS + " WHERE user_id = ? ORDER BY dog_id ASC LIMIT 1",
                DOG_ROW_MAPPER,
                userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long insert(Dog dog) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", dog.getUserId());
        params.put("dog_name", dog.getDogName());
        params.put("breed", dog.getBreed());
        params.put("dog_age", dog.getDogAge());
        params.put("traits", dog.getTraits());
        params.put("description", dog.getDescription());
        params.put("photo_url", dog.getPhotoUrl());
        Number key = insertActor.executeAndReturnKey(params);
        return key.longValue();
    }

    // update editable dog fields, scoped to userId so a user can only edit their own dog
    public int updateProfile(long dogId, long userId, String dogName, String breed,
                              Integer dogAge, String traits, String description) {
        return jdbcTemplate.update(
                "UPDATE dogs SET dog_name = ?, breed = ?, dog_age = ?, traits = ?, description = ?, updated_at = NOW() " +
                        "WHERE dog_id = ? AND user_id = ?",
                dogName, breed, dogAge, traits, description, dogId, userId);
    }

    // update the dog's photo url, scoped to userId so a user can only edit their own dog
    public int updatePhotoUrl(long dogId, long userId, String photoUrl) {
        return jdbcTemplate.update(
                "UPDATE dogs SET photo_url = ?, updated_at = NOW() WHERE dog_id = ? AND user_id = ?",
                photoUrl, dogId, userId);
    }
}
