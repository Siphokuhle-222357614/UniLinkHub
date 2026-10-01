package za.co.unilinkhub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * The logged-in user, if any, for code that has to behave differently for guests, owners and
 * admins on the same public endpoint (e.g. a taken-down listing is hidden from everyone but its
 * owner and admins). Controllers that require a login should use {@link CurrentUser} instead.
 */
public final class CurrentUserProvider {

    private CurrentUserProvider() {
    }

    public static Optional<UserPrincipal> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal principal ? Optional.of(principal) : Optional.empty();
    }

    public static Optional<UUID> currentId() {
        return current().map(UserPrincipal::getId);
    }

    public static boolean isAdmin() {
        return current().map(UserPrincipal::isAdmin).orElse(false);
    }
}
