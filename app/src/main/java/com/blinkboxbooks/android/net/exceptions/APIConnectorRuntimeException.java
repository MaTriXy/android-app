package com.blinkboxbooks.android.net.exceptions;

/**
 * This is a RuntimeException wrapper class for a standard APIConnectorException. This class can be
 * used as a container for an APIConnectorException for situations where we can't throw a standard
 * checked exception (for example via a content providers query method)
 */
public class APIConnectorRuntimeException extends RuntimeException {

    private APIConnectorException mException;

    /**
     * Construct a new APIConnectorRuntimeException
     * @param exception the APIConnectorException that this object is wrapping
     */
    public APIConnectorRuntimeException(APIConnectorException exception) {
        super();
        mException = exception;
    }

    /**
     * Get the standard APIConnectorException that this object is wrapping
     * @return an APIConnectorException object
     */
    public APIConnectorException getAPIConnectorException() {
        return mException;
    }
}
