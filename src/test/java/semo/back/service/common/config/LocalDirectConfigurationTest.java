package semo.back.service.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDirectConfigurationTest {

    @Test
    void defaultProfile_usesLocalDirectMode() throws IOException {
        PropertySource<?> properties = loadProperties("application.yml");

        assertThat(properties.getProperty("spring.profiles.active")).isEqualTo("local-direct");
        assertThat(properties.getProperty("spring.profiles.group.local-direct[0]")).isEqualTo("local");
    }

    @Test
    void localDirectProfile_disablesDiscoveryAndUsesDirectAuthUrl() throws IOException {
        PropertySource<?> properties = loadProperties("application-local-direct.yml");

        assertThat(properties.getProperty("spring.cloud.discovery.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("spring.cloud.service-registry.auto-registration.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("eureka.client.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("eureka.client.registerWithEureka")).isEqualTo(false);
        assertThat(properties.getProperty("eureka.client.fetchRegistry")).isEqualTo(false);
        assertThat(properties.getProperty("spring.cloud.openfeign.client.config.auth-back-server.url"))
                .isEqualTo("${SEMO_AUTH_BASE_URL:http://localhost:9000}");
    }

    @Test
    void localDirectProfile_allowsSemoFrontOriginForDirectBrowserCalls() throws IOException {
        PropertySource<?> properties = loadProperties("application-local-direct.yml");

        assertThat(properties.getProperty("semo.cors.allowed-origins"))
                .isEqualTo("${SEMO_CORS_ALLOWED_ORIGINS:http://localhost:3003,http://127.0.0.1:3003}");
    }

    private PropertySource<?> loadProperties(String resourcePath) throws IOException {
        return new YamlPropertySourceLoader()
                .load(resourcePath, new ClassPathResource(resourcePath))
                .get(0);
    }
}
