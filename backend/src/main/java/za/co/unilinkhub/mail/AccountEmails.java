package za.co.unilinkhub.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import za.co.unilinkhub.user.domain.User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The account emails UniLinkHub sends. Every link points at the frontend (not the raw API), so
 * the person lands on a friendly page that finishes the job and tells them what happened.
 */
@Component
public class AccountEmails {

    private final EmailService emailService;
    private final String baseUrl;

    public AccountEmails(EmailService emailService, @Value("${unilinkhub.app.base-url:http://localhost:5173}") String baseUrl) {
        this.emailService = emailService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public void sendVerification(User user, String token) {
        String link = link("/verify-email", token);
        send(user.getEmail(), "Verify your UniLinkHub email address", user.getFirstName(),
                "Welcome to UniLinkHub! Please confirm this is your CPUT student email address so you can start buying and selling on campus.",
                "Verify my email", link,
                "This link works for 48 hours. If you didn't sign up for UniLinkHub, you can ignore this email.");
    }

    public void sendPasswordReset(User user, String token) {
        String link = link("/forgot-password", token);
        send(user.getEmail(), "Reset your UniLinkHub password", user.getFirstName(),
                "Someone (hopefully you) asked to reset your UniLinkHub password. Use the button below to choose a new one.",
                "Choose a new password", link,
                "This link works for 1 hour. If you didn't ask for this, you can ignore this email - your password won't change.");
    }

    public void sendEmailChange(User user, String newEmail, String token) {
        String link = link("/confirm-email-change", token);
        send(newEmail, "Confirm your new UniLinkHub email address", user.getFirstName(),
                "Please confirm that you want to use this address for your UniLinkHub account from now on.",
                "Confirm new email", link,
                "Until you confirm, your account keeps using " + user.getEmail() + ".");
    }

    public void sendAdminInvite(User admin, String invitedBy, String token) {
        String link = link("/forgot-password", token) + "&invite=1";
        send(admin.getEmail(), "You've been invited to run UniLinkHub", admin.getFirstName(),
                invitedBy + " has created a UniLinkHub admin account for you. Admin accounts manage the marketplace - verifying "
                        + "businesses, reviewing reports and keeping it safe. Set a password to get started.",
                "Set my password", link,
                "This link works for 72 hours. If you weren't expecting this, you can ignore this email.");
    }

    private String link(String path, String token) {
        return baseUrl + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void send(String to, String subject, String firstName, String intro, String buttonLabel, String link, String footnote) {
        String plain = "Hi " + firstName + ",\n\n" + intro + "\n\n" + buttonLabel + ": " + link + "\n\n" + footnote
                + "\n\n- The UniLinkHub team";
        String html = """
                <div style="font-family:Inter,Segoe UI,Arial,sans-serif;background:#F4F6FA;padding:32px 16px">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;border:1px solid #E3E8EF">
                    <p style="margin:0 0 24px;font-size:20px;font-weight:700;color:#163D72">Uni<span style="color:#2A9BB4">Link</span>Hub</p>
                    <p style="margin:0 0 12px;font-size:15px;color:#1F2937">Hi %s,</p>
                    <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#1F2937">%s</p>
                    <a href="%s" style="display:inline-block;background:#163D72;color:#ffffff;text-decoration:none;font-weight:600;font-size:15px;padding:12px 22px;border-radius:10px">%s</a>
                    <p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#64748B">%s</p>
                    <p style="margin:16px 0 0;font-size:12px;line-height:1.6;color:#64748B;word-break:break-all">Button not working? Copy this link into your browser:<br>%s</p>
                  </div>
                </div>
                """.formatted(esc(firstName), esc(intro), esc(link), esc(buttonLabel), esc(footnote), esc(link));
        emailService.send(to, subject, html, plain);
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
