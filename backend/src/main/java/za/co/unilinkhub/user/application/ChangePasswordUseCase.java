package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void execute(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that account."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Your current password isn't right. We ask for it to make sure it's really you - please check it and try again.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("Your new password needs at least 8 characters so it's harder to guess.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
