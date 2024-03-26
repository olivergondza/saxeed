package com.github.olivergondza.saxeed.ex;

public abstract class SaxeedException extends RuntimeException {
    public SaxeedException(String message) {
        super(message);
    }

    public SaxeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
