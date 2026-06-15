package com.staging.logsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.concurrent.*;

@Slf4j
@Service
public class LogGeneratorService {

    private final Random random = new Random();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final RestTemplate restTemplate = new RestTemplate();

    private ScheduledFuture<?> task;

    private static final String BASE_URL = "http://localhost:8081/test";

    public synchronized void start() {

        if (task != null && !task.isCancelled()) {
            return;
        }

        task = scheduler.scheduleAtFixedRate(
                this::generateLog,
                0,
                5,
                TimeUnit.SECONDS);

        log.info("Log generator started");
    }

    public synchronized void stop() {

        if (task != null) {
            task.cancel(false);
            task = null;
        }

        log.info("Log generator stopped");
    }

    private void generateLog() {

        String endpoint = switch (random.nextInt(4)) {
            case 0 -> "/info";
            case 1 -> "/warn";
            // case 2 -> "/error";
            default -> "/all";
        };

        try {

            restTemplate.getForObject(
                    BASE_URL + endpoint,
                    String.class);

        } catch (Exception ex) {

            log.error(
                    "Failed to invoke endpoint {}",
                    endpoint,
                    ex);
        }
    }
}