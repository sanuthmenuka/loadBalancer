package com.sanuth.loadBalancer.exception;

public class BackendProxyException extends RuntimeException {

    public BackendProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
