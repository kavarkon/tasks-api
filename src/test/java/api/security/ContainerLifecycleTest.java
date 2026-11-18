package api.security;

import api.TestContainersBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContainerLifecycleTest extends TestContainersBaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void queryDatabase() {
        assertEquals(1, jdbcTemplate.queryForObject("SELECT 1", Integer.class));
    }
}
