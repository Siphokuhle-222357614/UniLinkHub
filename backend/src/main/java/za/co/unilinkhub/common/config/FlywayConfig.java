package za.co.unilinkhub.common.config;

import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * MySQL can't roll back table changes, so a migration that fails half-way stays recorded as
     * "failed" and Flyway then refuses to start until someone runs `flyway repair` by hand. The
     * migrations here are written to be safe to re-run after such a failure (see
     * V1_1__AdoptPreFlywaySchema), so clear the failed record and try again automatically.
     */
    @Bean
    public FlywayMigrationStrategy repairFailedMigrationsThenMigrate() {
        return flyway -> {
            MigrationInfo[] failed = Arrays.stream(flyway.info().all())
                    .filter(m -> m.getState().isFailed())
                    .toArray(MigrationInfo[]::new);
            if (failed.length > 0) {
                for (MigrationInfo m : failed) {
                    log.warn("Migration {} ({}) failed on an earlier start; clearing it so it can run again.", m.getVersion(), m.getDescription());
                }
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
