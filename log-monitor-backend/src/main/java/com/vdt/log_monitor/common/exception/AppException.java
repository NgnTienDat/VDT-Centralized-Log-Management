package com.vdt.log_monitor.common.exception;

import lombok.Getter;

/**
 * Base application exception used across all services.
 * Wraps an {@link ErrorCode} to provide structured error information
 * for consistent API error responses via {@link com.sys.core.utils.ApiResponse}.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
