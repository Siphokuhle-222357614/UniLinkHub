package za.co.unilinkhub.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends email through SMTP when {@code spring.mail.host} is set, and otherwise writes the email
 * to the log so local development and tests still work end to end without a mail account.
 * A failed send never fails the request that triggered it (registration still succeeds) - the
 * person can always ask for the email again ("resend verification email").
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${spring.mail.host:}") String host,
                        @Value("${unilinkhub.mail.from:UniLinkHub <no-reply@unilinkhub.app>}") String from) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(host) && mailSender.getIfAvailable() != null;
    }

    /** @return true if the email was handed to the mail server (false when only logged, or on failure). */
    public boolean send(String to, String subject, String html, String plainText) {
        if (!isConfigured()) {
            log.info("""

                    ===== EMAIL (not sent - MAIL_HOST is not configured) =====
                    To:      {}
                    Subject: {}

                    {}
                    ==========================================================""", to, subject, plainText);
            return false;
        }
        try {
            JavaMailSender sender = mailSender.getObject();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            sender.send(message);
            log.info("Sent \"{}\" email to {}", subject, to);
            return true;
        } catch (MailException | jakarta.mail.MessagingException ex) {
            log.error("Could not send \"{}\" email to {}: {}", subject, to, ex.getMessage());
            return false;
        }
    }
}
