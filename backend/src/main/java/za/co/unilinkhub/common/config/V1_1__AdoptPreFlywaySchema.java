package za.co.unilinkhub.common.config;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Brings a database that existed before Flyway up to exactly what V1__baseline.sql describes.
 *
 * Databases made by the old {@code ddl-auto=update} setting are adopted at V1 without running it
 * (spring.flyway.baseline-on-migrate), but they are only as new as the code that last started
 * against them - they can be missing whole tables (e.g. business posts), columns (e.g. campus) or
 * enum values, which made V2 fail. This runs once, straight after the baseline, and:
 *
 *  - creates every V1 table that's missing,
 *  - adds missing columns and updates columns whose type differs (e.g. a new enum value),
 *  - makes columns the app no longer uses optional, so they can't block inserts,
 *  - adds V1's missing unique constraints, foreign keys and indexes,
 *  - removes indexes left behind by a V2 that failed half-way, so V2 can run cleanly.
 *
 * It never drops tables, columns or rows. On a database built from V1 it finds nothing to do.
 * It's a Spring bean so Spring Boot hands it to Flyway; the class name sets its version (1.1).
 */
@Component
public class V1_1__AdoptPreFlywaySchema extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V1_1__AdoptPreFlywaySchema.class);

    private static final Pattern CREATE_TABLE = Pattern.compile("(?is)^CREATE TABLE (\\w+) \\((.*)\\) ENGINE = InnoDB$");
    private static final Pattern COLUMN = Pattern.compile("(?i)^(\\w+)\\s+(.+?)(\\s+not null)?(\\s+auto_increment)?$");
    private static final Pattern PRIMARY_KEY = Pattern.compile("(?i)^primary key \\((.+)\\)$");
    private static final Pattern ADD_CONSTRAINT = Pattern.compile("(?is)^ALTER TABLE (\\w+)\\s+ADD CONSTRAINT (\\w+) .*$");
    private static final Pattern CREATE_INDEX = Pattern.compile("(?is)^CREATE INDEX (\\w+) ON (\\w+) .*$");

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String schema = connection.getCatalog();
        int changes = 0;

        for (String sql : statements("db/migration/V1__baseline.sql")) {
            Matcher table = CREATE_TABLE.matcher(sql);
            Matcher constraint = ADD_CONSTRAINT.matcher(sql);
            Matcher index = CREATE_INDEX.matcher(sql);
            if (table.matches()) {
                changes += reconcileTable(connection, schema, table.group(1), table.group(2), sql);
            } else if (constraint.matches()) {
                if (!constraintExists(connection, schema, constraint.group(1), constraint.group(2))
                        && !equivalentExists(connection, schema, constraint.group(1), sql)) {
                    changes += tryExecute(connection, sql, "add constraint " + constraint.group(2));
                }
            } else if (index.matches()) {
                if (!indexExists(connection, schema, index.group(2), index.group(1))) {
                    changes += tryExecute(connection, sql, "add index " + index.group(1));
                }
            }
        }

        // A V2 that failed half-way (MySQL can't roll back DDL) left some of its indexes behind;
        // drop them so the re-run of V2 doesn't trip over "Duplicate key name".
        if (!migrationSucceeded(connection, "2")) {
            for (String sql : statements("db/migration/V2__lookup_indexes.sql")) {
                Matcher index = CREATE_INDEX.matcher(sql);
                if (index.matches() && indexExists(connection, schema, index.group(2), index.group(1))) {
                    changes += execute(connection, "DROP INDEX " + index.group(1) + " ON " + index.group(2),
                            "remove half-applied index " + index.group(1) + " (V2 will recreate it)");
                }
            }
        }

        if (changes == 0) {
            log.info("Database already matches the baseline schema - nothing to adopt.");
        } else {
            log.info("Brought the existing database up to the baseline schema with {} change(s); no data was removed.", changes);
        }
    }

    private int reconcileTable(Connection c, String schema, String table, String body, String createSql) throws SQLException {
        Map<String, ExistingColumn> existing = columns(c, schema, table);
        if (existing.isEmpty()) {
            return execute(c, createSql, "create missing table " + table);
        }

        int changes = 0;
        List<String> primaryKey = List.of();
        Map<String, String> wanted = new LinkedHashMap<>();
        for (String part : splitTopLevel(body)) {
            Matcher pk = PRIMARY_KEY.matcher(part);
            if (pk.matches()) {
                primaryKey = List.of(pk.group(1).replace(" ", "").split(","));
            } else {
                wanted.put(part.split("\\s+")[0].toLowerCase(Locale.ROOT), part);
            }
        }

        for (Map.Entry<String, String> entry : wanted.entrySet()) {
            String name = entry.getKey();
            String definition = entry.getValue();
            Matcher col = COLUMN.matcher(definition);
            if (!col.matches()) continue;
            String type = col.group(2);
            boolean notNull = col.group(3) != null;
            boolean autoIncrement = col.group(4) != null;
            ExistingColumn current = existing.get(name);

            if (current == null) {
                // A surrogate auto-increment key has to arrive as the primary key in the same statement.
                String extra = autoIncrement && primaryKey.equals(List.of(name)) && !hasPrimaryKey(existing) ? " PRIMARY KEY FIRST" : "";
                changes += execute(c, "ALTER TABLE " + table + " ADD COLUMN " + definition + extra, "add missing column " + table + "." + name);
            } else if (!normalize(current.type).equals(normalize(type)) || (!notNull && !current.nullable)) {
                // Keep NOT NULL off while existing rows might still hold NULLs; Hibernate only checks types.
                String modify = definition;
                if (notNull && current.nullable) modify = modify.replaceAll("(?i)\\s+not null", "");
                changes += execute(c, "ALTER TABLE " + table + " MODIFY COLUMN " + modify, "update column " + table + "." + name
                        + " (" + current.type + (current.nullable ? "" : " not null") + " -> " + type + (notNull ? " not null" : "") + ")");
            }
        }

        // Columns from older versions of the app: keep the data, but don't let them block inserts.
        for (ExistingColumn old : existing.values()) {
            if (!wanted.containsKey(old.name) && !old.nullable && old.defaultValue == null && !old.autoIncrement && !old.primaryKey) {
                changes += execute(c, "ALTER TABLE " + table + " MODIFY COLUMN " + old.name + " " + old.type + " NULL",
                        "make unused column " + table + "." + old.name + " optional");
            }
        }
        return changes;
    }

    // ------------------------------------------------------------------ database metadata

    private record ExistingColumn(String name, String type, boolean nullable, String defaultValue, boolean autoIncrement, boolean primaryKey) {
    }

    private static Map<String, ExistingColumn> columns(Connection c, String schema, String table) throws SQLException {
        Map<String, ExistingColumn> out = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT column_name, column_type, is_nullable, column_default, extra, column_key FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position")) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1).toLowerCase(Locale.ROOT);
                    String extra = rs.getString(5) == null ? "" : rs.getString(5).toLowerCase(Locale.ROOT);
                    String dflt = rs.getString(4);
                    // MariaDB reports "no default" as the text NULL.
                    if (dflt != null && dflt.equalsIgnoreCase("NULL")) dflt = null;
                    out.put(name, new ExistingColumn(name, rs.getString(2), "YES".equals(rs.getString(3)), dflt,
                            extra.contains("auto_increment"), "PRI".equals(rs.getString(6))));
                }
            }
        }
        return out;
    }

    private static boolean hasPrimaryKey(Map<String, ExistingColumn> columns) {
        return columns.values().stream().anyMatch(ExistingColumn::primaryKey);
    }

    private static boolean constraintExists(Connection c, String schema, String table, String name) throws SQLException {
        return exists(c, "SELECT 1 FROM information_schema.table_constraints WHERE table_schema = ? AND table_name = ? AND constraint_name = ?",
                schema, table, name);
    }

    private static final Pattern FOREIGN_KEY = Pattern.compile("(?i).*FOREIGN KEY \\((\\w+)\\) REFERENCES (\\w+) .*");
    private static final Pattern UNIQUE = Pattern.compile("(?i).*UNIQUE \\(([^)]+)\\).*");

    /** Older databases have the same keys under Hibernate's generated names (FK1a2b3c...); don't add duplicates. */
    private static boolean equivalentExists(Connection c, String schema, String table, String sql) throws SQLException {
        Matcher fk = FOREIGN_KEY.matcher(sql);
        if (fk.matches()) {
            return exists(c, "SELECT 1 FROM information_schema.key_column_usage WHERE table_schema = ? AND table_name = ? "
                    + "AND column_name = ? AND referenced_table_name = ?", schema, table, fk.group(1), fk.group(2));
        }
        Matcher unique = UNIQUE.matcher(sql);
        if (unique.matches()) {
            String wanted = unique.group(1).replace(" ", "").toLowerCase(Locale.ROOT);
            try (PreparedStatement ps = c.prepareStatement("SELECT index_name, GROUP_CONCAT(LOWER(column_name) ORDER BY seq_in_index) "
                    + "FROM information_schema.statistics WHERE table_schema = ? AND table_name = ? AND non_unique = 0 GROUP BY index_name")) {
                ps.setString(1, schema);
                ps.setString(2, table);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (wanted.equals(rs.getString(2))) return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean indexExists(Connection c, String schema, String table, String name) throws SQLException {
        return exists(c, "SELECT 1 FROM information_schema.statistics WHERE table_schema = ? AND table_name = ? AND index_name = ?",
                schema, table, name);
    }

    private static boolean exists(Connection c, String sql, String... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean migrationSucceeded(Connection c, String version) {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM flyway_schema_history WHERE version = ? AND success = 1")) {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException noHistoryYet) {
            return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Compares column types the way MySQL and MariaDB report them ("int(11)", "bit(1)", "enum('A','B')"). */
    static String normalize(String type) {
        String t = type.toLowerCase(Locale.ROOT).replace(" ", "");
        t = t.replaceAll("^integer$", "int").replaceAll("^(int|bigint|smallint|tinyint)\\(\\d+\\)$", "$1");
        if (t.equals("bit")) t = "bit(1)";
        return t;
    }

    private static int execute(Connection c, String sql, String what) throws SQLException {
        log.info("Adopting existing database: {}", what);
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
        return 1;
    }

    /** Constraints can fail on old data (e.g. duplicates); that shouldn't stop the app starting. */
    private static int tryExecute(Connection c, String sql, String what) {
        try {
            return execute(c, sql, what);
        } catch (SQLException ex) {
            log.warn("Couldn't {} on the existing data ({}). The app works without it; tidy the data and add it later.", what, ex.getMessage());
            return 0;
        }
    }

    private static List<String> statements(String resource) throws IOException {
        String text = new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
        StringBuilder withoutComments = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (!line.trim().startsWith("--")) withoutComments.append(line).append('\n');
        }
        List<String> out = new ArrayList<>();
        for (String s : withoutComments.toString().split(";")) {
            String trimmed = s.trim().replaceAll("\\s+", " ");
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    /** Splits "a int, b enum ('x','y'), primary key (a)" on the commas that aren't inside brackets. */
    private static List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char ch : body.toCharArray()) {
            if (ch == '(') depth++;
            if (ch == ')') depth--;
            if (ch == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.toString().isBlank()) parts.add(current.toString().trim());
        return parts;
    }
}
