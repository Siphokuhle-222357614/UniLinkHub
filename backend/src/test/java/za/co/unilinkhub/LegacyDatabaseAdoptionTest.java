package za.co.unilinkhub;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recreates a real deploy failure: a database made by the old ddl-auto=update setting (so it lacks
 * newer tables, columns and enum values), adopted by Flyway at V1, where V2 then failed half-way.
 * The app must repair it and start, without losing the data already in it.
 *
 * Uses a second database next to MigrationTest's, on the same MIGRATION_TEST_DB_URL server.
 */
@EnabledIfEnvironmentVariable(named = "MIGRATION_TEST_DB_URL", matches = ".+")
class LegacyDatabaseAdoptionTest extends ApiTestSupport {

    private static final String DB = "unilinkhub_legacy_adoption_test";
    private static final String LEGACY_EMAIL = "legacy.student@mycput.ac.za";

    // What the old app version didn't have yet.
    private static final Set<String> MISSING_TABLES = Set.of("business_posts", "post_comments", "post_likes", "listing_photos", "order_items");
    private static final Map<String, Set<String>> MISSING_COLUMNS = Map.of(
            "users", Set.of("campus", "seller_rules_accepted_at"),
            "businesses", Set.of("campus", "pickup_location"),
            "orders", Set.of("pickup_code", "pickup_code_failures", "pickup_code_locked_until"),
            "listings", Set.of("taken_down_at", "takedown_reason"));

    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void legacyDatabase(DynamicPropertyRegistry registry) throws Exception {
        String base = System.getenv("MIGRATION_TEST_DB_URL");
        String user = System.getenv().getOrDefault("MIGRATION_TEST_DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("MIGRATION_TEST_DB_PASSWORD", "");
        String url = base.replaceFirst("/[^/?]+(\\?|$)", "/" + DB + "$1");

        try (Connection c = DriverManager.getConnection(base, user, password); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + DB);
            s.execute("CREATE DATABASE " + DB);
        }
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            for (String sql : legacySchema()) s.execute(sql);
            s.execute("INSERT INTO users (id, is_seller, created_at, first_name, last_name, email, password_hash, account_status, role, student_number, legacy_nickname) "
                    + "VALUES (UNHEX(REPLACE(UUID(),'-','')), 0, NOW(6), 'Legacy', 'Student', '" + LEGACY_EMAIL + "', 'x', 'ACTIVE', 'STUDENT', '219000001', 'lee')");
        }
        // The deploy that broke: baseline at V1, then V2 fails on a table the old schema doesn't have.
        try {
            Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration")
                    .baselineOnMigrate(true).baselineVersion("1").target("2").load().migrate();
            throw new IllegalStateException("Expected V2 to fail on the legacy schema, as it did on the real deploy");
        } catch (FlywayException expected) {
            // V2 is now recorded as failed, with some of its indexes created.
        }

        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    /** V1 as an older app version would have left it (made by Hibernate's ddl-auto=update). */
    private static java.util.List<String> legacySchema() throws Exception {
        String v1 = new ClassPathResource("db/migration/V1__baseline.sql").getContentAsString(StandardCharsets.UTF_8);
        String noComments = Arrays.stream(v1.split("\\R")).filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String raw : noComments.split(";")) {
            String sql = raw.trim();
            if (sql.isEmpty()) continue;
            String table = sql.replaceAll("(?is)^(?:CREATE TABLE|ALTER TABLE|CREATE INDEX \\w+ ON) (\\w+).*", "$1");
            if (MISSING_TABLES.contains(table)) continue;
            if (sql.startsWith("CREATE TABLE")) {
                StringBuilder kept = new StringBuilder();
                for (String line : sql.split("\n")) {
                    String col = line.trim().split("\\s+")[0];
                    if (MISSING_COLUMNS.getOrDefault(table, Set.of()).contains(col)) continue;
                    kept.append(line).append("\n");
                }
                sql = kept.toString()
                        .replace("student_number varchar(32),", "student_number varchar(32) not null,")
                        .replace("'OTHER','PROHIBITED_ITEM','SPAM'", "'OTHER','SPAM'");
                if (table.equals("users")) sql = sql.replace("id binary(16) not null,", "id binary(16) not null,\n    legacy_nickname varchar(40) not null,");
            }
            out.add(sql);
        }
        return out;
    }

    @Test
    void theFailedMigrationIsRepairedAndEverythingIsApplied() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(Arrays.stream(applied).noneMatch(m -> m.getState().isFailed())).isTrue();
        assertThat(Arrays.stream(applied).map(m -> m.getVersion().getVersion())).contains("1", "1.1", "2", "3");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void oldDataIsKeptAndTheOldSchemaWasCaughtUp() {
        assertThat(userRepository.findByEmail(LEGACY_EMAIL)).isPresent();
        assertThat(jdbc.queryForObject("SELECT column_type FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'reports' AND column_name = 'reason'", String.class)).contains("PROHIBITED_ITEM");
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'users' AND column_name = 'student_number'", String.class)).isEqualTo("YES");
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'users' AND column_name = 'legacy_nickname'", String.class)).isEqualTo("YES");
    }

    @Test
    void featuresThatNeedTheNewTablesWork() throws Exception {
        String[] seller = verifiedSeller();
        String listingId = product(seller[0], seller[1], "Adopted schema cupcakes", 4);

        String buyer = student();
        mockMvc.perform(post("/api/orders").header("Authorization", buyer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"listingId\":\"%s\",\"quantity\":1}],\"fulfilmentMethod\":\"PICKUP\"}".formatted(listingId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/businesses/" + seller[1] + "/posts").header("Authorization", seller[0])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Fresh batch out of the oven\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/businesses/" + seller[1] + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("Fresh batch out of the oven"));
    }
}
