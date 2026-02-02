package com.jiralite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiServiceTest {

    @Test
    void polishTicketReturnsFallbackWhenNoApiKey() {
        AiService service = new AiService("", "gemini-pro");
        String result = service.polishTicket("Fix login bug");
        assertThat(result).contains("Title").contains("Fix login bug");
    }
}
