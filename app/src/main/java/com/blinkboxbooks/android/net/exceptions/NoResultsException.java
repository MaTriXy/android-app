package com.blinkboxbooks.android.net.exceptions;

/**
 * An exception that is thrown when the server has no results for a request.
 */
public class NoResultsException extends APIConnectorException {

    public NoResultsException() {
        super();
    }

    public NoResultsException(String message) {
        super(message);
    }
}
