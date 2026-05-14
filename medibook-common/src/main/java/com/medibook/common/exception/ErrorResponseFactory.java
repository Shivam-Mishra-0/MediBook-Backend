package com.medibook.common.exception;

import java.time.LocalDateTime;

@FunctionalInterface
public interface ErrorResponseFactory<T> {

    T create(
            int status,
            String error,
            String message,
            String path,
            LocalDateTime timestamp);
}
