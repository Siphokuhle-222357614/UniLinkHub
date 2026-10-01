package za.co.unilinkhub.user.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.co.unilinkhub.common.exception.BadRequestException;

import java.util.Locale;

/**
 * UniLinkHub is for CPUT students only, and the student email address is how we know someone is a
 * CPUT student - only CPUT can hand out @mycput.ac.za addresses, and the verification email proves
 * the person can read that inbox. That's why no admin has to approve new students by hand.
 */
@Component
public class StudentEmailPolicy {

    private final String allowedDomain;

    public StudentEmailPolicy(@Value("${unilinkhub.registration.allowed-email-domain:mycput.ac.za}") String allowedDomain) {
        this.allowedDomain = allowedDomain.toLowerCase(Locale.ROOT).replaceFirst("^@", "");
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public void requireStudentEmail(String email) {
        String normalized = normalize(email);
        if (normalized == null || !normalized.endsWith("@" + allowedDomain)) {
            throw new BadRequestException("Please use your CPUT student email address (it ends in @" + allowedDomain
                    + "). UniLinkHub is only for CPUT students, and your student email is how we confirm that.");
        }
    }

    public String allowedDomain() {
        return allowedDomain;
    }
}
