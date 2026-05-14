package com.medibook.payment.exception;

import com.medibook.common.exception.BaseBadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends BaseBadRequestException {

    public BadRequestException(String message) {
        super(message);
    }
}
