package com.example.orderservice.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class per tutti gli integration test.
 * Il container PostgreSQL è statico: viene avviato una sola volta
 * per l'intera suite di test (non per ogni classe), riducendo
 * drasticamente i tempi di esecuzione.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    // Container statico — condiviso tra tutte le sottoclassi
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("orderdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);  // riusa il container tra i run se .testcontainers.properties lo abilita

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Disabilita il driver TC — usiamo @DynamicPropertySource
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");
    }

    @Autowired
    protected MockMvc mockMvc;

    // Helper per costruire l'header Authorization
    protected String bearerToken(String jwt) {
        return "Bearer " + jwt;
    }

    protected String customerToken() {
        return bearerToken(
                JwtTestUtils.customerToken(OrderServiceFixtures.CUSTOMER_ID));
    }

    protected String adminToken() {
        return bearerToken(JwtTestUtils.adminToken());
    }
}