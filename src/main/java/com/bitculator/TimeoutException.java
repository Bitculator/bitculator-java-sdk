package com.bitculator;

/** The request exceeded the configured timeout (after retries). */
public class TimeoutException extends BitculatorException {

    public TimeoutException(String message) {
        super(message);
    }
}
