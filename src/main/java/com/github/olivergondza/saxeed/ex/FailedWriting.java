package com.github.olivergondza.saxeed.ex;

public class FailedWriting extends SaxeedException {
    public FailedWriting(String message) {
        super(message);
    }

    public FailedWriting(String message, Throwable cause) {
        super(message, cause);
    }
}
