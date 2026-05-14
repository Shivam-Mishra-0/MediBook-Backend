package com.medibook.provider.exception;

import com.medibook.common.exception.BaseForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends BaseForbiddenException {

    public ForbiddenException(String message) {
        super(message);
    }
}
