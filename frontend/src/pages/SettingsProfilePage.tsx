import { Avatar, Box, Button, Paper, Stack, TextField, Typography } from "@mui/material";
import { useEffect, useRef, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getProfile, updateProfile, presignAvatar } from "../api/profile";
import { useNotify } from "../components/Notifications";

export default function SettingsProfilePage() {
  const [displayName, setDisplayName] = useState("");
  const [avatarKey, setAvatarKey] = useState("");
  const [avatarPreview, setAvatarPreview] = useState<string | undefined>();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const { notifySuccess, notifyError } = useNotify();
  const queryClient = useQueryClient();

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile()
  });

  const saveMutation = useMutation({
    mutationFn: () => updateProfile({ displayName, avatarS3Key: avatarKey }),
    onSuccess: () => {
      notifySuccess("Profile updated");
      queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
    onError: () => notifyError("Failed to update profile")
  });

  useEffect(() => {
    if (profileQuery.data) {
      setDisplayName(profileQuery.data.displayName ?? "");
      setAvatarKey(profileQuery.data.avatarS3Key ?? "");
      setAvatarPreview(undefined);
    }
  }, [profileQuery.data]);

  const handlePickAvatar = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const presign = await presignAvatar(file.name, file.type || "application/octet-stream");
      await fetch(presign.uploadUrl, {
        method: "PUT",
        headers: presign.headers,
        body: file
      });
      setAvatarKey(presign.key);
      setAvatarPreview(URL.createObjectURL(file));
      notifySuccess("Avatar uploaded, click Save to apply.");
    } catch {
      notifyError("Avatar upload failed");
    }
  };

  return (
    <Stack spacing={3}>
      <Typography variant="h4" fontWeight={700}>
        Profile
      </Typography>
      <Paper sx={{ p: 3, display: "grid", gap: 2, maxWidth: 480 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <Avatar src={avatarPreview} sx={{ width: 72, height: 72 }}>
            {displayName?.[0]}
          </Avatar>
          <Button variant="outlined" onClick={handlePickAvatar}>
            Upload Avatar
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            hidden
            aria-label="Upload Avatar"
            onChange={handleFileChange}
          />
        </Stack>
        <TextField
          label="Display Name"
          value={displayName}
          onChange={(e) => setDisplayName(e.target.value)}
        />
        <TextField
          label="Avatar S3 Key"
          helperText="Auto-filled after upload"
          value={avatarKey}
          onChange={(e) => setAvatarKey(e.target.value)}
        />
        <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end" }}>
          <Button
            variant="contained"
            onClick={() => saveMutation.mutate()}
            disabled={saveMutation.isPending}
          >
            {saveMutation.isPending ? "Saving..." : "Save"}
          </Button>
        </Box>
      </Paper>
      <Paper sx={{ p: 3, maxWidth: 480 }}>
        <Typography variant="h6">Security</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          Change password via Cognito hosted UI.
        </Typography>
        <Button variant="outlined" sx={{ mt: 2 }}>
          Change Password
        </Button>
      </Paper>
      {profileQuery.data?.lastLoginAt && (
        <Typography variant="body2" color="text.secondary">
          Last login: {new Date(profileQuery.data.lastLoginAt).toLocaleString()}
        </Typography>
      )}
    </Stack>
  );
}
