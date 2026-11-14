package com.csscholar.backend.document.service;

import com.csscholar.backend.common.InternalRequestSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class IngestionClient {

    private static final Logger logger = LoggerFactory.getLogger(IngestionClient.class);

    private final String aiServiceUrl;
    private final String internalServiceKey;
    private final HttpClient httpClient;

    public IngestionClient(
            @Value("${ai-service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai-service.internal-key:cs_scholar_internal_service_secret_key_change_in_production}") String internalServiceKey
    ) {
        this.aiServiceUrl = aiServiceUrl.replaceAll("/+$", "");
        this.internalServiceKey = internalServiceKey;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public boolean triggerIngestion(String payloadJson) {
        String endpoint = aiServiceUrl + "/internal/v1/ingestion/process";
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson));

            InternalRequestSigner.signRequest(reqBuilder, "POST", URI.create(endpoint), payloadJson, internalServiceKey);
            HttpRequest request = reqBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 202 || response.statusCode() == 200) {
                logger.info("Ingestion successfully dispatched to AI service. Response: {}", response.body());
                return true;
            } else {
                logger.warn("AI service rejected ingestion dispatch. Status: {}, Body: {}",
                        response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            logger.warn("Error dispatching ingestion request to {}: {}", endpoint, e.getMessage());
            return false;
        }
    }

    public byte[] fetchPagePreview(String storageKey, int pageNumber) {
        try {
            String encodedKey = java.net.URLEncoder.encode(storageKey, java.nio.charset.StandardCharsets.UTF_8);
            String endpoint = aiServiceUrl + "/internal/v1/ingestion/page-preview?storage_key=" + encodedKey + "&page_number=" + pageNumber;
            URI uri = URI.create(endpoint);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .GET();

            InternalRequestSigner.signRequest(reqBuilder, "GET", uri, "", internalServiceKey);
            HttpRequest request = reqBuilder.build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                logger.warn("AI service page preview returned status: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch page preview from AI service: {}", e.getMessage());
            return null;
        }
    }
}

