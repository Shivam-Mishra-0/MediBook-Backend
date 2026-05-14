package com.medibook.common.exception;

public class BaseUnauthorizedException extends RuntimeException {

    public BaseUnauthorizedException(String message) {
        super(message);
    }
}
