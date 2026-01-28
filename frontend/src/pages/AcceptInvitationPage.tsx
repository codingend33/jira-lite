import { Alert, Box, Button, Card, CardContent } from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { acceptInvitation } from "../api/onboarding";
import Loading from "../components/Loading";
import { useAuth } from "../auth/AuthContext";
import { buildAuthorizeUrl } from "../auth/auth";

export default function AcceptInvitationPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const token = searchParams.get("token");

    const mutation = useMutation({
        mutationFn: (token: string) => acceptInvitation(token),
        onSuccess: async () => {
            // Force re-login to get fresh token with org_id and role claims
            const url = await buildAuthorizeUrl();
            window.location.assign(url);
        },
        onError: (error: any) => {
            setErrorMessage(error.response?.data?.message || "Failed to accept invitation");
        },
    });

    useEffect(() => {
        if (!token) {
            setErrorMessage("Invalid invitation link");
            return;
        }

        if (!isAuthenticated) {
            // Store token and redirect directly to Cognito
            console.log("Storing pending invitation token:", token);
            localStorage.setItem("pending-invitation-token", token);

            // Initiate login immediately
            buildAuthorizeUrl().then(url => {
                console.log("Redirecting to Cognito:", url);
                window.location.assign(url);
            });
            return;
        }

        // Auto-accept if authenticated and token exists
        mutation.mutate(token);
    }, [token, isAuthenticated]);

    if (!token) {
        return (
            <Box
                sx={{
                    minHeight: "100vh",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                }}
            >
                <Card sx={{ maxWidth: 400 }}>
                    <CardContent>
                        <Alert severity="error">Invalid invitation link</Alert>
                    </CardContent>
                </Card>
            </Box>
        );
    }

    if (mutation.isPending) {
        return <Loading />;
    }

    if (errorMessage) {
        return (
            <Box
                sx={{
                    minHeight: "100vh",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                }}
            >
                <Card sx={{ maxWidth: 400 }}>
                    <CardContent>
                        <Alert severity="error">{errorMessage}</Alert>
                        <Button
                            variant="outlined"
                            fullWidth
                            sx={{ mt: 2 }}
                            onClick={() => navigate("/projects")}
                        >
                            Go to Projects
                        </Button>
                    </CardContent>
                </Card>
            </Box>
        );
    }

    return <Loading />;
}
