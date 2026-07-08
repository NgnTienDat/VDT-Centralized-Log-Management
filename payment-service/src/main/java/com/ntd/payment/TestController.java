//package com.dev.logsapp;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/test")
//@Slf4j
//public class TestController {
//    @GetMapping("/info")
//    public ResponseEntity<String> getLogInfo() {
//        log.info("User #88421 updated profile successfully, 3 fields changed");
//        return ResponseEntity.ok("INFO log generated");
//    }
//
//    @GetMapping("/error")
//    public ResponseEntity<String> getLogError() {
//        log.error("Payment processing failed for order #12345: insufficient funds", new RuntimeException("Payment gateway timeout"));
//        return ResponseEntity.ok("ERROR log generated");
//    }
//
//    @GetMapping("/debug")
//    public ResponseEntity<String> getLogDebug() {
//        log.debug("Debugging user login flow: step 1 completed, session ID: abc123xyz");
//        return ResponseEntity.ok("DEBUG log generated");
//    }
//
//    @GetMapping("/warn")
//    public ResponseEntity<String> getLogWarn() {
//        log.warn("Disk space running low on server 'db-server-01': only 5% remaining");
//        return ResponseEntity.ok("WARN log generated");
//    }
//
//    @GetMapping("/all")
//    public ResponseEntity<String> getLogAll() {
//        log.info("User #88421 created a new order #56789 with 2 items in cart");
//        log.error("Database connection pool exhausted — all 20 connections in use, request queued");
//        return ResponseEntity.ok("All log levels generated");
//    }
//}


package com.ntd.payment;

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
        log.info("Scheduled report generated successfully for sales department");
        return ResponseEntity.ok("INFO log generated");
    }

    @GetMapping("/error")
    public ResponseEntity<String> getLogError() {
        log.error(
                "Failed to synchronize inventory data with external warehouse service",
                new NullPointerException("External API unavailable")
        );
        return ResponseEntity.ok("ERROR log generated");
    }

    @GetMapping("/debug")
    public ResponseEntity<String> getLogDebug() {
        log.debug("Cache refresh process started for product catalog, batch size=500");
        return ResponseEntity.ok("DEBUG log generated");
    }

    @GetMapping("/warn")
    public ResponseEntity<String> getLogWarn() {
        log.warn("Memory usage exceeded warning threshold: current usage 82%");
        return ResponseEntity.ok("WARN log generated");
    }

    @GetMapping("/all")
    public ResponseEntity<String> getLogAll() {
        log.info("New customer account registered with email verification completed");
        log.error("Message queue consumer failed to process event after 3 retry attempts");
        return ResponseEntity.ok("All log levels generated");
    }
}