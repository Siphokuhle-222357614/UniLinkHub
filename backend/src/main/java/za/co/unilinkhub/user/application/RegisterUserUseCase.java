package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.ConflictException;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserUseCase.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDTO execute(String studentNumber, String firstName, String lastName,
                            String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account already exists for this email address");
        }
        if (userRepository.existsByStudentNumber(studentNumber)) {
            throw new ConflictException("An account already exists for this student number");
        }

        String verificationToken = UUID.randomUUID().toString();
        User user = User.register(
                studentNumber, firstName, lastName, email,
                passwordEncoder.encode(rawPassword),
                verificationToken
        );

        User saved = userRepository.save(user);

        // TODO: wire a real email/SMS provider. Logging keeps the flow demoable end-to-end for now -
        // grab the link from the console and hit GET /api/auth/verify?token=... to activate the account.
        log.info("Verification link for {}: /api/auth/verify?token={}", saved.getEmail(), verificationToken);

        return UserDTO.from(saved);
    }
}
