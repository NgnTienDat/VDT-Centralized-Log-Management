package com.vdt.log_monitor.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public enum ErrorCode  {

    // 1xxx - Common errors
    UNCATEGORIZED_ERROR(1000, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(1001, "Invalid request", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(1002, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED(1003, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1004, "Forbidden", HttpStatus.FORBIDDEN),

    // 2xxx - Query Service errors
    LOG_NOT_FOUND(2001, "Log entry not found", HttpStatus.NOT_FOUND),
    INVALID_TIME_RANGE(2002, "Invalid time range", HttpStatus.BAD_REQUEST),
    INVALID_QUERY_SYNTAX(2003, "Invalid query syntax", HttpStatus.BAD_REQUEST),
    INDEX_NOT_FOUND(2004, "Elasticsearch index not found", HttpStatus.NOT_FOUND),

    // 3xxx - Alert Service errors
    ALERT_RULE_NOT_FOUND(3001, "Alert rule not found", HttpStatus.NOT_FOUND),
    WEBHOOK_DELIVERY_FAILED(3002, "Webhook delivery failed", HttpStatus.BAD_GATEWAY),
    NOTIFICATION_SEND_FAILED(3003, "Notification send failed", HttpStatus.SERVICE_UNAVAILABLE),
    ALERT_CONFIGURATION_INVALID(3004, "Alert configuration is invalid", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatusCode;
}
