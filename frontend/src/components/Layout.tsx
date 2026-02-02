import { AppBar, Box, Button, Container, IconButton, Toolbar, Typography, Badge, Menu, MenuItem, ListItemText, TextField, InputAdornment } from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useThemeContext } from "../theme/ThemeContext";
import Brightness4Icon from "@mui/icons-material/Brightness4";
import Brightness7Icon from "@mui/icons-material/Brightness7";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "../api/client";

export default function Layout() {
  const { logout, state } = useAuth();
  const { mode, toggle } = useThemeContext();
  const queryClient = useQueryClient();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const navigate = useNavigate();

  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: () => apiRequest<any[]>("/notifications", { method: "GET" })
  });

  const unread = useMemo(
    () => (notificationsQuery.data ?? []).filter((n) => !n.isRead).length,
    [notificationsQuery.data]
  );

  const markReadMutation = useMutation({
    mutationFn: (id: string) => apiRequest(`/notifications/${id}/read`, { method: "PATCH" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] })
  });

  return (
    <Box minHeight="100vh">
      <AppBar position="static" color="transparent" elevation={0}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Jira Lite
          </Typography>
          <Button component={NavLink} to="/dashboard" color="inherit">
            Dashboard
          </Button>
          <Button component={NavLink} to="/projects" color="inherit">
            Projects
          </Button>
          <Button component={NavLink} to="/tickets" color="inherit">
            Tickets
          </Button>
          <Button component={NavLink} to="/settings/profile" color="inherit">
            Settings
          </Button>
          <Box sx={{ flexGrow: 1 }} />
          <TextField
            size="small"
            placeholder="Search tickets..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && searchTerm.trim()) {
                navigate(`/tickets?keyword=${encodeURIComponent(searchTerm.trim())}`);
                setAnchorEl(null);
              }
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              )
            }}
            sx={{ minWidth: 220, mr: 2 }}
          />
          <IconButton color="inherit" onClick={toggle} size="small" aria-label="Toggle theme">
            {mode === "light" ? <Brightness4Icon /> : <Brightness7Icon />}
          </IconButton>
          <IconButton
            color="inherit"
            aria-label="Notifications"
            onClick={(e) => setAnchorEl(e.currentTarget)}
            size="small"
          >
            <Badge color="error" badgeContent={unread || null}>
              <NotificationsNoneIcon />
            </Badge>
          </IconButton>
          <Menu
            open={Boolean(anchorEl)}
            onClose={() => setAnchorEl(null)}
            anchorEl={anchorEl}
            slotProps={{ paper: { sx: { width: 320 } } }}
          >
            {(notificationsQuery.data ?? []).map((n) => (
              <MenuItem
                key={n.id}
                dense
                onClick={() => markReadMutation.mutate(n.id)}
                disabled={markReadMutation.isPending}
              >
                <ListItemText
                  primary={n.content}
                  secondary={new Date(n.createdAt).toLocaleString()}
                  primaryTypographyProps={{ fontWeight: n.isRead ? 400 : 700 }}
                />
              </MenuItem>
            ))}
            {(notificationsQuery.data?.length ?? 0) === 0 && (
              <MenuItem dense disabled>
                <ListItemText primary="No notifications" />
              </MenuItem>
            )}
          </Menu>
          <Typography variant="body2" sx={{ opacity: 0.7 }}>
            {state.profile?.email ?? state.profile?.sub ?? ""}
          </Typography>
          <Button variant="outlined" color="primary" onClick={logout}>
            Logout
          </Button>
        </Toolbar>
      </AppBar>
      <Container sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
