package com.jiralite.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class PolishRequest {
    @NotBlank
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
