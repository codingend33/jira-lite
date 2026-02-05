import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    MenuItem,
    Select,
    Stack,
    Typography
} from "@mui/material";
import { useState } from "react";
import { Delete, Restore, Warning } from "@mui/icons-material";
import ErrorBanner from "../components/ErrorBanner";
import Loading from "../components/Loading";
import { useTrash, useRestoreProject, useRestoreTicket } from "../query/trashQueries";
import type { TrashItem } from "../api/trash";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";

export default function TrashPage() {
    const [filter, setFilter] = useState<"all" | "project" | "ticket">("all");
    const [confirmRestore, setConfirmRestore] = useState<TrashItem | null>(null);
    const { state } = useAuth();

    // Check if user has ADMIN role
    const groups = state.profile?.["cognito:groups"] ?? [];
    const isAdmin = groups.some((g) => g.toUpperCase() === "ADMIN");

    const trashQuery = useTrash(filter);
    const restoreProject = useRestoreProject();
    const restoreTicket = useRestoreTicket();

    const handleRestore = async () => {
        if (!confirmRestore) return;
        try {
            if (confirmRestore.type === "PROJECT") {
                await restoreProject.mutateAsync(confirmRestore.id);
            } else {
                await restoreTicket.mutateAsync(confirmRestore.id);
            }
            setConfirmRestore(null);
        } catch {
            // ErrorBanner will show the error
        }
    };

    const isPending = restoreProject.isPending || restoreTicket.isPending;

    if (trashQuery.isLoading) {
        return <Loading />;
    }

    const isForbidden = trashQuery.error instanceof ApiError && trashQuery.error.status === 403;
    const mutationError = isForbidden ? null : trashQuery.error || restoreProject.error || restoreTicket.error;
    const items = isForbidden ? [] : trashQuery.data ?? [];

    return (
        <Stack spacing={3}>
            <ErrorBanner error={mutationError} />

            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                    <Delete sx={{ fontSize: 32, color: "text.secondary" }} />
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                        Trash
                    </Typography>
                </Box>
                <Select
                    size="small"
                    value={filter}
                    onChange={(e) => setFilter(e.target.value as "all" | "project" | "ticket")}
                    sx={{ minWidth: 120 }}
                >
                    <MenuItem value="all">All</MenuItem>
                    <MenuItem value="project">Projects</MenuItem>
                    <MenuItem value="ticket">Tickets</MenuItem>
                </Select>
            </Box>

            <Alert severity="info" icon={<Warning />}>
                Items in trash will be permanently deleted after 30 days.
            </Alert>
            {!isAdmin && (
                <Alert severity="warning" sx={{ mt: 1 }}>
                    Viewing only. Ask an admin to restore items.
                </Alert>
            )}

            {isForbidden ? (
                <Alert severity="warning">
                    You currently do not have permission to view trash items. Ask an admin to grant read access or restore items for you.
                </Alert>
            ) : items.length === 0 ? (
                <Box sx={{ textAlign: "center", py: 8 }}>
                    <Delete sx={{ fontSize: 64, color: "text.disabled", mb: 2 }} />
                    <Typography variant="h6" color="text.secondary">
                        Trash is empty
                    </Typography>
                    <Typography variant="body2" color="text.disabled">
                        Deleted projects and tickets will appear here.
                    </Typography>
                </Box>
            ) : (
                <Stack spacing={2}>
                    {items.map((item) => (
                        <Card key={item.id} variant="outlined">
                            <CardContent sx={{ display: "grid", gap: 1.25 }}>
                                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                                    <Box>
                                        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.5 }}>
                                            <Chip
                                                label={item.type}
                                                size="small"
                                                color={item.type === "PROJECT" ? "primary" : "secondary"}
                                                variant="outlined"
                                            />
                                            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                                                {item.key}
                                            </Typography>
                                        </Box>
                                        <Typography variant="h6">
                                            {item.name}
                                        </Typography>
                                    </Box>
                                    <Chip
                                        label={`${item.daysRemaining} days left`}
                                        color={item.daysRemaining <= 7 ? "error" : item.daysRemaining <= 14 ? "warning" : "default"}
                                        size="small"
                                    />
                                </Box>

                                <Typography variant="caption" color="text.secondary">
                                    Deleted: {new Date(item.deletedAt).toLocaleString()}
                                </Typography>

                                <Box sx={{ display: "flex", gap: 2, pt: 1 }}>
                                    {isAdmin ? (
                                        <Button
                                            size="small"
                                            variant="contained"
                                            startIcon={<Restore />}
                                            onClick={() => setConfirmRestore(item)}
                                        >
                                            Restore
                                        </Button>
                                    ) : (
                                        <Typography variant="caption" color="text.disabled">
                                            View only (admins can restore)
                                        </Typography>
                                    )}
                                </Box>
                            </CardContent>
                        </Card>
                    ))}
                </Stack>
            )}

            {/* Restore Confirmation Dialog */}
            <Dialog open={Boolean(confirmRestore)} onClose={() => setConfirmRestore(null)}>
                <DialogTitle>Restore {confirmRestore?.type?.toLowerCase()}?</DialogTitle>
                <DialogContent>
                    <Typography>
                        {confirmRestore
                            ? `"${confirmRestore.name}" (${confirmRestore.key}) will be restored from trash.`
                            : ""}
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setConfirmRestore(null)}>Cancel</Button>
                    <Button
                        variant="contained"
                        onClick={handleRestore}
                        disabled={isPending}
                        startIcon={<Restore />}
                    >
                        Restore
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    );
}
