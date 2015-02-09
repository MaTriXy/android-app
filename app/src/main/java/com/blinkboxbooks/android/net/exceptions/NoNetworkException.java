package com.blinkboxbooks.android.net.exceptions;

/**
 * An exception that is thrown when no network exists for a server call.
 */
public class NoNetworkException extends APIConnectorException {

    public NoNetworkException() {
        super();
    }

    public NoNetworkException(String message) {
        super(message);
    }
}
