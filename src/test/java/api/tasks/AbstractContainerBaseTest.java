package api.tasks;

import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractContainerBaseTest {

    static final PostgreSQLContainer POSTGRE_SQL_CONTAINER;

    static {
        POSTGRE_SQL_CONTAINER = new PostgreSQLContainer("psql:latest");
        POSTGRE_SQL_CONTAINER.start();
    }
}

/*class FirstTest extends AbstractContainerBaseTest {

    @Test
    void someTestMethod() {
        String url = POSTGRE_SQL_CONTAINER.getJdbcUrl();

        // create a connection and run test as normal
    }
}*/

