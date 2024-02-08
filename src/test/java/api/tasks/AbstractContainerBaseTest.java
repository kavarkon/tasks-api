//package api.tasks;
//
//import org.springframework.boot.test.context.SpringBootTest;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//@SpringBootTest
//@Testcontainers
//public class AbstractContainerBaseTest {
//
//    @Container
//    static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER;
//
//    static {
//        POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:latest");
//        POSTGRE_SQL_CONTAINER.start();
//    }
//}
/*
public class AbstractContainerBaseTest {

    static final PostgreSQLContainer POSTGRE_SQL_CONTAINER;

    static {
        PostgreSQLContainer = new PostgreSQLContainer<>("postgres:latest");

        MY_SQL_CONTAINER.start();
    }
}*/



/*class FirstTest extends AbstractContainerBaseTest {

    @Test
    void someTestMethod() {
        String url = POSTGRE_SQL_CONTAINER.getJdbcUrl();

        // create a connection and run test as normal
    }
}*/