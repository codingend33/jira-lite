package com.jiralite.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new organization during onboarding.
 */
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    // Constructors
    public CreateOrganizationRequest() {
    }

    public CreateOrganizationRequest(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
