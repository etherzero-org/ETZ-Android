package com.etzwallet.tools.exceptions;

import java.security.InvalidKeyException;

public class KeyPermanentlyInvalidatedException extends InvalidKeyException {
    /**
     * Constructs a new {@code KeyPermanentlyInvalidatedException} without detail message and cause.
     */
    public KeyPermanentlyInvalidatedException() {
        super("Key permanently invalidated");
    }

    /**
     * Constructs a new {@code KeyPermanentlyInvalidatedException} with the provided detail message
     * and no cause.
     */
    public KeyPermanentlyInvalidatedException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code KeyPermanentlyInvalidatedException} with the provided detail message
     * and cause.
     */
    public KeyPermanentlyInvalidatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
