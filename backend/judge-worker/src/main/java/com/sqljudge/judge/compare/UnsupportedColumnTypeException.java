package com.sqljudge.judge.compare;

public class UnsupportedColumnTypeException extends RuntimeException {

    public UnsupportedColumnTypeException(Class<?> type) {
        super("Unsupported result value type: " + type.getName());
    }
}
