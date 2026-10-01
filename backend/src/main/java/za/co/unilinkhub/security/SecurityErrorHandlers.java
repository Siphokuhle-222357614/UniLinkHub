package za.co.unilinkhub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import za.co.unilinkhub.common.web.ApiError;
import za.co.unilinkhub.common.web.RejectionMessages;

import java.io.IOException;
import java.time.Instant;

/**
 * Rejections that happen in the security filters, before any controller runs. Without these,
 * Spring answers with an empty 401/403 body, which tells the person nothing - these write the
 * same plain-English {@link ApiError} every other rejection uses.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** Set by JwtAuthenticationFilter when a valid token belongs to a suspended/deactivated account. */
    public static final String REJECTION_ATTRIBUTE = "unilinkhub.auth.rejection";

    private final ObjectMapper objectMapper;

    // 401: not logged in, session expired, or the account was blocked after logging in.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        Object blocked = request.getAttribute(REJECTION_ATTRIBUTE);
        String message;
        if (blocked instanceof String reason) {
            message = reason;
        } else if (request.getHeader("Authorization") != null) {
            message = RejectionMessages.SESSION_ENDED;
        } else {
            message = RejectionMessages.NOT_LOGGED_IN;
        }
        write(response, HttpStatus.UNAUTHORIZED, message, request);
    }

    // 403: logged in, but this URL is admin-only (the /api/admin/** rule in SecurityConfig).
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        write(response, HttpStatus.FORBIDDEN, RejectionMessages.ADMIN_ONLY, request);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), null));
    }
}
