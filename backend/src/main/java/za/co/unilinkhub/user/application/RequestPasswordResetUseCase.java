package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.mail.AccountEmails;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestPasswordResetUseCase {

    private static final Duration RESET_LINK_LIFETIME = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final AccountEmails accountEmails;

    public void execute(String email) {
        userRepository.findByEmail(StudentEmailPolicy.normalize(email)).ifPresent(this::issueResetToken);
        // Deliberately silent when the email isn't found, so this endpoint can't be used to
        // probe which addresses are registered.
    }

    private void issueResetToken(User user) {
        String token = UUID.randomUUID().toString();
        user.requestPasswordReset(token, LocalDateTime.now().plus(RESET_LINK_LIFETIME));
        userRepository.save(user);
        accountEmails.sendPasswordReset(user, token);
    }
}
