package za.co.unilinkhub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import za.co.unilinkhub.user.application.AdminAccountService;

/**
 * Solves the "who invites the first admin?" problem: if ADMIN_EMAIL and ADMIN_PASSWORD are set and
 * no admin exists yet, one is created at startup. After that, admins invite each other from the
 * admin console. Does nothing once any admin exists, so it's safe to leave the variables set.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminAccountService adminAccountService;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;

    public AdminBootstrap(AdminAccountService adminAccountService,
                          @Value("${unilinkhub.admin.bootstrap-email:}") String email,
                          @Value("${unilinkhub.admin.bootstrap-password:}") String password,
                          @Value("${unilinkhub.admin.bootstrap-first-name:UniLinkHub}") String firstName,
                          @Value("${unilinkhub.admin.bootstrap-last-name:Admin}") String lastName) {
        this.adminAccountService = adminAccountService;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return;
        }
        if (password.length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters - the first admin account was not created.");
            return;
        }
        if (adminAccountService.createInitialAdminIfMissing(email, password, firstName, lastName)) {
            log.info("Created the first admin account for {} (from ADMIN_EMAIL).", email);
        }
    }
}
