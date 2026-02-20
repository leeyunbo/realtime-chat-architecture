package com.bok.chat.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> postgres;
    static final GenericContainer<?> redis;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgres.start();

        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
