package za.co.unilinkhub.common.exception;

/**
 * The person isn't logged in, or their session has ended (HTTP 401). The frontend logs the user
 * out on any 401, so this must only be used for "who are you?" problems - never for "you're not
 * allowed to do that", which is {@link ForbiddenException}.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
