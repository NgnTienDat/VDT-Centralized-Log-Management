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

package com.staging.logsapp;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        log.error("Payment processing failed for order #12345: insufficient funds",
                new NullPointerException("Payment gateway timeout"));
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

    // @GetMapping("/test-load")
    // public ResponseEntity<String> testLoad(@RequestParam(defaultValue = "100")
    // int ratePerSecond,
    // @RequestParam(defaultValue = "60") int durationSeconds) {
    // long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
    // int delayMs = 5000 / ratePerSecond;

    // new Thread(() -> {
    // while (System.currentTimeMillis() < endTime) {
    // log.info("Scheduled report generated successfully for sales department");
    // log.error("Failed to synchronize inventory data with external warehouse
    // service",
    // new NullPointerException("External API unavailable"));
    // try {
    // Thread.sleep(delayMs);
    // } catch (InterruptedException e) {
    // Thread.currentThread().interrupt();
    // }
    // }
    // }).start();

    // return ResponseEntity.ok("Started loading test: " + ratePerSecond + "
    // logs/sec for " + durationSeconds + "s");
    // }

    @GetMapping("/test-load")
    public ResponseEntity<String> testLoad(
            @RequestParam(defaultValue = "5000") int ratePerSecond,
            @RequestParam(defaultValue = "60") int durationSeconds) {

        int threadCount = 8;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount + 1);

        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;

        int logsPerThread = ratePerSecond / threadCount;

        AtomicLong logCounter = new AtomicLong(0);

        // Monitor thread: in ra số log thực tế mỗi giây rồi reset counter
        executor.submit(() -> {
            long windowStart = System.currentTimeMillis();
            while (System.currentTimeMillis() < endTime) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
                long count = logCounter.getAndSet(0);
                long elapsed = System.currentTimeMillis() - windowStart;
                System.out.printf("[testLoad] Actual rate: %d logs/s (window: %d ms)%n", count, elapsed);
                windowStart = System.currentTimeMillis();
            }
        });

        for (int i = 0; i < threadCount; i++) {

            executor.submit(() -> {

                while (System.currentTimeMillis() < endTime) {

                    for (int j = 0; j < logsPerThread / 10; j++) {

                        log.info(
                                "Scheduled report generated successfully for sales department");
                        logCounter.incrementAndGet();

                        log.error(
                                "Failed to synchronize inventory data with external warehouse service",
                                new NullPointerException(
                                        "External API unavailable"));
                        logCounter.incrementAndGet();
                    }

                    LockSupport.parkNanos(
                            TimeUnit.MILLISECONDS.toNanos(100));
                }
            });
        }

        return ResponseEntity.ok(
                "Generating " + ratePerSecond +
                        " logs/sec for " +
                        durationSeconds + " seconds");
    }
}