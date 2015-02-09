package com.blinkboxbooks.android.net.exceptions;

/**
 * Abstract base class that all APIConnector exceptions should extend.
 */
public abstract class APIConnectorException extends Exception {

    public APIConnectorException() {
        super();
    }

    public APIConnectorException(String message) {
        super(message);
    }
}
