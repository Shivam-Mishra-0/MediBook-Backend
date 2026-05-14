package com.medibook.notification.exception;

import com.medibook.common.exception.BaseGlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler<ErrorResponse> {

    public GlobalExceptionHandler() {
        super(ErrorResponse::new);
    }
}
