package com.jiralite.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.EchoRequest;
import com.jiralite.backend.dto.EchoResponse;
import com.jiralite.backend.service.DemoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Demo endpoints for testing error handling and trace ID propagation.
 */
@RestController
@RequestMapping("/demo")
@Tag(name = "Demo", description = "Demo endpoints for testing")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Echo endpoint that tests validation and error handling.
     *
     * - title blank: returns 400 VALIDATION_ERROR
     * - title="panic": returns 500 INTERNAL_ERROR
     * - otherwise: returns 200 with echoed title
     */
    @PostMapping("/echo")
    @Operation(summary = "Echo title and return with timestamp")
    @ApiResponse(responseCode = "200", description = "Echo successful",
        content = @Content(schema = @Schema(implementation = EchoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error or bad request")
    @ApiResponse(responseCode = "500", description = "Internal error")
    public ResponseEntity<EchoResponse> echo(@Valid @RequestBody EchoRequest request) {
        EchoResponse response = demoService.echo(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

