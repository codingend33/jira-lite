package com.jiralite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.jiralite.backend.dto.ErrorCode;
import com.jiralite.backend.exception.ApiException;

class AiServiceTest {

    @Test
    void polishTicketThrowsWhenNoApiKey() {
        AiService service = new AiService("", "gemini-pro");
        assertThatThrownBy(() -> service.polishTicket("Fix login bug"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
                    assertThat(apiEx.getStatusCode()).isEqualTo(500);
                });
    }
}
