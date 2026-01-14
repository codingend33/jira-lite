package com.jiralite.backend.service;

import org.springframework.stereotype.Service;

import com.jiralite.backend.dto.EchoRequest;
import com.jiralite.backend.dto.EchoResponse;
import java.time.OffsetDateTime;

/**
 * Service layer for demo endpoint.
 * Contains business logic separate from HTTP concerns.
 */
@Service
public class DemoService {

    /**
     * Process echo request.
     * If title is "panic", throws RuntimeException to simulate unhandled error.
     */
    public EchoResponse echo(EchoRequest request) {
        if ("panic".equals(request.title())) {
            throw new RuntimeException("Simulated panic error");
        }

        return new EchoResponse(
            request.title(),
            OffsetDateTime.now().toString()
        );
    }
}

