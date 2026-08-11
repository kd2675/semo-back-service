package semo.back.service.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "semoDateTimeProvider")
public class JpaConfig {
    @Bean
    public Clock semoClock() {
        return Clock.systemDefaultZone();
    }
}
