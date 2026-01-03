package nl.mallepetrus.jiptv.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"prod", "production"})
public class HealthConfig {

    @Bean
    public HealthIndicator jiptvHealthIndicator() {
        return () -> Health.up()
                .withDetail("service", "JIPTV")
                .withDetail("status", "operational")
                .build();
    }
}