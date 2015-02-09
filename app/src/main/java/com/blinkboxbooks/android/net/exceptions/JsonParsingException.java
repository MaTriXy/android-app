package com.blinkboxbooks.android.net.exceptions;

/**
 * An exception that is thrown when we JSON returned by the server cannot be parsed
 */
public class JsonParsingException extends APIConnectorException {

    public JsonParsingException() {
        super();
    }

    public JsonParsingException(String message) {
        super(message);
    }
}