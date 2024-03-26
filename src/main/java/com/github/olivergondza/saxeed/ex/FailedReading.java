package com.github.olivergondza.saxeed.ex;

public class FailedReading extends SaxeedException {

    public FailedReading(String message) {
        super(message);
    }

    public FailedReading(String message, Throwable cause) {
        super(message, cause);
    }
}
