package com.zippp.signature.exception;

/**
 * Raised when a JWT cannot be signed because the configured signing key is
 * missing, blank, or otherwise unusable (not a valid RSA private key, wrong
 * format, etc.).
 *
 * <p>This is an unchecked exception: key problems surface as
 * {@link IllegalStateException} so the application fails fast at startup
 * rather than producing bogus tokens at runtime.
 */
public class SignatureKeyException extends IllegalStateException {

    public SignatureKeyException(String message) {
        super(message);
    }

    public SignatureKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}