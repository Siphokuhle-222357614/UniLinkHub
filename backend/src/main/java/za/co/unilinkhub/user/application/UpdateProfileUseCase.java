package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.shared.domain.Campus;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final UserRepository userRepository;

    public UserDTO execute(UUID userId, String firstName, String lastName, String phoneNumber, String campus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that account."));
        user.updateProfile(firstName, lastName, phoneNumber);
        // "" clears the campus; null leaves it unchanged; anything else must be a real campus.
        user.updateCampus(Campus.parseOptional(campus), campus != null && campus.isBlank());
        return UserDTO.from(userRepository.save(user));
    }
}
