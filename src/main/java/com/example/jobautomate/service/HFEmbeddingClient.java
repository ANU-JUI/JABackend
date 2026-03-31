package com.example.jobautomate.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class HFEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(HFEmbeddingClient.class);

    private final RestTemplate rest = new RestTemplate();

    @Value("${hf.api.key:}")
    private String apiKey;

   @Value("${hf.embedding.url:https://router.huggingface.co/hf-inference/models/BAAI/bge-small-en-v1.5}")
    private String url;

    public HFEmbeddingClient() {
        this.rest.setInterceptors(Collections.singletonList((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            if (contentType != null && contentType.contains(",")) {
                response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            }
            return response;
        }));
    }

    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        try {
            ResponseEntity<List> response = rest.postForEntity(url, buildEntity(text), List.class);
            return toVector(response.getBody());
        } catch (RestClientException exception) {
            log.info("HF API KEY PRESENT : {}",apiKey!=null && !apiKey.isBlank());
            log.warn("HuggingFace single embedding request failed: {}", exception.getMessage());
            return List.of();
        }
    }

    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            ResponseEntity<List> response = rest.postForEntity(url, buildEntity(texts), List.class);
            return toVectorList(response.getBody());
        } catch (RestClientException exception) {
            log.warn("HuggingFace batch embedding request failed: {}", exception.getMessage());
            return List.of();
        }
    }

    private HttpEntity<Map<String, Object>> buildEntity(Object inputs) {
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }

        return new HttpEntity<>(body, headers);
    }

    private List<Double> toVector(List<?> raw) {
        if (raw == null) {
            return List.of();
        }

        List<Double> vector = new ArrayList<>();
        for (Object value : raw) {
            if (value instanceof Number number) {
                vector.add(number.doubleValue());
            }
        }
        return vector;
    }

    private List<List<Double>> toVectorList(List<?> raw) {
        if (raw == null) {
            return List.of();
        }

        List<List<Double>> result = new ArrayList<>();
        for (Object row : raw) {
            if (!(row instanceof List<?> inner)) {
                continue;
            }

            List<Double> vector = new ArrayList<>();
            for (Object value : inner) {
                if (value instanceof Number number) {
                    vector.add(number.doubleValue());
                }
            }
            result.add(vector);
        }
        return result;
    }
}
