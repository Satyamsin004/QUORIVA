package com.csscholar.backend.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HealthIndicator for the internal Python FastAPI AI Service.
 *
 * <p>Enables Spring Boot Actuator to monitor the downstream AI service
 * and allows frontend clients to observe AI subsystem status through Spring Boot
 * rather than accessing internal AI service ports directly.</p>
 */
@Component
public class AiServiceHealthIndicator implements HealthIndicator {

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final HttpClient httpClient;

    public AiServiceHealthIndicator() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(2000))
                .build();
    }

    @Override
    public Health health() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/health"))
                    .timeout(Duration.ofMillis(2000))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return Health.up()
                        .withDetail("service", "FastAPI AI Engine")
                        .withDetail("endpoint", aiServiceUrl + "/health")
                        .withDetail("httpStatus", response.statusCode())
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "FastAPI AI Engine")
                        .withDetail("httpStatus", response.statusCode())
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "FastAPI AI Engine")
                    .withDetail("error", e.getMessage() != null ? e.getMessage() : "Connection refused")
                    .build();
        }
    }
}

