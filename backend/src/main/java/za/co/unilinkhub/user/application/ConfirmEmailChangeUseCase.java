package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ConfirmEmailChangeUseCase {

    private final UserRepository userRepository;

    public UserDTO execute(String token) {
        User user = userRepository.findByEmailChangeToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired email change link"));
        user.confirmEmailChange(token);
        return UserDTO.from(userRepository.save(user));
    }
}
