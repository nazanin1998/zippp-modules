package com.zippp.signature.exception;

/**
 * Raised when a JWT cannot be parsed — token is malformed, signature does not
 * verify, expiration / not-yet-valid claims fail, or the configured public key
 * is unusable.
 *
 * <p>Mapped to HTTP 401/403 by callers; the starter does not impose a
 * mapping because it doesn't depend on web.
 */
public class SignatureParseException extends RuntimeException {

    public SignatureParseException(String message) {
        super(message);
    }

    public SignatureParseException(String message, Throwable cause) {
        super(message, cause);
    }
}