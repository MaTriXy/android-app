package com.blinkboxbooks.android.net.exceptions;

/**
 * An exception that is thrown when we detect an unexpected server error.
 */
public class InternalServerErrorException extends APIConnectorException {

    public InternalServerErrorException() {
        super();
    }

    public InternalServerErrorException(String message) {
        super(message);
    }
}