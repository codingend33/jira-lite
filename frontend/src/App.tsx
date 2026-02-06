import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import Loading from "./components/Loading";
import { lazy, Suspense, useEffect } from "react";
import { useNotify } from "./components/Notifications";

const LoginPage = lazy(() => import("./pages/LoginPage"));
const CreateOrganizationPage = lazy(() => import("./pages/CreateOrganizationPage"));
const AcceptInvitationPage = lazy(() => import("./pages/AcceptInvitationPage"));
const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const ProjectsPage = lazy(() => import("./pages/ProjectsPage"));
const ProjectDetailPage = lazy(() => import("./pages/ProjectDetailPage"));
const TicketsPage = lazy(() => import("./pages/TicketsPage"));
const TicketDetailPage = lazy(() => import("./pages/TicketDetailPage"));
const TicketFormPage = lazy(() => import("./pages/TicketFormPage"));
const SettingsProfilePage = lazy(() => import("./pages/SettingsProfilePage"));
const SettingsMembersPage = lazy(() => import("./pages/SettingsMembersPage"));
const NotificationsPage = lazy(() => import("./pages/NotificationsPage"));
const TrashPage = lazy(() => import("./pages/TrashPage"));

export default function App() {
  const navigate = useNavigate();
  const { notifyError } = useNotify();

  useEffect(() => {
    const handleAuthFailed = () => {
      notifyError("Session expired, please sign in again.");
      navigate("/login", { replace: true });
    };
    const handleForbidden = () => {
      const path = window.location.pathname;
      if (path.startsWith("/trash") || path.startsWith("/settings/members")) {
        return;
      }
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
    <Suspense fallback={<Loading />}>
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
    </Suspense>
  );
}
