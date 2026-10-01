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
                .orElseThrow(() -> new BadRequestException("This email change link isn't valid any more - it may have been used already. Request a new one from your account settings."));
        user.confirmEmailChange(token);
        return UserDTO.from(userRepository.save(user));
    }
}
