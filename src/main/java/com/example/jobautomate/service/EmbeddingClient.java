package com.example.jobautomate.service;

import com.example.jobautomate.config.EmbeddingProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final EmbeddingProperties properties;
    private final RestClient restClient;

    public EmbeddingClient(EmbeddingProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeoutMs());
        requestFactory.setReadTimeout(properties.getTimeoutMs());
        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    public boolean isConfigured() {
        return properties.isEnabled() && StringUtils.hasText(properties.getBaseUrl());
    }

    public List<Double> embed(String text) {
        if (!isConfigured() || !StringUtils.hasText(text)) {
            return List.of();
        }

        try {
            EmbedResponse response = restClient.post()
                .uri(properties.getEmbedPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedRequest(text))
                .retrieve()
                .body(EmbedResponse.class);
            if (response == null || response.embedding() == null) {
                return List.of();
            }
            return response.embedding();
        } catch (RestClientException exception) {
            log.warn("Embedding service request failed: {}", exception.getMessage());
            return List.of();
        }
    }

    public Map<String, List<Double>> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Map.of();
        }

        Map<String, List<Double>> embeddings = new LinkedHashMap<>();
        List<String> uniqueTexts = texts.stream()
            .filter(StringUtils::hasText)
            .distinct()
            .toList();

        int batchSize = Math.max(1, properties.getBatchSize());
        for (int start = 0; start < uniqueTexts.size(); start += batchSize) {
            List<String> batch = uniqueTexts.subList(start, Math.min(uniqueTexts.size(), start + batchSize));
            for (String text : new ArrayList<>(batch)) {
                embeddings.put(text, embed(text));
            }
        }
        return embeddings;
    }

    private record EmbedRequest(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbedResponse(List<Double> embedding) {
    }
}
