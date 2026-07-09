package com.zuzdog.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    private final JdbcTemplate jdbcTemplate;

    public HealthCheckController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // localhost:8080/health
    // this is a health check to check if jdbctemplate is working.

    @GetMapping("/health")
    public String healthCheck() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "JdbcTemplate bean is working! SELECT 1 returned: " + result;
    }
}
