package za.co.unilinkhub.common.exception;

/** A fair-use limit was hit (HTTP 429). The message says what the limit is and when it resets. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
