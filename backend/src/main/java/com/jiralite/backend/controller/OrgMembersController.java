package com.jiralite.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jiralite.backend.dto.CreateMemberRequest;
import com.jiralite.backend.dto.MemberResponse;
import com.jiralite.backend.dto.UpdateMemberRequest;
import com.jiralite.backend.service.OrgMemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Admin-only org member management endpoints.
 */
@RestController
@RequestMapping("/org/members")
@Tag(name = "Org Members", description = "Admin member management within current org")
@Validated
public class OrgMembersController {

    private final OrgMemberService orgMemberService;

    public OrgMembersController(OrgMemberService orgMemberService) {
        this.orgMemberService = orgMemberService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List members in current org (ADMIN only)")
    public ResponseEntity<List<MemberResponse>> listMembers() {
        return ResponseEntity.ok(orgMemberService.listMembers());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add member to current org (ADMIN only)")
    public ResponseEntity<MemberResponse> createMember(@Valid @RequestBody CreateMemberRequest request) {
        MemberResponse response = orgMemberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update member role/status in current org (ADMIN only)")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(orgMemberService.updateMember(userId, request));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove member from current org (ADMIN only)")
    public ResponseEntity<Void> deleteMember(@PathVariable UUID userId) {
        orgMemberService.deleteMember(userId);
        return ResponseEntity.noContent().build();
    }
}
