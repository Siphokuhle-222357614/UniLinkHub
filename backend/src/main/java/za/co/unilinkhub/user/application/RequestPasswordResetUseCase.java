package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestPasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestPasswordResetUseCase.class);

    private final UserRepository userRepository;

    public void execute(String email) {
        userRepository.findByEmail(email).ifPresent(this::issueResetToken);
        // Deliberately silent when the email isn't found, so this endpoint can't be used to
        // probe which addresses are registered.
    }

    private void issueResetToken(User user) {
        String token = UUID.randomUUID().toString();
        user.requestPasswordReset(token);
        userRepository.save(user);

        // TODO: wire a real email/SMS provider - see RegisterUserUseCase for the same gap.
        log.info("Password reset code for {}: {}", user.getEmail(), token);
    }
}
