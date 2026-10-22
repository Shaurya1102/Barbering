package com.Barbering.backend.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Customizes Flyway migration execution. Runs repair before migrate so that
 * benign line-ending (CRLF vs LF) or comment differences between environments
 * do not block application startup with checksum mismatches on previously applied migrations.
 */
@Configuration
public class DatabaseConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
