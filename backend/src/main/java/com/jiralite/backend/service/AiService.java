package com.jiralite.backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.exception.ApiException;

/**
 * Simple wrapper for Google Gemini polish endpoint.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final List<String> MODEL_RETRY_ORDER = List.of(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash");

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiService(
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:gemini-2.0-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String polishTicket(String rawText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI service is not configured",
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        String prompt = "Rewrite the following ticket into concise markdown with exactly these sections:\n"
                + "**Title:**\n"
                + "**Description:**\n"
                + "**Acceptance Criteria:**\n\n"
                + "Ticket input:\n" + rawText;
        try {
            return callGemini(model, prompt);
        } catch (ApiException ex) {
            throw ex;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return tryFallbackModels(prompt);
            }
            throw mapProviderException(ex);
        } catch (Exception ex) {
            log.error("Gemini API call failed: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI polish failed, please retry",
                    HttpStatus.BAD_GATEWAY.value());
        }
    }

    private String callGemini(String modelName, String prompt) {
        var body = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] { Map.of("text", prompt) })
                });
        ResponseEntity<Map> response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(modelName))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
        log.info("Gemini API response body: {}", response.getBody());
        String text = extractText(response.getBody());
        log.info("Extracted text from Gemini: {}", text == null ? "NULL"
                : (text.isBlank() ? "BLANK" : text.substring(0, Math.min(100, text.length()))));
        if (text == null || text.isBlank()) {
            throw new ApiException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI service returned empty content",
                    HttpStatus.BAD_GATEWAY.value());
        }
        return text;
    }

    private String tryFallbackModels(String prompt) {
        log.warn("Configured Gemini model {} unavailable, trying fallback models", model);
        for (String fallbackModel : MODEL_RETRY_ORDER) {
            if (fallbackModel.equals(model)) {
                continue;
            }
            try {
                return callGemini(fallbackModel, prompt);
            } catch (HttpStatusCodeException ex) {
                log.warn("Fallback model {} failed with {} {}", fallbackModel, ex.getStatusCode().value(),
                        ex.getStatusCode());
                if (ex.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                    throw mapProviderException(ex);
                }
            }
        }
        throw new ApiException(
                ErrorCode.INTERNAL_ERROR,
                "AI model is unavailable. Please update AI_GEMINI_MODEL.",
                HttpStatus.BAD_GATEWAY.value());
    }

    private ApiException mapProviderException(HttpStatusCodeException ex) {
        log.error("Gemini API call failed: {} {} - {}", ex.getStatusCode().value(), ex.getStatusCode(),
                ex.getResponseBodyAsString());
        return new ApiException(
                ErrorCode.INTERNAL_ERROR,
                "AI provider request failed: " + ex.getStatusCode().value(),
                HttpStatus.BAD_GATEWAY.value());
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map body) {
        if (body == null)
            return null;
        Object candidatesObj = body.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty())
            return null;
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> candidate))
            return null;
        Object content = candidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap))
            return null;
        Object partsObj = contentMap.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty())
            return null;
        return parts.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(pMap -> pMap.get("text"))
                .filter(text -> text != null && !text.toString().isBlank())
                .map(Object::toString)
                .collect(Collectors.joining("\n"))
                .trim();
    }
}
