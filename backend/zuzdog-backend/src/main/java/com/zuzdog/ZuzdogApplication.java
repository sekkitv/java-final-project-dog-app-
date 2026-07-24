package com.zuzdog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// This is our entrypoint for the application.
// We need it to run the app, it is the main class that starts everything up.

// We also use the @ConfigurationPropertiesScan annotation to scan for configuration properties classes in the specified package.
//
// @EnableScheduling turns on support for @Scheduled bean methods. We need it so
// SessionService.purgeExpired() can run in the background and sweep expired
// sessions. Without this annotation @Scheduled methods are silently ignored.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ZuzdogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuzdogApplication.class, args);
    }
}
