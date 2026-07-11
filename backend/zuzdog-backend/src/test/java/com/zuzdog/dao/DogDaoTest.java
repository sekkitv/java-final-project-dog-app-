package com.zuzdog.dao;

import com.zuzdog.model.Dog;
import com.zuzdog.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Optional;

// assert that is a library that allows us to write tests in a more readdable way and we can debug it easier
// instead of writing if statements and throwing exceptions we can use asserThat to check if a value is what we expect
// an exception will be thrown if the value is not what we expect and we can see the actual value and expected value
import static org.assertj.core.api.Assertions.assertThat;

public class DogDaoTest {

    public static void main(String[] args) {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5433/zuzdog",
                "postgres",
                "postgres");
        
        // because it is a standalone test file we need to create a jdbcTemplate object and pass it to the dao constructor
        // usually spring does this for us via annotations and dependency injection but in this case this is a test and this is why
        // we need to do that alone
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        UserDao userDao = new UserDao(jdbcTemplate);
        DogDao dogDao = new DogDao(jdbcTemplate);

        long userId = -1L;
        try {
            long ts = System.currentTimeMillis();
            String ownerUsername = "dao_test_owner_" + ts;
            String ownerEmail = "dao_test_owner_" + ts + "@test.local";

            User owner = new User();
            owner.setUsername(ownerUsername);
            owner.setPasswordHash("placeholder_hash");
            owner.setSalt("placeholder_salt");
            owner.setEmail(ownerEmail);
            owner.setMaxDistance(25.0);

            userId = userDao.insert(owner);
            
            // assert that is a library that allows us to write tests in a more readdable way and we can debug it easier
            // instead of writing if statements and throwing exceptions we can use asserThat to check if a value is what we expect
            // an exception will be thrown if the value is not what we expect and we can see the actual value and expected value
            assertThat(userId).as("owner insert should return a positive id").isPositive();

            Dog dog = new Dog();
            dog.setUserId(userId);
            dog.setDogName("DaoTestDog_" + ts);
            dog.setBreed("Mixed");
            dog.setDogAge(2);

            long dogId = dogDao.insert(dog);
            assertThat(dogId).as("dog insert should return a positive generated id").isPositive();

            Optional<Dog> primary = dogDao.findPrimaryByUserId(userId);
            assertThat(primary).as("findPrimaryByUserId should return the inserted dog").isPresent();
            assertThat(primary.get().getDogId()).isEqualTo(dogId);
            assertThat(primary.get().getUserId()).isEqualTo(userId);
            assertThat(primary.get().getDogName()).isEqualTo(dog.getDogName());
            assertThat(primary.get().getBreed()).isEqualTo("Mixed");
            assertThat(primary.get().getDogAge()).isEqualTo(2);

            System.out.println("DogDaoTest: PASS (userId=" + userId + ", dogId=" + dogId + ")");
        } catch (Throwable t) {
            System.out.println("DogDaoTest: FAIL: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        } finally {
            if (userId > 0) {
                jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
            }
        }
    }
}
