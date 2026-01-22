import { AppBar, Box, Button, Container, Toolbar, Typography } from "@mui/material";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function Layout() {
  const { logout, state } = useAuth();

  return (
    <Box minHeight="100vh">
      <AppBar position="static" color="transparent" elevation={0}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Jira Lite
          </Typography>
          <Button component={NavLink} to="/projects" color="inherit">
            Projects
          </Button>
          <Button component={NavLink} to="/tickets" color="inherit">
            Tickets
          </Button>
          <Box sx={{ flexGrow: 1 }} />
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
