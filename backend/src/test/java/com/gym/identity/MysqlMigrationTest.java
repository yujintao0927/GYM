package com.gym.identity;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MysqlMigrationTest {
    static String url() {
        String url = System.getenv("GYM_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:mysql://localhost:3306/gym_dev_test_night001(?:\\?.*)?"))
            throw new IllegalStateException("Explicit isolated test database required");
        return url;
    }
    @Test void initialMigrationExistsAndIsRepeatable() {
        var flyway = Flyway.configure().dataSource(url(), System.getenv("GYM_DB_USERNAME"),
            System.getenv("GYM_DB_PASSWORD")).cleanDisabled(true).load();
        flyway.migrate();
        assertThat(flyway.info().applied()).extracting(m -> m.getVersion().toString()).containsExactly("1", "2");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
