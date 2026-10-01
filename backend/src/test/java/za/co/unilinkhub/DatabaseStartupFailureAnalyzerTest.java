package za.co.unilinkhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import za.co.unilinkhub.common.config.DatabaseStartupFailureAnalyzer;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/** A database that can't be reached on startup is explained in plain English, not a 100-line stack trace. */
class DatabaseStartupFailureAnalyzerTest {

    private final DatabaseStartupFailureAnalyzer analyzer = new DatabaseStartupFailureAnalyzer();

    /** Wraps the cause the way Spring/Hikari/Flyway do, so the analyzer has to dig for it. */
    private static Throwable wrapped(Throwable cause) {
        return new IllegalStateException("Error creating bean with name 'flywayInitializer'",
                new RuntimeException("Unable to obtain connection from database", new SQLException("Communications link failure", cause)));
    }

    @Test
    void unknownHostSaysTheServerCantBeFoundAndToCheckAiven() {
        FailureAnalysis analysis = analyzer.analyze(wrapped(new UnknownHostException("mysql-32b3ef96-unilinkhub.f.aivencloud.com")));
        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("can't find the database server 'mysql-32b3ef96-unilinkhub.f.aivencloud.com'");
        assertThat(analysis.getAction()).contains("Running").contains("DB_URL");
    }

    @Test
    void refusedConnectionPointsAtThePortAndAllowList() {
        FailureAnalysis analysis = analyzer.analyze(wrapped(new ConnectException("Connection refused")));
        assertThat(analysis.getAction()).contains("PORT").contains("allow-list");
    }

    @Test
    void wrongPasswordAndSslAndDatabaseNameAreExplained() {
        assertThat(analyzer.analyze(wrapped(new SQLException("Access denied for user 'avnadmin'@'1.2.3.4'"))).getAction())
                .contains("DB_USERNAME").contains("DB_PASSWORD");
        assertThat(analyzer.analyze(wrapped(new SQLException("Connections using insecure transport are prohibited while --require_secure_transport=ON."))).getAction())
                .contains("sslMode=REQUIRED");
        assertThat(analyzer.analyze(wrapped(new SQLException("Unknown database 'unilinkhub'"))).getAction())
                .contains("defaultdb");
    }

    @Test
    void unrelatedFailuresAreLeftToSpring() {
        assertThat(analyzer.analyze(new IllegalStateException("Port 8081 was already in use"))).isNull();
    }
}
