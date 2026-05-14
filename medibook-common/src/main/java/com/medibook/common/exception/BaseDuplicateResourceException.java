package com.medibook.common.exception;

public class BaseDuplicateResourceException extends RuntimeException {

    public BaseDuplicateResourceException(
            String resourceName,
            String fieldName,
            Object fieldValue) {

        super(String.format(
                "%s already exists with %s: %s",
                resourceName,
                fieldName,
                fieldValue
        ));
    }

    public BaseDuplicateResourceException(String message) {
        super(message);
    }
}
