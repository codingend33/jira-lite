import {
  AppBar,
  Avatar,
  Badge,
  Box,
  Button,
  Container,
  IconButton,
  InputAdornment,
  ListItemText,
  Menu,
  MenuItem,
  TextField,
  Toolbar,
  Typography
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useThemeContext } from "../theme/ThemeContext";
import Brightness4Icon from "@mui/icons-material/Brightness4";
import Brightness7Icon from "@mui/icons-material/Brightness7";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import { useMemo, useState, useEffect, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getProfile, getAvatarUrl } from "../api/profile";
import { useOrgMembers } from "../query/memberQueries";
import { listNotifications, markNotificationRead, Notification, connectNotificationStream } from "../api/notifications";

export default function Layout() {
  const { logout, state } = useAuth();
  const { mode, toggle } = useThemeContext();
  const queryClient = useQueryClient();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  // user menu anchor
  const [userMenuEl, setUserMenuEl] = useState<null | HTMLElement>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const navigate = useNavigate();
  const membersQuery = useOrgMembers();

  const notificationsQuery = useQuery({
    queryKey: ["notifications"],
    queryFn: () => listNotifications({ page: 0, size: 20 }),
    staleTime: 0,
    gcTime: 0,
    // 保底 15s 轮询一次
    refetchInterval: 15000
  });

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: () => getProfile()
  });
  const avatarQuery = useQuery({
    queryKey: ["profile", "avatar-url"],
    queryFn: async () => (await getAvatarUrl()) ?? null,
    enabled: Boolean(profileQuery.data?.avatarS3Key)
  });

  // SSE: start stream once access token存在
  useEffect(() => {
    if (!state.tokens?.accessToken) return;
    let cancelled = false;
    const start = async () => {
      try {
        await connectNotificationStream(state.tokens!.accessToken, (ev) => {
          if (cancelled) return;
          if (ev.type === "notification") {
            queryClient.invalidateQueries({ queryKey: ["notifications"] });
          }
        });
      } catch {
        // 连接失败则稍后重试
        setTimeout(() => {
          if (!cancelled) start();
        }, 5000);
      }
    };
    start();
    return () => {
      cancelled = true;
    };
  }, [state.tokens?.accessToken, state.tokens, queryClient]);

  const memberMap = useMemo(() => {
    const map = new Map<string, string>();
    membersQuery.data?.forEach((m) => {
      map.set(m.userId, m.displayName || m.email || m.userId);
    });
    return map;
  }, [membersQuery.data]);

  const humanizeText = useCallback(
    (text: string) => {
      let result = text;
      memberMap.forEach((name, id) => {
        result = result.split(id).join(name);
      });
      return result;
    },
    [memberMap]
  );

  const summarize = (text: string, max = 90) => {
    if (text.length <= max) return text;
    return `${text.slice(0, max)}…`;
  };

  const notifications: Notification[] = useMemo(() => {
    const data = notificationsQuery.data as any;
    if (Array.isArray(data)) return data;
    if (data?.content) return data.content;
    return [];
  }, [notificationsQuery.data]);

  const unread = useMemo(
    () => notifications.filter((n: Notification) => !n.read && !n.isRead).length,
    [notifications]
  );

  const markReadMutation = useMutation({
    mutationFn: (id: string) => markNotificationRead(id),
    onSuccess: (_, id) => {
      queryClient.setQueryData(["notifications"], (prev: any) => {
        if (!prev) return prev;
        const list = Array.isArray(prev) ? prev : prev.content;
        if (!list) return prev;
        const nextList = list.map((n: Notification) => (n.id === id ? { ...n, read: true, isRead: true } : n));
        return Array.isArray(prev) ? nextList : { ...prev, content: nextList };
      });
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    }
  });

  const menuItems = useMemo(() => notifications.slice(0, 10), [notifications]);
  const badgeCount = unread;

  const displayName = profileQuery.data?.displayName || state.profile?.email || state.profile?.sub || "";
  const accountLabel = displayName || "Account";
  const avatarLetter = accountLabel.trim().charAt(0).toUpperCase();
  const avatarSrc = avatarQuery.data ?? undefined;

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
          <Button component={NavLink} to="/trash" color="inherit">
            Trash
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
            <Badge color="error" badgeContent={badgeCount || null}>
              <NotificationsNoneIcon />
            </Badge>
          </IconButton>
          <Menu
            open={Boolean(anchorEl)}
            onClose={() => setAnchorEl(null)}
            anchorEl={anchorEl}
            slotProps={{ paper: { sx: { width: 320 } } }}
          >
            {menuItems.map((n: Notification) => {
              const content = n.content ?? (n as any).message ?? "";
              const full = humanizeText(content);
              const short = summarize(full);
              return (
                <MenuItem
                  key={n.id}
                  dense
                  onClick={() => markReadMutation.mutate(n.id)}
                  disabled={markReadMutation.isPending}
                >
                  <ListItemText
                    primary={short}
                    secondary={new Date(n.createdAt).toLocaleString()}
                    primaryTypographyProps={{
                      fontWeight: n.read || n.isRead ? 400 : 700,
                      noWrap: true,
                      title: full
                    }}
                  />
                </MenuItem>
              );
            })}
            {menuItems.length === 0 && (
              <MenuItem dense disabled>
                <ListItemText primary="No notifications" />
              </MenuItem>
            )}
            {notifications.length > 0 && (
              <MenuItem
                dense
                onClick={() => {
                  setAnchorEl(null);
                  navigate("/notifications");
                }}
              >
                <ListItemText primary="View all" />
              </MenuItem>
            )}
          </Menu>
          <Button
            color="inherit"
            onClick={(e) => setUserMenuEl(e.currentTarget)}
            startIcon={
              <Avatar sx={{ width: 32, height: 32 }} alt={accountLabel} src={avatarSrc}>
                {avatarLetter}
              </Avatar>
            }
            aria-haspopup="true"
            aria-controls={userMenuEl ? "user-menu" : undefined}
            sx={{ textTransform: "none", fontWeight: 600 }}
          >
            {accountLabel}
          </Button>
          <Menu
            id="user-menu"
            anchorEl={userMenuEl}
            open={Boolean(userMenuEl)}
            onClose={() => setUserMenuEl(null)}
          >
            <MenuItem
              onClick={() => {
                setUserMenuEl(null);
                navigate("/settings/profile");
              }}
            >
              <ListItemText primary="Settings" />
            </MenuItem>
            <MenuItem
              onClick={() => {
                setUserMenuEl(null);
                logout();
              }}
            >
              <ListItemText primary="Logout" />
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>
      <Container sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
