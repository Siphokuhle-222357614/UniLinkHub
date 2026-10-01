package za.co.unilinkhub.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ConflictException;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.TooManyRequestsException;
import za.co.unilinkhub.common.exception.UnauthorizedException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Translates every exception into the one {@link ApiError} shape, with a message written for the
 * person using the app: plain English, and always saying why the request was refused.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoRoute(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, RejectionMessages.NOT_FOUND, req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, ex.getCode());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooMany(TooManyRequestsException ex, HttpServletRequest req) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), req);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, RejectionMessages.WRONG_LOGIN, req);
    }

    // Raised at login for an account whose email isn't verified yet (AuthController writes the message).
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, RejectionMessages.CODE_EMAIL_NOT_VERIFIED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiError> handleLocked(LockedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, RejectionMessages.CODE_ACCOUNT_SUSPENDED);
    }

    // Every @PreAuthorize in this app is hasRole('ADMIN'), so a denial always means "admins only".
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, RejectionMessages.ADMIN_ONLY, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .distinct()
                .collect(Collectors.joining(" "));
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Some required information is missing (" + humanize(ex.getParameterName())
                + "). Please fill it in and try again.", req);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Please choose an image to upload.", req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, RejectionMessages.IMAGE_TOO_BIG, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, RejectionMessages.UNREADABLE_REQUEST, req);
    }

    // e.g. /api/listings/not-a-real-id - previously fell through to a 500.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, RejectionMessages.BAD_LINK, req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "That action isn't available here. Please refresh the page and try again.", req);
    }

    // Optimistic/pessimistic lock clashes - two people changing the same thing at once.
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> handleConcurrency(ConcurrencyFailureException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, RejectionMessages.CHANGED_AT_SAME_TIME, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        if (isEventStream(req)) {
            // A live-updates connection dropped (tab closed, network gone). Nobody is there to read an error.
            log.debug("Live-updates connection ended: {}", ex.getMessage());
            return null;
        }
        if (causedByOversizedPacket(ex)) {
            // The database refused a row bigger than its max_allowed_packet (XAMPP/MariaDB ships with 1MB).
            log.warn("Database rejected an oversized write on {} {} - raise the database's max_allowed_packet "
                    + "(e.g. 16M) to accept larger images. {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
            return build(HttpStatus.PAYLOAD_TOO_LARGE, RejectionMessages.IMAGE_TOO_BIG_TO_STORE, req);
        }
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, RejectionMessages.UNEXPECTED, req);
    }

    /** "studentNumber" + @NotBlank -> "Student number is required." */
    private static String describe(FieldError error) {
        String label = humanize(error.getField());
        String code = error.getCode() == null ? "" : error.getCode();
        return switch (code) {
            case "NotBlank", "NotNull", "NotEmpty" -> label + " is required.";
            case "Email" -> label + " must be a real email address, like name@mycput.ac.za.";
            case "Positive" -> label + " must be more than zero.";
            case "PositiveOrZero", "Min" -> label + " can't be less than zero.";
            default -> label + " " + error.getDefaultMessage() + ".";
        };
    }

    private static String humanize(String field) {
        String leaf = field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field;
        leaf = leaf.replaceAll("\\[\\d+]", "");
        String spaced = leaf.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return spaced.isEmpty() ? "This field" : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static boolean causedByOversizedPacket(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getClass().getSimpleName().equals("PacketTooBigException")
                    || (t.getMessage() != null && t.getMessage().contains("max_allowed_packet"))) {
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        return build(status, message, req, null);
    }

    /** The /api/stream connection speaks text/event-stream - a JSON error body can't be written to it. */
    private static boolean isEventStream(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return req.getRequestURI().endsWith("/api/stream") || (accept != null && accept.contains("text/event-stream"));
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req, String code) {
        if (isEventStream(req)) {
            return ResponseEntity.status(status).build();
        }
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI(), code);
        return ResponseEntity.status(status).body(body);
    }
}
