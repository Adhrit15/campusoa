package in.campusoa.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests (*IT). It starts ONE real PostgreSQL 16 container in Docker
 * for the whole test run (the "singleton container" pattern: a static field started once), and
 * {@code @ServiceConnection} tells Spring Boot to point the DataSource at it, replacing
 * DATABASE_URL etc. Flyway then runs every migration against it, so migrations are tested too.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
