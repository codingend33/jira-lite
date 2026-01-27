package com.jiralite.backend.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.CreateInvitationRequest;
import com.jiralite.backend.dto.CreateInvitationResponse;
import com.jiralite.backend.dto.CreateOrganizationRequest;
import com.jiralite.backend.dto.CreateOrganizationResponse;
import com.jiralite.backend.service.InvitationService;
import com.jiralite.backend.service.OnboardingService;

import jakarta.validation.Valid;

/**
 * Controller for user onboarding and invitation management.
 * Allows authenticated users without org_id to create organizations.
 */
@RestController
@RequestMapping("/api")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final InvitationService invitationService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OnboardingController(
            OnboardingService onboardingService,
            InvitationService invitationService) {
        this.onboardingService = onboardingService;
        this.invitationService = invitationService;
    }

    /**
     * Create new organization (self-service onboarding).
     * Accessible to authenticated users even without org_id.
     */
    @PostMapping("/orgs")
    public ResponseEntity<CreateOrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");

        CreateOrganizationResponse response = onboardingService.createOrganization(userId, email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Create invitation for new member (ADMIN only).
     */
    @PostMapping("/orgs/{orgId}/invitations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateInvitationResponse> createInvitation(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateInvitationRequest request,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        String token = invitationService.createInvitation(orgId, request.getEmail(), request.getRole(), userId);

        String invitationUrl = frontendUrl + "/invite?token=" + token;

        return ResponseEntity.ok(new CreateInvitationResponse(
                token,
                invitationUrl,
                "Invitation created successfully"));
    }

    /**
     * Accept invitation (accessible to authenticated users without org_id).
     */
    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<String> acceptInvitation(
            @PathVariable String token,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");

        invitationService.acceptInvitation(token, userId, email);

        return ResponseEntity.ok("Invitation accepted successfully. Please refresh your session to continue.");
    }
}
