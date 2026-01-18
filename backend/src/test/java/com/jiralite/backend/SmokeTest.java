package com.jiralite.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.jiralite.backend.security.TestJwtDecoderConfig;

/**
 * Minimal smoke test to ensure application context loads.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class SmokeTest {

    @Test
    void contextLoads() {
        // no-op
    }
}

