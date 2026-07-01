package com.ntd.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogGeneratorService logGeneratorService;

    @PostMapping("/on")
    public ResponseEntity<String> startLogging() {

        logGeneratorService.start();

        return ResponseEntity.ok(
                "Log generator started"
        );
    }

    @PostMapping("/off")
    public ResponseEntity<String> stopLogging() {

        logGeneratorService.stop();

        return ResponseEntity.ok(
                "Log generator stopped"
        );
    }
}