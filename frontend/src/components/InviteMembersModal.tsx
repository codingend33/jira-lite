import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    FormControlLabel,
    FormLabel,
    IconButton,
    InputAdornment,
    Radio,
    RadioGroup,
    TextField,
    Typography,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { createInvitation, CreateInvitationResponse } from "../api/onboarding";
import { useAuth } from "../auth/AuthContext";

interface InviteMembersModalProps {
    open: boolean;
    onClose: () => void;
}

export default function InviteMembersModal({ open, onClose }: InviteMembersModalProps) {
    const [email, setEmail] = useState("");
    const [role, setRole] = useState("MEMBER");
    const { state } = useAuth();

    const orgId = (state.profile as any)?.["custom:org_id"];

    const mutation = useMutation<CreateInvitationResponse, Error, void>({
        mutationFn: () => createInvitation(orgId, { email, role }),
        onSuccess: () => {
            // Don't reset form or close modal - show success with invitation link
        },
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!email.trim()) return;
        mutation.mutate();
    };

    const handleClose = () => {
        mutation.reset();
        setEmail("");
        setRole("MEMBER");
        onClose();
    };

    const handleCopyLink = () => {
        if (mutation.data?.invitationUrl) {
            navigator.clipboard.writeText(mutation.data.invitationUrl);
        }
    };

    const handleSendAnother = () => {
        mutation.reset();
        setEmail("");
        setRole("MEMBER");
    };

    return (
        <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
            <DialogTitle>Invite Member to Organization</DialogTitle>
            <DialogContent>
                <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
                    {mutation.isError && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                            {mutation.error instanceof Error
                                ? mutation.error.message
                                : "Failed to send invitation"}
                        </Alert>
                    )}

                    {mutation.isSuccess && mutation.data && (
                        <Alert severity="success" sx={{ mb: 2 }}>
                            <Typography variant="body2" gutterBottom>
                                Invitation created successfully!
                            </Typography>
                            <Typography variant="caption" color="text.secondary" gutterBottom>
                                Share this link with the new member:
                            </Typography>
                            <TextField
                                fullWidth
                                value={mutation.data.invitationUrl}
                                InputProps={{
                                    readOnly: true,
                                    endAdornment: (
                                        <InputAdornment position="end">
                                            <IconButton onClick={handleCopyLink} edge="end" size="small">
                                                <ContentCopyIcon fontSize="small" />
                                            </IconButton>
                                        </InputAdornment>
                                    ),
                                }}
                                sx={{ mt: 1, mb: 1 }}
                                size="small"
                            />
                            <Button
                                size="small"
                                onClick={handleSendAnother}
                                sx={{ mt: 1 }}
                            >
                                Send Another Invitation
                            </Button>
                        </Alert>
                    )}

                    {!mutation.isSuccess && (
                        <>
                            <TextField
                                fullWidth
                                label="Email Address"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                sx={{ mb: 3 }}
                                disabled={mutation.isPending}
                            />

                            <FormControl component="fieldset" sx={{ mb: 2 }}>
                                <FormLabel component="legend">Role</FormLabel>
                                <RadioGroup value={role} onChange={(e) => setRole(e.target.value)}>
                                    <FormControlLabel
                                        value="MEMBER"
                                        control={<Radio />}
                                        label="Member - Can view and edit projects"
                                        disabled={mutation.isPending}
                                    />
                                    <FormControlLabel
                                        value="ADMIN"
                                        control={<Radio />}
                                        label="Admin - Full organization access"
                                        disabled={mutation.isPending}
                                    />
                                </RadioGroup>
                            </FormControl>
                        </>
                    )}
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} disabled={mutation.isPending}>
                    {mutation.isSuccess ? "Close" : "Cancel"}
                </Button>
                {!mutation.isSuccess && (
                    <Button
                        onClick={handleSubmit}
                        variant="contained"
                        disabled={!email.trim() || mutation.isPending}
                    >
                        {mutation.isPending ? "Sending..." : "Send Invitation"}
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
}
