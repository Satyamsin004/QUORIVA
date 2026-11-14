package com.csscholar.backend.search.client;

import com.csscholar.backend.common.InternalRequestSigner;
import com.csscholar.backend.common.SearchException;
import com.csscholar.backend.search.dto.SearchResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SearchClient {

    private static final Logger logger = LoggerFactory.getLogger(SearchClient.class);

    private final String aiServiceUrl;
    private final String internalServiceKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SearchClient(
            @Value("${ai-service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai-service.internal-key:cs_scholar_internal_service_secret_key_change_in_production}") String internalServiceKey,
            ObjectMapper objectMapper
    ) {
        this.aiServiceUrl = aiServiceUrl.replaceAll("/+$", "");
        this.internalServiceKey = internalServiceKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public SearchResponse executeSearch(String query, UUID workspaceId, UUID userId, Integer topK) {
        String endpoint = aiServiceUrl + "/internal/v1/retrieval/search";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("query", query);
            payload.put("workspaceId", workspaceId.toString());
            payload.put("userId", userId.toString());
            if (topK != null) {
                payload.put("topK", topK);
            }

            String bodyJson = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

            InternalRequestSigner.signRequest(reqBuilder, "POST", URI.create(endpoint), bodyJson, internalServiceKey);
            HttpRequest request = reqBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), SearchResponse.class);
            } else {
                logger.error("AI service search endpoint returned status {}: {}", response.statusCode(), response.body());
                throw new SearchException("Search engine responded with error status: " + response.statusCode());
            }
        } catch (SearchException se) {
            throw se;
        } catch (Exception e) {
            logger.error("Failed to execute search against AI service at {}: {}", endpoint, e.getMessage());
            throw new SearchException("Failed to communicate with AI retrieval engine. Please try again later.", e);
        }
    }
}

