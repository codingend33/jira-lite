import { apiRequest } from "./client";

export interface CreateOrganizationRequest {
    name: string;
}

export interface CreateOrganizationResponse {
    orgId: string;
    orgName: string;
    message: string;
}

export interface CreateInvitationRequest {
    email: string;
    role: string;
}

export interface CreateInvitationResponse {
    token: string;
    invitationUrl: string;
    message: string;
}

/**
 * Create a new organization (self-service onboarding)
 */
export async function createOrganization(
    request: CreateOrganizationRequest
): Promise<CreateOrganizationResponse> {
    return apiRequest("/orgs", {
        method: "POST",
        body: JSON.stringify(request),
    });
}

/**
 * Create invitation for new member (ADMIN only)
 */
export async function createInvitation(
    orgId: string,
    request: CreateInvitationRequest
): Promise<CreateInvitationResponse> {
    return apiRequest(`/orgs/${orgId}/invitations`, {
        method: "POST",
        body: JSON.stringify(request),
    });
}

/**
 * Accept invitation token
 */
export async function acceptInvitation(token: string): Promise<{ message: string }> {
    return apiRequest(`/invitations/${token}/accept`, {
        method: "POST",
    });
}
