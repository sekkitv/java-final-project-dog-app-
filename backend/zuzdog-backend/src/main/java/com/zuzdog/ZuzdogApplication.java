package com.zuzdog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// This is our entrypoint for the application.
// We need it to run the app, it is the main class that starts everything up.

// We also use the @ConfigurationPropertiesScan annotation to scan for configuration properties classes in the specified package.
//
// @EnableScheduling turns on @Scheduled methods. we need it so
// SessionService.purgeExpired() runs in the background. without it the
// @Scheduled methods just never run.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ZuzdogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuzdogApplication.class, args);
    }
}
