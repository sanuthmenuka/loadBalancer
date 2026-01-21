package com.sanuth.loadBalancer.exception;

public class NoHealthyBackendException extends RuntimeException {

    public NoHealthyBackendException(String message) {
        super(message);
    }
}
