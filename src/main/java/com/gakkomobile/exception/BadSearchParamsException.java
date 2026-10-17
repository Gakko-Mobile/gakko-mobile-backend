package com.gakkomobile.exception;

public class BadSearchParamsException extends RuntimeException {
    public BadSearchParamsException(String message) {
        super(message);
    }
}
