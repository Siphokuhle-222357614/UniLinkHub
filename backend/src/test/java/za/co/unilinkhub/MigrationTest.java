package za.co.unilinkhub;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real Flyway migrations against a real MySQL/MariaDB database, then lets Hibernate
 * validate the entities against the result - so a migration that doesn't match the code fails
 * here instead of on deploy. Needs an EMPTY database:
 *
 *   MIGRATION_TEST_DB_URL=jdbc:mysql://localhost:3306/unilinkhub_migration_test
 *   MIGRATION_TEST_DB_USERNAME / MIGRATION_TEST_DB_PASSWORD
 *
 * CI sets these against a MySQL service container. Skipped when they're not set.
 */
@EnabledIfEnvironmentVariable(named = "MIGRATION_TEST_DB_URL", matches = ".+")
@TestPropertySource(properties = {
        "spring.datasource.url=${MIGRATION_TEST_DB_URL}",
        "spring.datasource.username=${MIGRATION_TEST_DB_USERNAME:root}",
        "spring.datasource.password=${MIGRATION_TEST_DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
})
class MigrationTest extends ApiTestSupport {

    @Autowired private Flyway flyway;

    @Test
    void migrationsApplyCleanlyAndMatchTheEntities() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();
        assertThat(Arrays.stream(applied).allMatch(m -> m.getState().isApplied() && !m.getState().isFailed())).isTrue();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void theAppWorksOnTheMigratedSchema() throws Exception {
        String[] seller = seller("BELLVILLE");
        String listingId = product(seller[0], seller[1], "Migration check notebook", 3);

        mockMvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campus").value("BELLVILLE"));
    }
}
