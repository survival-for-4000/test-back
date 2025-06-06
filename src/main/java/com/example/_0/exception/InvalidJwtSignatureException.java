package com.example._0.exception;

public class InvalidJwtSignatureException extends RuntimeException {
    public InvalidJwtSignatureException() {
        super();
    }

    public InvalidJwtSignatureException(String message) {
        super(message);
    }
}
