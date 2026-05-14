package com.medibook.schedule.exception;

import com.medibook.common.exception.BaseResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends BaseResourceNotFoundException {

    public ResourceNotFoundException(
            String resourceName,
            String fieldName,
            Object fieldValue) {
        super(resourceName, fieldName, fieldValue);
    }
}
