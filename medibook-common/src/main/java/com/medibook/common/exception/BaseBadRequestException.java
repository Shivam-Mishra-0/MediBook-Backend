package com.medibook.common.exception;

public class BaseBadRequestException extends RuntimeException {

    public BaseBadRequestException(String message) {
        super(message);
    }
}
