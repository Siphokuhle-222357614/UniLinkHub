package za.co.unilinkhub.common.config;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * When the app can't reach its database on startup, Spring prints ~100 lines of stack trace with the
 * real reason buried at the bottom. This turns the common cases into a short, plain-English
 * "what went wrong / what to do" block at the top of the log (Render, IntelliJ, anywhere).
 *
 * Registered in META-INF/spring.factories.
 */
public class DatabaseStartupFailureAnalyzer implements FailureAnalyzer {

    private static final String DB_URL_FORMAT =
            "DB_URL must look like: jdbc:mysql://HOST:PORT/DATABASE?sslMode=REQUIRED&serverTimezone=UTC "
                    + "(for Aiven: HOST and PORT from the service's Overview page, DATABASE is usually defaultdb).";

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            String message = t.getMessage() == null ? "" : t.getMessage();

            if (t instanceof UnknownHostException) {
                return new FailureAnalysis(
                        "UniLinkHub couldn't start because it can't find the database server '" + message + "'. "
                                + "That address doesn't exist on the internet right now.",
                        "1. If the database is on Aiven, open the Aiven console and check the service says Running - "
                                + "a powered-off or deleted service loses its address. Power it on and wait until it's Running.\n"
                                + "2. Check that the host in the DB_URL environment variable exactly matches the Host shown by your "
                                + "database provider (no typos, no extra spaces).\n"
                                + DB_URL_FORMAT,
                        failure);
            }
            if (t instanceof ConnectException || t instanceof SocketTimeoutException
                    || message.contains("Connection refused") || message.contains("connect timed out")) {
                return new FailureAnalysis(
                        "UniLinkHub couldn't start because the database server didn't answer (" + message + ").",
                        "1. Check the PORT in DB_URL - managed databases such as Aiven use their own port, not 3306.\n"
                                + "2. If the provider has an IP allow-list, allow your host (on Aiven: service > Overview > "
                                + "Allowed IP addresses; 0.0.0.0/0 allows Render).\n"
                                + "3. Running locally? Make sure MySQL/MariaDB (e.g. XAMPP) is started.\n"
                                + DB_URL_FORMAT,
                        failure);
            }
            if (message.contains("Access denied")) {
                return new FailureAnalysis(
                        "UniLinkHub couldn't start because the database rejected the username or password.",
                        "Check DB_USERNAME and DB_PASSWORD (on Aiven the user is usually avnadmin; copy the password "
                                + "from the service's Overview page).",
                        failure);
            }
            if (message.contains("insecure transport") || message.contains("require_secure_transport")) {
                return new FailureAnalysis(
                        "UniLinkHub couldn't start because the database only accepts encrypted (SSL) connections.",
                        "Add sslMode=REQUIRED to DB_URL and remove useSSL=false. " + DB_URL_FORMAT,
                        failure);
            }
            if (message.contains("Unknown database")) {
                return new FailureAnalysis(
                        "UniLinkHub couldn't start because the database named in DB_URL doesn't exist (" + message + ").",
                        "Use a database that exists on the server (on Aiven the default one is defaultdb), or create it first. "
                                + DB_URL_FORMAT,
                        failure);
            }
        }
        return null;
    }
}
