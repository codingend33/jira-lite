import { Avatar, Button, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listMembersAdmin, removeMember } from "../api/members";
import { getProfile, getAvatarUrl } from "../api/profile";
import ErrorBanner from "../components/ErrorBanner";
import { useNotify } from "../components/Notifications";

export default function SettingsMembersPage() {
  const queryClient = useQueryClient();
  const { notifySuccess, notifyError } = useNotify();

  const membersQuery = useQuery({
    queryKey: ["members-admin"],
    queryFn: () => listMembersAdmin()
  });
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

  return (
    <Stack spacing={3}>
      <Typography variant="h4" fontWeight={700}>
        Members
      </Typography>
      <ErrorBanner error={membersQuery.error || removeMutation.error} />
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
              {(membersQuery.data ?? []).map((m) => (
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
                <TableCell>{m.role}</TableCell>
                <TableCell>{m.status}</TableCell>
                <TableCell align="right">
                  {m.role !== "ADMIN" && (
                    <Button
                      color="error"
                      onClick={() => removeMutation.mutate(m.userId)}
                      disabled={removeMutation.isPending}
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
    </Stack>
  );
}
