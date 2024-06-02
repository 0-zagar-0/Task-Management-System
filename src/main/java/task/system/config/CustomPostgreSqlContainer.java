package task.system.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class CustomPostgreSqlContainer extends PostgreSQLContainer<CustomPostgreSqlContainer> {
    private static final String DB_IMAGE = "postgresql:14";

    private static CustomPostgreSqlContainer postgreSqlContainer;

    private CustomPostgreSqlContainer() {
        super(DB_IMAGE);
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("TEST_DB_URL", postgreSqlContainer.getJdbcUrl());
        System.setProperty("TEST_DB_USERNAME", postgreSqlContainer.getUsername());
        System.setProperty("TEST_DB_PASSWORD", postgreSqlContainer.getPassword());
    }

    public static synchronized CustomPostgreSqlContainer getInstance() {
        if (postgreSqlContainer == null) {
            postgreSqlContainer = new CustomPostgreSqlContainer();
        }

        return postgreSqlContainer;
    }

    @Override
    public void stop() {

    }
}
