package com.zuzdog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// This is our entrypoint for the application.
// We need it to run the app, it is the main class that starts everything up.

// We also use the @ConfigurationPropertiesScan annotation to scan for configuration properties classes in the specified package.
@SpringBootApplication
@ConfigurationPropertiesScan
public class ZuzdogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuzdogApplication.class, args);
    }
}
