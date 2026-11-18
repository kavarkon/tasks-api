package api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersBaseTest.PostgreSQLTestConfiguration.class)
@Transactional
public abstract class TestContainersBaseTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgreSQLTestConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer createPostgreSQLContainer() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }
}
