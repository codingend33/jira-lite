import { Avatar, Button, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography, Select, MenuItem, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions } from "@mui/material";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listMembersAdmin, removeMember, updateMember } from "../api/members";
import { getProfile, getAvatarUrl } from "../api/profile";
import ErrorBanner from "../components/ErrorBanner";
import { useNotify } from "../components/Notifications";
import { useOrgMembers } from "../query/memberQueries";
import { useAuth } from "../auth/AuthContext";
import InviteMembersModal from "../components/InviteMembersModal";

export default function SettingsMembersPage() {
  const queryClient = useQueryClient();
  const { notifySuccess, notifyError } = useNotify();
  const { state: authState } = useAuth();
  const isAdmin = authState.profile?.["cognito:groups"]?.some((g: string) => g.toUpperCase() === "ADMIN");
  const [inviteOpen, setInviteOpen] = useState(false);

  const membersQuery = useQuery({
    queryKey: ["members-admin"],
    queryFn: () => listMembersAdmin(),
    enabled: isAdmin
  });
  const membersLiteQuery = useOrgMembers();
  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile()
  });
  const avatarQuery = useQuery({
    queryKey: ["profile", "avatar-url"],
    queryFn: () => getAvatarUrl(),
    enabled: Boolean(profileQuery.data?.avatarS3Key)
  });

  const removeMutation = useMutation({
    mutationFn: (userId: string) => removeMember(userId),
    onSuccess: () => {
      notifySuccess("Member removed");
      queryClient.invalidateQueries({ queryKey: ["members-admin"] });
    },
    onError: () => notifyError("Failed to remove member")
  });

  const updateMutation = useMutation({
    mutationFn: (data: { userId: string; role: string }) => updateMember(data.userId, { role: data.role }),
    onSuccess: () => {
      notifySuccess("Member role updated");
      queryClient.invalidateQueries({ queryKey: ["members-admin"] });
    },
    onError: () => notifyError("Failed to update member role")
  });

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingRoleChange, setPendingRoleChange] = useState<{ userId: string; role: string; name: string } | null>(null);

  const handleRoleChangeRequest = (userId: string, role: string, name: string) => {
    setPendingRoleChange({ userId, role, name });
    setConfirmOpen(true);
  };

  const confirmRoleChange = () => {
    if (pendingRoleChange) {
      updateMutation.mutate({ userId: pendingRoleChange.userId, role: pendingRoleChange.role });
      setPendingRoleChange(null);
      setConfirmOpen(false);
    }
  };

  const cancelRoleChange = () => {
    setPendingRoleChange(null);
    setConfirmOpen(false);
  };

  const memberRows = (isAdmin ? membersQuery.data : membersLiteQuery.data) ?? [];

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h4" fontWeight={700}>
          Members
        </Typography>
        {isAdmin && (
          <Button variant="contained" onClick={() => setInviteOpen(true)}>
            Invite Members
          </Button>
        )}
      </Stack>
      <ErrorBanner error={(isAdmin ? membersQuery.error : membersLiteQuery.error) || removeMutation.error} />
      {!isAdmin && (
        <Typography variant="body2" color="text.secondary">
          View only: contact an admin to change roles or invite/remove members.
        </Typography>
      )}
      <Paper sx={{ p: 2 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Avatar</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {memberRows.map((m) => (
              <TableRow key={m.userId}>
                <TableCell>
                  <Avatar
                    src={
                      m.avatarUrl
                        ? m.avatarUrl
                        : m.userId === profileQuery.data?.id
                          ? avatarQuery.data ?? undefined
                          : undefined
                    }
                  >
                    {(m.displayName || m.email || "?")[0]}
                  </Avatar>
                </TableCell>
                <TableCell>{m.displayName || "—"}</TableCell>
                <TableCell>{m.email || "—"}</TableCell>
                <TableCell>
                  <Select
                    size="small"
                    value={m.role}
                    onChange={(e) => handleRoleChangeRequest(m.userId, e.target.value, m.displayName || m.email || "User")}
                    disabled={!isAdmin || updateMutation.isPending || m.userId === profileQuery.data?.id}
                    sx={{ minWidth: 120 }}
                  >
                    <MenuItem value="ADMIN">ADMIN</MenuItem>
                    <MenuItem value="MEMBER">MEMBER</MenuItem>
                  </Select>
                </TableCell>
                <TableCell>{m.status}</TableCell>
                <TableCell align="right">
                  {m.role !== "ADMIN" && (
                    <Button
                      color="error"
                      onClick={() => (isAdmin ? removeMutation.mutate(m.userId) : notifyError("No permission"))}
                      disabled={!isAdmin || removeMutation.isPending}
                    >
                      Remove
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
      <InviteMembersModal open={inviteOpen} onClose={() => setInviteOpen(false)} />

      {/* Confirmation Dialog */}
      <Dialog open={confirmOpen} onClose={cancelRoleChange}>
        <DialogTitle>Confirm Role Change</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to change the role of <b>{pendingRoleChange?.name}</b> to <b>{pendingRoleChange?.role}</b>?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={cancelRoleChange}>Cancel</Button>
          <Button onClick={confirmRoleChange} autoFocus variant="contained">
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </Stack >
  );
}
