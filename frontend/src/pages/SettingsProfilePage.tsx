import {
  Avatar,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper,
  Stack,
  TextField,
  Typography,
  Alert,
  IconButton,
  InputAdornment
} from "@mui/material";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";
import { useEffect, useRef, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getProfile, updateProfile, presignAvatar, getAvatarUrl } from "../api/profile";
import { useNotify } from "../components/Notifications";
import { changePassword } from "../api/changePassword";

export default function SettingsProfilePage() {
  const [displayName, setDisplayName] = useState("");
  const [avatarKey, setAvatarKey] = useState("");
  const [avatarPreview, setAvatarPreview] = useState<string | undefined>();
  const [changePwdOpen, setChangePwdOpen] = useState(false);
  const [currentPwd, setCurrentPwd] = useState("");
  const [newPwd, setNewPwd] = useState("");
  const [confirmPwd, setConfirmPwd] = useState("");
  const [changePwdError, setChangePwdError] = useState<string | null>(null);
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
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
  const changePwdMutation = useMutation({
    mutationFn: () => changePassword(currentPwd, newPwd),
    onSuccess: () => {
      notifySuccess("Password updated");
      setChangePwdOpen(false);
      setCurrentPwd("");
      setNewPwd("");
      setConfirmPwd("");
      setChangePwdError(null);
    },
    onError: (err: any) => {
      let msg = err?.message || "Failed to change password";
      if (err?.name === "NotAuthorizedException") {
        if (msg.includes("required scopes")) {
          msg = "Access token missing required scopes. Please sign out and log in again, then retry.";
        } else {
          msg = "Current password is incorrect.";
        }
      } else if (err?.name === "InvalidPasswordException") {
        msg = "New password does not meet policy.";
      }
      setChangePwdError(msg);
      notifyError(msg);
    }
  });

  useEffect(() => {
    if (profileQuery.data) {
      setDisplayName(profileQuery.data.displayName ?? "");
      setAvatarKey(profileQuery.data.avatarS3Key ?? "");
      setAvatarPreview(undefined);
      if (profileQuery.data.avatarS3Key) {
        getAvatarUrl().then((url) => {
          if (url) setAvatarPreview(url);
        });
      }
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
          {"Change your password without leaving the app. If you forgot your current password, go to the login page and use \"Forgot password\"."}
        </Typography>
        <Button variant="outlined" sx={{ mt: 2 }} onClick={() => setChangePwdOpen(true)}>
          Change Password
        </Button>
      </Paper>
      {profileQuery.data?.lastLoginAt && (
        <Typography variant="body2" color="text.secondary">
          Last login: {new Date(profileQuery.data.lastLoginAt).toLocaleString()}
        </Typography>
      )}

      <Dialog
        open={changePwdOpen}
        fullWidth
        maxWidth="sm"
        onClose={() => !changePwdMutation.isPending && setChangePwdOpen(false)}
      >
        <DialogTitle>Change Password</DialogTitle>
        <DialogContent sx={{ display: "grid", gap: 2, mt: 1, minWidth: 460 }}>
          {changePwdError && <Alert severity="error">{changePwdError}</Alert>}
          <TextField
            label="Current Password"
            type={showCurrent ? "text" : "password"}
            value={currentPwd}
            onChange={(e) => setCurrentPwd(e.target.value)}
            autoFocus
            fullWidth
            margin="dense"
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label="toggle current password visibility"
                    onClick={() => setShowCurrent((v) => !v)}
                    edge="end"
                  >
                    {showCurrent ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              )
            }}
          />
          <TextField
            label="New Password"
            type={showNew ? "text" : "password"}
            value={newPwd}
            onChange={(e) => setNewPwd(e.target.value)}
            helperText="Must satisfy your Cognito password policy."
            fullWidth
            margin="dense"
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label="toggle new password visibility"
                    onClick={() => setShowNew((v) => !v)}
                    edge="end"
                  >
                    {showNew ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              )
            }}
          />
          <TextField
            label="Confirm New Password"
            type={showConfirm ? "text" : "password"}
            value={confirmPwd}
            onChange={(e) => setConfirmPwd(e.target.value)}
            error={Boolean(confirmPwd) && newPwd !== confirmPwd}
            helperText={newPwd !== confirmPwd ? "Passwords do not match." : " "}
            fullWidth
            margin="dense"
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label="toggle confirm password visibility"
                    onClick={() => setShowConfirm((v) => !v)}
                    edge="end"
                  >
                    {showConfirm ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              )
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setChangePwdOpen(false)} disabled={changePwdMutation.isPending}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={() => changePwdMutation.mutate()}
            disabled={
              changePwdMutation.isPending || !currentPwd || !newPwd || newPwd !== confirmPwd
            }
          >
            {changePwdMutation.isPending ? "Saving..." : "Change Password"}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
