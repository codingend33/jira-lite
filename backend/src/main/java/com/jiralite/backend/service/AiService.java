package com.jiralite.backend.service;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Simple wrapper for Google Gemini polish endpoint.
 */
@Service
public class AiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiService(
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:gemini-pro}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String polishTicket(String rawText) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(rawText);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            var body = Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{Map.of("text",
                                    "Rewrite the following ticket into structured markdown with Title, Description, Acceptance Criteria:\n" + rawText)})
                    });
            ResponseEntity<Map> response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .body(new HttpEntity<>(body, headers))
                    .retrieve()
                    .toEntity(Map.class);
            String text = extractText(response.getBody());
            return text == null || text.isBlank() ? fallback(rawText) : text;
        } catch (Exception ex) {
            return fallback(rawText);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map body) {
        if (body == null) return null;
        Object candidatesObj = body.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) return null;
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> candidate)) return null;
        Object content = candidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) return null;
        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) return null;
        Object p0 = parts.get(0);
        if (p0 instanceof Map<?, ?> pMap) {
            Object text = pMap.get("text");
            return text != null ? text.toString() : null;
        }
        return null;
    }

    private String fallback(String rawText) {
        return """
                **Title:** %s
                **Description:**
                - %s

                **Acceptance Criteria:**
                - Clear reproduction steps
                - Expected behaviour described
                - Success is verifiable
                """.formatted(rawText.length() > 60 ? rawText.substring(0, 60) + "..." : rawText, rawText);
    }
}
