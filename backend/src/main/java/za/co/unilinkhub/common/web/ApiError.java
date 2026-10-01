package za.co.unilinkhub.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * The one error shape every API rejection uses. {@code message} is written for the person using
 * the app (plain English, says why); {@code code} is optional and lets the frontend offer a
 * follow-up action, e.g. a "resend verification email" button for EMAIL_NOT_VERIFIED.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String code
) {
}
