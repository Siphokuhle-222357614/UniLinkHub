package za.co.unilinkhub.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * The limits on the public auth endpoints, in one place:
 * - wrong passwords: 5 per email per 15 minutes (stops guessing one student's password), and a
 *   per-network cap (stops one attacker trying many accounts);
 * - sign-ups, password-reset and verification emails: capped so they can't be used to flood
 *   someone's inbox or mass-create accounts.
 * Per-network limits are configurable because every request in the test suite comes from 127.0.0.1.
 */
@Component
public class AuthAbuseGuard {

    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final RateLimiter limiter;
    private final int failedLoginsPerEmail;
    private final int failedLoginsPerIp;
    private final int registrationsPerIp;
    private final int emailsPerAddress;
    private final int emailsPerIp;

    public AuthAbuseGuard(RateLimiter limiter,
                          @Value("${unilinkhub.rate-limits.failed-logins-per-email:5}") int failedLoginsPerEmail,
                          @Value("${unilinkhub.rate-limits.failed-logins-per-ip:30}") int failedLoginsPerIp,
                          @Value("${unilinkhub.rate-limits.registrations-per-ip-per-hour:10}") int registrationsPerIp,
                          @Value("${unilinkhub.rate-limits.emails-per-address-per-hour:3}") int emailsPerAddress,
                          @Value("${unilinkhub.rate-limits.emails-per-ip-per-hour:20}") int emailsPerIp) {
        this.limiter = limiter;
        this.failedLoginsPerEmail = failedLoginsPerEmail;
        this.failedLoginsPerIp = failedLoginsPerIp;
        this.registrationsPerIp = registrationsPerIp;
        this.emailsPerAddress = emailsPerAddress;
        this.emailsPerIp = emailsPerIp;
    }

    public void beforeLogin(String email, HttpServletRequest request) {
        limiter.requireCapacity("login-email:" + norm(email), failedLoginsPerEmail, FIFTEEN_MINUTES,
                "Too many wrong passwords for this account.");
        limiter.requireCapacity("login-ip:" + ip(request), failedLoginsPerIp, FIFTEEN_MINUTES,
                "Too many failed log-ins from your network.");
    }

    public void loginFailed(String email, HttpServletRequest request) {
        limiter.record("login-email:" + norm(email), FIFTEEN_MINUTES);
        limiter.record("login-ip:" + ip(request), FIFTEEN_MINUTES);
    }

    public void loginSucceeded(String email) {
        limiter.reset("login-email:" + norm(email));
    }

    public void beforeRegister(HttpServletRequest request) {
        limiter.consume("register-ip:" + ip(request), registrationsPerIp, ONE_HOUR,
                "Too many new accounts have been created from your network.");
    }

    /** Password resets and verification re-sends - both send an email to {@code email}. */
    public void beforeSendingEmail(String purpose, String email, HttpServletRequest request) {
        limiter.consume("email-ip:" + ip(request), emailsPerIp, ONE_HOUR, "Too many emails have been requested from your network.");
        limiter.consume(purpose + ":" + norm(email), emailsPerAddress, ONE_HOUR,
                "We've already sent several emails to this address in the last hour - check your inbox and spam folder.");
    }

    private static String norm(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Behind a proxy (Render) this is the visitor's address, via server.forward-headers-strategy. */
    private static String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
