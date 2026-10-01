package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ConflictException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.mail.AccountEmails;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestEmailChangeUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentEmailPolicy studentEmailPolicy;
    private final AccountEmails accountEmails;

    public void execute(UUID userId, String newEmail, String currentPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that account."));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Your current password isn't right. We ask for it to make sure it's really you - please check it and try again.");
        }
        String normalized = StudentEmailPolicy.normalize(newEmail);
        // Students must stay on their CPUT address; admin (staff) accounts aren't student accounts.
        if (!user.isAdmin()) {
            studentEmailPolicy.requireStudentEmail(normalized);
        }
        if (userRepository.existsByEmail(normalized)) {
            throw new ConflictException("There's already an account with this email address. Try logging in, or reset your password if you've forgotten it.");
        }

        String token = UUID.randomUUID().toString();
        user.requestEmailChange(normalized, token);
        userRepository.save(user);
        accountEmails.sendEmailChange(user, normalized, token);
    }
}
