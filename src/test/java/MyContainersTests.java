import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class MyContainersTests extends AbstractContainerDatabaseTest  {

    private final static Logger LOGGER = Logger.getLogger(MyContainersTests.class.getName());

    // will be started before and stopped after each test method
    @Container
    private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer<>()
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");

    @Test
    void test() {
        LOGGER.info(postgresqlContainer.getJdbcUrl());
        assertTrue(postgresqlContainer.isRunning());
    }

    @Test
    public void testSimple() throws SQLException, IOException, InterruptedException {
        MountableFile dbBinaryBackup = MountableFile.forClasspathResource("db/load/golden-db.dump");

        try (PostgreSQLContainer postgres = new PostgreSQLContainer<>("postgres:10.12-alpine")
                .withUsername("postgres")
                .withPassword("postgres")
                .withInitScript("db/load/northwind.sql")
                .withCopyFileToContainer(dbBinaryBackup, "/") ) {
            postgres.start();
            org.testcontainers.containers.Container.ExecResult result = postgres.execInContainer("ls");
            LOGGER.info("cmd: " + result.toString());
            ResultSet resultSet = performQuery(postgres, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            Assertions.assertEquals(1, resultSetInt);

            resultSet = performQuery(postgres, "SELECT version()");
            String version = resultSet.getString(1);


            LOGGER.info(postgres.getJdbcUrl());

            Thread.sleep(60000);
            LOGGER.info("Stopped");
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer<>().withCommand("postgres -c max_connections=42")) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            Assertions.assertEquals( "42", result);
        }
    }

    @Test
    public void testUnsetCommand() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer<>().withCommand("postgres -c max_connections=42").withCommand()) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            Assertions.assertNotEquals( "42", result);
        }
    }

    @Disabled
    @Test
    public void testExplicitInitScript() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer<>().withInitScript("somepath/init_postgresql.sql")) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            Assertions.assertEquals( "hello world", firstColumnValue);
        }
    }
}