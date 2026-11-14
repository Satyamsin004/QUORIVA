package com.csscholar.backend.learning.client;

import com.csscholar.backend.common.InternalRequestSigner;
import com.csscholar.backend.common.LearningException;
import com.csscholar.backend.learning.dto.internal.GenerateFlashcardsInternalRequest;
import com.csscholar.backend.learning.dto.internal.GenerateFlashcardsInternalResponse;
import com.csscholar.backend.learning.dto.internal.GenerateQuizInternalRequest;
import com.csscholar.backend.learning.dto.internal.GenerateQuizInternalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class LearningClient {

    private static final Logger logger = LoggerFactory.getLogger(LearningClient.class);

    private final String aiServiceUrl;
    private final String internalServiceKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LearningClient(
            @Value("${ai-service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai-service.internal-key:cs_scholar_internal_service_secret_key_change_in_production}") String internalServiceKey,
            ObjectMapper objectMapper
    ) {
        this.aiServiceUrl = aiServiceUrl.replaceAll("/+$", "");
        this.internalServiceKey = internalServiceKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public GenerateQuizInternalResponse generateQuiz(GenerateQuizInternalRequest requestPayload) {
        String endpoint = aiServiceUrl + "/internal/v1/learning/quizzes/generate";
        try {
            String bodyJson = objectMapper.writeValueAsString(requestPayload);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            InternalRequestSigner.signRequest(reqBuilder, "POST", URI.create(endpoint), bodyJson, internalServiceKey);
            HttpRequest request = reqBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), GenerateQuizInternalResponse.class);
            } else {
                logger.error("AI service quiz generator returned status {}: {}", response.statusCode(), response.body());
                throw new LearningException("AI Quiz generator responded with error status: " + response.statusCode());
            }
        } catch (LearningException le) {
            throw le;
        } catch (Exception e) {
            logger.error("Failed to execute quiz generation request against AI service at {}: {}", endpoint, e.getMessage());
            throw new LearningException("Failed to communicate with AI Quiz generator. Please try again later.", e);
        }
    }

    public GenerateFlashcardsInternalResponse generateFlashcards(GenerateFlashcardsInternalRequest requestPayload) {
        String endpoint = aiServiceUrl + "/internal/v1/learning/flashcards/generate";
        try {
            String bodyJson = objectMapper.writeValueAsString(requestPayload);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            InternalRequestSigner.signRequest(reqBuilder, "POST", URI.create(endpoint), bodyJson, internalServiceKey);
            HttpRequest request = reqBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), GenerateFlashcardsInternalResponse.class);
            } else {
                logger.error("AI service flashcard generator returned status {}: {}", response.statusCode(), response.body());
                throw new LearningException("AI Flashcard generator responded with error status: " + response.statusCode());
            }
        } catch (LearningException le) {
            throw le;
        } catch (Exception e) {
            logger.error("Failed to execute flashcard generation request against AI service at {}: {}", endpoint, e.getMessage());
            throw new LearningException("Failed to communicate with AI Flashcard generator. Please try again later.", e);
        }
    }
}

