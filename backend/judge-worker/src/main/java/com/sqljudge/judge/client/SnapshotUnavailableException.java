package com.sqljudge.judge.client;

public class SnapshotUnavailableException extends RuntimeException {

    public SnapshotUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
