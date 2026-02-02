package com.jiralite.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.PolishRequest;
import com.jiralite.backend.dto.PolishResponse;
import com.jiralite.backend.service.AiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI", description = "AI helpers")
@Validated
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/polish")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Polish ticket text via Gemini")
    public ResponseEntity<PolishResponse> polish(@Valid @RequestBody PolishRequest request) {
        String result = aiService.polishTicket(request.getText());
        return ResponseEntity.ok(new PolishResponse(result));
    }
}
