package za.co.unilinkhub.user.repository;

import za.co.unilinkhub.user.domain.AccountStatus;
import za.co.unilinkhub.user.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port: what the application layer needs from persistence, independent of Spring Data / JPA.
 * {@link za.co.unilinkhub.user.infrastructure.JpaUserRepository} is the adapter that implements it.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentNumber(String studentNumber);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    Optional<User> findByEmailChangeToken(String emailChangeToken);

    List<User> findByAccountStatus(AccountStatus accountStatus);

    List<User> findAll();

    long countAll();

    boolean existsByEmail(String email);

    boolean existsByStudentNumber(String studentNumber);
}
