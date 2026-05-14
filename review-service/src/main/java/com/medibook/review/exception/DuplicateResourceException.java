package com.medibook.review.exception;

import com.medibook.common.exception.BaseDuplicateResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends BaseDuplicateResourceException {

    public DuplicateResourceException(
            String resourceName,
            String fieldName,
            Object fieldValue) {
        super(resourceName, fieldName, fieldValue);
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}
