package com.gakkomobile;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gakkomobile.auth.dto.UserCreatedEvent;
import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public abstract class BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private JwtService jwtService;

    protected WebTestClient authenticatedClient;
    protected UserEntity currentUser;

    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gakko_test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void baseSetUp() {

        cleanDatabase();

        authenticatedClient = webTestClient.mutate()
                .defaultHeader("Authorization", "Bearer mock-token")
                .build();

        when(jwtService.extractUsername(anyString()))
                .thenReturn("student@pjatk.edu.pl");
        when(jwtService.isTokenValid(anyString(), any()))
                .thenReturn(true);

        currentUser = createAndSaveUser(uuid(0), "student@pjatk.edu.pl");
    }

    private void cleanDatabase() {

        List<String> tables = jdbcTemplate.queryForList("""
                SELECT quote_ident(tablename)
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')
                """, String.class);

        if (!tables.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables) + " CASCADE");
        }
    }

    protected <T> T persistAndCommit(T entity) {

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.persist(entity);
            entityManager.flush();
        });
        return entity;
    }

    protected void createDefaultNotificationSettings(UUID userId) {

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                eventPublisher.publishEvent(new UserCreatedEvent(userId)));
    }

    protected UserEntity createAndSaveUser(UUID userId, String email) {

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$dummyHash");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.STUDENT);
        user.setPasswordChangedAt(OffsetDateTime.now());
        return persistAndCommit(user);
    }
}