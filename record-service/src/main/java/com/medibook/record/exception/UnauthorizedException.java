package com.medibook.record.exception;

import com.medibook.common.exception.BaseUnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends BaseUnauthorizedException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
