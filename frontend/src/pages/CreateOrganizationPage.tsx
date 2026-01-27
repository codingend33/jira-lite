import { Alert, Box, Button, Card, CardContent, TextField, Typography } from "@mui/material";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { createOrganization } from "../api/onboarding";
import Loading from "../components/Loading";

export default function CreateOrganizationPage() {
    const [orgName, setOrgName] = useState("");


    const mutation = useMutation({
        mutationFn: createOrganization,
        onSuccess: () => {
            // Force token refresh by reloading the page
            window.location.href = "/projects";
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!orgName.trim()) return;
        mutation.mutate({ name: orgName.trim() });
    };

    if (mutation.isPending) {
        return <Loading />;
    }

    return (
        <Box
            sx={{
                minHeight: "100vh",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                backgroundColor: "#f5f5f5",
            }}
        >
            <Card sx={{ maxWidth: 500, width: "100%", mx: 2 }}>
                <CardContent sx={{ p: 4 }}>
                    <Typography variant="h4" gutterBottom align="center">
                        Welcome to Jira Lite!
                    </Typography>
                    <Typography variant="body1" color="text.secondary" paragraph align="center">
                        To get started, create your organization.
                    </Typography>

                    {mutation.isError && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                            {mutation.error instanceof Error
                                ? mutation.error.message
                                : "Failed to create organization. Please try again."}
                        </Alert>
                    )}

                    <Box component="form" onSubmit={handleSubmit} sx={{ mt: 3 }}>
                        <TextField
                            fullWidth
                            label="Organization Name"
                            placeholder="e.g., My Team, ACME Corp"
                            value={orgName}
                            onChange={(e) => setOrgName(e.target.value)}
                            required
                            autoFocus
                            sx={{ mb: 3 }}
                        />

                        <Button
                            type="submit"
                            variant="contained"
                            color="primary"
                            fullWidth
                            size="large"
                            disabled={!orgName.trim() || mutation.isPending}
                        >
                            Create Organization
                        </Button>

                        <Typography variant="body2" color="text.secondary" sx={{ mt: 2, textAlign: "center" }}>
                            You will be the administrator of this organization.
                        </Typography>
                    </Box>
                </CardContent>
            </Card>
        </Box>
    );
}
