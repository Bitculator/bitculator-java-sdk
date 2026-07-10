package com.bitculator;

/** Base class for every exception thrown by the SDK (unchecked). */
public class BitculatorException extends RuntimeException {

    public BitculatorException(String message) {
        super(message);
    }

    public BitculatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
