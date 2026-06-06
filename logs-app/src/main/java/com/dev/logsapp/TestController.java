package com.dev.logsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {
    @GetMapping("/info")
    public ResponseEntity<String> getLogInfo() {
        log.info("User #88421 updated profile successfully, 3 fields changed");
        return ResponseEntity.ok("INFO log generated");
    }

    @GetMapping("/error")
    public ResponseEntity<String> getLogError() {
        log.error("Payment processing failed for order #12345: insufficient funds", new RuntimeException("Payment gateway timeout"));
        return ResponseEntity.ok("ERROR log generated");
    }

    @GetMapping("/debug")
    public ResponseEntity<String> getLogDebug() {
        log.debug("Debugging user login flow: step 1 completed, session ID: abc123xyz");
        return ResponseEntity.ok("DEBUG log generated");
    }

    @GetMapping("/warn")
    public ResponseEntity<String> getLogWarn() {
        log.warn("Disk space running low on server 'db-server-01': only 5% remaining");
        return ResponseEntity.ok("WARN log generated");
    }

    @GetMapping("/all")
    public ResponseEntity<String> getLogAll() {
        log.info("User #88421 created a new order #56789 with 2 items in cart");
        log.error("Database connection pool exhausted — all 20 connections in use, request queued");
        return ResponseEntity.ok("All log levels generated");
    }
}
