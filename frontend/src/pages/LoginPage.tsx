import { Box, Button, Card, CardContent, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Loading from "../components/Loading";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { login, handleCallback } = useAuth();
  const googleProvider = import.meta.env.VITE_COGNITO_IDP_GOOGLE;
  const microsoftProvider = import.meta.env.VITE_COGNITO_IDP_MICROSOFT;
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const code = params.get("code");
    if (!code) {
      return;
    }

    const lastCode = sessionStorage.getItem("jira-lite-auth-code");
    if (lastCode === code) {
      return;
    }

    sessionStorage.setItem("jira-lite-auth-code", code);
    setLoading(true);
    handleCallback(code)
      .then(() => {
        // Check for pending invitation token
        const pendingToken = sessionStorage.getItem("pending-invitation-token");
        if (pendingToken) {
          sessionStorage.removeItem("pending-invitation-token");
          navigate(`/invite?token=${pendingToken}`, { replace: true });
          return;
        }

        // Check if user has org_id in token
        // This will be handled by ProtectedRoute, which redirects to /create-org if missing
        navigate("/projects", { replace: true });
      })
      .catch(() => {
        setError("Login failed. Please try again.");
        navigate("/login", { replace: true });
      })
      .finally(() => setLoading(false));
  }, [handleCallback, location.search, navigate]);

  if (loading) {
    return <Loading />;
  }

  return (
    <Box sx={{ display: "flex", justifyContent: "center", mt: 12 }}>
      <Card sx={{ width: 420 }}>
        <CardContent sx={{ display: "grid", gap: 2 }}>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            Jira Lite
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Sign in with Cognito to access your workspace.
          </Typography>
          {error ? (
            <Typography variant="body2" color="error">
              {error}
            </Typography>
          ) : null}
          <Button variant="contained" onClick={() => login()}>
            Login with Cognito
          </Button>
          {googleProvider ? (
            <Button variant="outlined" onClick={() => login(googleProvider)}>
              Login with Google
            </Button>
          ) : null}
          {microsoftProvider ? (
            <Button variant="outlined" onClick={() => login(microsoftProvider)}>
              Login with Microsoft
            </Button>
          ) : null}
        </CardContent>
      </Card>
    </Box>
  );
}
