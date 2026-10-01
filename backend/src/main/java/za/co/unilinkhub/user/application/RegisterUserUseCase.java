package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.ConflictException;
import za.co.unilinkhub.mail.AccountEmails;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    static final Duration VERIFICATION_LINK_LIFETIME = Duration.ofHours(48);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentEmailPolicy studentEmailPolicy;
    private final AccountEmails accountEmails;

    public UserDTO execute(String studentNumber, String firstName, String lastName,
                            String email, String rawPassword) {
        String normalizedEmail = StudentEmailPolicy.normalize(email);
        studentEmailPolicy.requireStudentEmail(normalizedEmail);
        String trimmedStudentNumber = studentNumber.trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("There's already an account with this email address. Try logging in, or reset your password if you've forgotten it.");
        }
        if (userRepository.existsByStudentNumber(trimmedStudentNumber)) {
            throw new ConflictException("There's already an account with this student number. If it's yours, log in or reset your password.");
        }

        String verificationToken = UUID.randomUUID().toString();
        User user = User.register(
                trimmedStudentNumber, firstName.trim(), lastName.trim(), normalizedEmail,
                passwordEncoder.encode(rawPassword),
                verificationToken, LocalDateTime.now().plus(VERIFICATION_LINK_LIFETIME)
        );
        User saved = userRepository.save(user);
        accountEmails.sendVerification(saved, verificationToken);
        return UserDTO.from(saved);
    }
}
