package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.LocalDateTime;

/** Redeems a reset link - also how an invited admin sets their first password (see AdminAccountService). */
@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void execute(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token == null ? "" : token.trim())
                .orElseThrow(() -> new BadRequestException("This reset link isn't valid any more - it may have been used already. Please request a new one."));
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("Your new password needs at least 8 characters so it's harder to guess.");
        }
        user.resetPassword(token.trim(), passwordEncoder.encode(newPassword), LocalDateTime.now());
        userRepository.save(user);
    }
}
