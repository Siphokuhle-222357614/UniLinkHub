package za.co.unilinkhub.common.exception;

/**
 * The person is logged in but isn't allowed to do this (HTTP 403). The message is shown to them
 * as-is, so it must say in plain English why they were stopped - e.g. "Only the owner of this
 * business can do this." - never just "Forbidden".
 */
public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String message) {
        this(message, null);
    }

    /** @param code a stable machine-readable reason the frontend can react to (e.g. "EMAIL_NOT_VERIFIED"). */
    public ForbiddenException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
