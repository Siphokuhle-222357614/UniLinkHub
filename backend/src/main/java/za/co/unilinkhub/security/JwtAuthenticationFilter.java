package za.co.unilinkhub.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            String email = jwtService.extractUsername(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (userDetails instanceof UserPrincipal principal && jwtService.isTokenValid(token, principal)) {
                    // The token proves who this is, but the account itself is re-checked on every
                    // request so a suspension or deactivation takes effect immediately rather than
                    // when the (24h) token happens to expire.
                    String blocked = blockedReason(principal);
                    if (blocked != null) {
                        request.setAttribute(SecurityErrorHandlers.REJECTION_ATTRIBUTE, blocked);
                    } else {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ignored) {
            // Invalid/expired token, or the account behind it no longer exists: the request proceeds
            // unauthenticated and endpoint security decides the outcome (SecurityErrorHandlers explains it).
        }

        filterChain.doFilter(request, response);
    }

    private static String blockedReason(UserPrincipal principal) {
        return switch (principal.getAccountStatus()) {
            case SUSPENDED -> "Your account has been suspended, so you've been logged out"
                    + (principal.getSuspensionReason() != null && !principal.getSuspensionReason().isBlank()
                    ? ". Reason: " + principal.getSuspensionReason() + "." : ".")
                    + " Contact an admin if you think this is a mistake.";
            case DEACTIVATED -> "This account has been deactivated, so you've been logged out. Log in again to reactivate it.";
            case PENDING_VERIFICATION -> "Please verify your email address before using UniLinkHub.";
            case ACTIVE -> null;
        };
    }
}
