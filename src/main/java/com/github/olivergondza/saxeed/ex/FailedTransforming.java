package com.github.olivergondza.saxeed.ex;

public class FailedTransforming extends SaxeedException {
    public FailedTransforming(String message) {
        super(message);
    }

    public FailedTransforming(String message, Throwable cause) {
        super(message, cause);
    }
}
