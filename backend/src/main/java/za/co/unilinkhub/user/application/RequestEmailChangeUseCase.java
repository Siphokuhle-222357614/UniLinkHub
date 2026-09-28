package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ConflictException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestEmailChangeUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestEmailChangeUseCase.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void execute(UUID userId, String newEmail, String currentPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("An account already exists for this email address");
        }

        String token = UUID.randomUUID().toString();
        user.requestEmailChange(newEmail, token);
        userRepository.save(user);

        // TODO: wire a real email/SMS provider - see RegisterUserUseCase for the same gap.
        log.info("Email change confirmation for {} -> {}: /api/auth/confirm-email-change?token={}",
                user.getEmail(), newEmail, token);
    }
}
