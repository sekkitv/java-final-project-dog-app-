package com.zuzdog.dao;

import com.zuzdog.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDaoTest {

    public static void main(String[] args) {
                
        // because it is a standalone test file we need to create a jdbcTemplate object and pass it to the dao constructor
        // usually spring does this for us via annotations and dependency injection but in this case this is a test and this is why
        // we need to do that alone
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5433/zuzdog",
                "postgres",
                "postgres");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        UserDao userDao = new UserDao(jdbcTemplate);

        long userId = -1L;
        try {
            long ts = System.currentTimeMillis();
            String username = "dao_test_user_" + ts;
            String email = "dao_test_user_" + ts + "@test.local";

            User user = new User();
            user.setUsername(username);
            user.setPasswordHash("placeholder_hash");
            user.setSalt("placeholder_salt");
            user.setEmail(email);
            user.setMaxDistance(25.0);

            userId = userDao.insert(user);
            assertThat(userId).as("insert should return a positive generated id").isPositive();

            Optional<User> byId = userDao.findById(userId);
            assertThat(byId).as("findById should return the inserted user").isPresent();
            assertThat(byId.get().getUsername()).isEqualTo(username);
            assertThat(byId.get().getEmail()).isEqualTo(email);
            assertThat(byId.get().getMaxDistance()).isEqualTo(25.0);

            Optional<User> byUsername = userDao.findByUsername(username);
            assertThat(byUsername).as("findByUsername should return the inserted user").isPresent();
            assertThat(byUsername.get().getUserId()).isEqualTo(userId);
            assertThat(byUsername.get().getEmail()).isEqualTo(email);

            int updated = userDao.updateLocation(userId, 32.05, 34.78);
            assertThat(updated).as("updateLocation should affect exactly 1 row").isEqualTo(1);

            Optional<User> relocated = userDao.findById(userId);
            assertThat(relocated).isPresent();
            assertThat(relocated.get().getLat()).isEqualTo(32.05);
            assertThat(relocated.get().getLng()).isEqualTo(34.78);

            System.out.println("UserDaoTest: PASS (userId=" + userId + ")");
        } catch (Throwable t) {
            System.out.println("UserDaoTest: FAIL: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        } finally {
            if (userId > 0) {
                jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
            }
        }
    }
}
