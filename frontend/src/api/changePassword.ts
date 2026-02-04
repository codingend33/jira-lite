import { ChangePasswordCommand, CognitoIdentityProviderClient } from "@aws-sdk/client-cognito-identity-provider";
import { getAccessToken } from "../auth/storage";

function inferRegionFromDomain(domain?: string): string | undefined {
  if (!domain) return undefined;
  const match = domain.match(/auth\.([^.]+)\.amazoncognito\.com/i);
  return match?.[1];
}

export async function changePassword(currentPassword: string, newPassword: string) {
  const accessToken = getAccessToken();
  if (!accessToken) {
    throw new Error("Not authenticated");
  }
  const domain = import.meta.env.VITE_COGNITO_DOMAIN as string | undefined;
  const region = (import.meta.env.VITE_COGNITO_REGION as string | undefined) || inferRegionFromDomain(domain);
  if (!region) {
    throw new Error("Missing Cognito region (set VITE_COGNITO_REGION or ensure domain contains region)");
  }

  const client = new CognitoIdentityProviderClient({ region });
  return client.send(
    new ChangePasswordCommand({
      AccessToken: accessToken,
      PreviousPassword: currentPassword,
      ProposedPassword: newPassword
    })
  );
}
