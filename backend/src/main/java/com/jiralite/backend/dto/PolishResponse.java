package com.jiralite.backend.dto;

public class PolishResponse {
    private String result;

    public PolishResponse() {
    }

    public PolishResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
