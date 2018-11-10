package com.etzwallet.tools.exceptions;

import java.security.InvalidKeyException;

public class UserNotAuthenticatedException extends InvalidKeyException {

    /**
     * Constructs a new {@code UserNotAuthenticatedException} without detail message and cause.
     */
    public UserNotAuthenticatedException() {
        super("User not authenticated");
    }

    /**
     * Constructs a new {@code UserNotAuthenticatedException} with the provided detail message and
     * no cause.
     */
    public UserNotAuthenticatedException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code UserNotAuthenticatedException} with the provided detail message and
     * cause.
     */
    public UserNotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
