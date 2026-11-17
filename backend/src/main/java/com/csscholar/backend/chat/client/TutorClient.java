package com.csscholar.backend.chat.client;

import com.csscholar.backend.chat.dto.TutorChatInternalRequest;
import com.csscholar.backend.chat.dto.TutorChatInternalResponse;
import com.csscholar.backend.common.InternalRequestSigner;
import com.csscholar.backend.common.TutorException;
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
public class TutorClient {

    private static final Logger logger = LoggerFactory.getLogger(TutorClient.class);

    private final String aiServiceUrl;
    private final String internalServiceKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TutorClient(
            @Value("${ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl,
            @Value("${ai-service.internal-key:cs_scholar_internal_service_secret_key_change_in_production}") String internalServiceKey,
            ObjectMapper objectMapper
    ) {
        this.aiServiceUrl = aiServiceUrl.replaceAll("/+$", "");
        this.internalServiceKey = internalServiceKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public TutorChatInternalResponse generateAnswer(TutorChatInternalRequest requestPayload) {
        String endpoint = aiServiceUrl + "/internal/v1/tutor/answer";
        try {
            String bodyJson = objectMapper.writeValueAsString(requestPayload);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            InternalRequestSigner.signRequest(reqBuilder, "POST", URI.create(endpoint), bodyJson, internalServiceKey);
            HttpRequest request = reqBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), TutorChatInternalResponse.class);
            } else {
                logger.error("AI service tutor endpoint returned status {}: {}", response.statusCode(), response.body());
                throw new TutorException("Academic Tutor engine responded with error status: " + response.statusCode());
            }
        } catch (TutorException te) {
            throw te;
        } catch (Exception e) {
            logger.error("Failed to execute tutor answer request against AI service at {}: {}", endpoint, e.getMessage());
            throw new TutorException("Failed to communicate with Academic Tutor engine. Please try again later.", e);
        }
    }
}

