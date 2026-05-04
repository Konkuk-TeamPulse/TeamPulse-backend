package com.teampulse.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ReleaseProfileConfigurationTest {

    @Test
    void mysqlProfileUsesFlywayAndSchemaValidation() throws Exception {
        var properties = load("application-mysql.properties");

        assertThat(properties.getProperty("app.runtime.storage-mode")).isEqualTo("mysql");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration/mysql");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test
    void prodProfileKeepsRuntimeSurfacesClosedByDefault() throws Exception {
        var properties = load("application-prod.properties");

        assertThat(properties.getProperty("app.runtime.storage-mode")).isEqualTo("mysql");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("springdoc.api-docs.enabled")).contains(":false");
        assertThat(properties.getProperty("app.security.public-api-docs")).contains(":false");
        assertThat(properties.getProperty("app.security.public-roadmap")).contains(":false");
        assertThat(properties.getProperty("app.security.public-legacy-mobile-api")).contains(":false");
    }

    @Test
    void supabaseProfileIsNotTheLaunchDatabaseProfile() throws Exception {
        var properties = load("application-supabase.properties");

        assertThat(properties.getProperty("app.runtime.storage-mode")).isEqualTo("supabase");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
    }

    private Properties load(String resourceName) throws IOException {
        var raw = new Properties();
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(stream).as(resourceName + " exists").isNotNull();
            raw.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        var properties = new Properties();
        raw.forEach((key, value) -> properties.setProperty(stripBom(String.valueOf(key)), String.valueOf(value)));
        return properties;
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
