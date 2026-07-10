package com.bitculator;

/** A network-level failure prevented the request from completing. */
public class ConnectionException extends BitculatorException {

    public ConnectionException(String message) {
        super(message);
    }
}
