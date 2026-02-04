import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import CreateOrganizationPage from "./pages/CreateOrganizationPage";
import AcceptInvitationPage from "./pages/AcceptInvitationPage";
import DashboardPage from "./pages/DashboardPage";
import ProjectsPage from "./pages/ProjectsPage";
import ProjectDetailPage from "./pages/ProjectDetailPage";
import TicketsPage from "./pages/TicketsPage";
import TicketDetailPage from "./pages/TicketDetailPage";
import TicketFormPage from "./pages/TicketFormPage";
import SettingsProfilePage from "./pages/SettingsProfilePage";
import SettingsMembersPage from "./pages/SettingsMembersPage";
import NotificationsPage from "./pages/NotificationsPage";
import TrashPage from "./pages/TrashPage";
import { useEffect } from "react";
import { useNotify } from "./components/Notifications";

export default function App() {
  const navigate = useNavigate();
  const { notifyError } = useNotify();

  useEffect(() => {
    const handleAuthFailed = () => {
      notifyError("Session expired, please sign in again.");
      navigate("/login", { replace: true });
    };
    const handleForbidden = () => {
      notifyError("You don't have permission to perform this action.");
    };
    window.addEventListener("api:auth-failed", handleAuthFailed);
    window.addEventListener("api:forbidden", handleForbidden);
    return () => {
      window.removeEventListener("api:auth-failed", handleAuthFailed);
      window.removeEventListener("api:forbidden", handleForbidden);
    };
  }, [navigate, notifyError]);

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/invite" element={<AcceptInvitationPage />} />
      <Route
        path="/create-org"
        element={
          <ProtectedRoute>
            <CreateOrganizationPage />
          </ProtectedRoute>
        }
      />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
        <Route path="/tickets" element={<TicketsPage />} />
        <Route path="/tickets/new" element={<TicketFormPage mode="create" />} />
        <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
        <Route path="/tickets/:ticketId/edit" element={<TicketFormPage mode="edit" />} />
        <Route path="/trash" element={<TrashPage />} />
        <Route path="/settings/profile" element={<SettingsProfilePage />} />
        <Route path="/settings/members" element={<SettingsMembersPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/projects" replace />} />
    </Routes>
  );
}
