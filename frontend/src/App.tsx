import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import CreateOrganizationPage from "./pages/CreateOrganizationPage";
import AcceptInvitationPage from "./pages/AcceptInvitationPage";
import ProjectsPage from "./pages/ProjectsPage";
import ProjectDetailPage from "./pages/ProjectDetailPage";
import TicketsPage from "./pages/TicketsPage";
import TicketDetailPage from "./pages/TicketDetailPage";
import TicketFormPage from "./pages/TicketFormPage";
import { useEffect } from "react";
import { useNotify } from "./components/Notifications";

export default function App() {
  const navigate = useNavigate();
  const { notifyError } = useNotify();

  useEffect(() => {
    const handleAuthFailed = () => {
      notifyError("登录已过期，请重新登录");
      navigate("/login", { replace: true });
    };
    const handleForbidden = () => {
      notifyError("没有权限执行此操作");
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
        <Route path="/" element={<Navigate to="/projects" replace />} />
        <Route path="/projects" element={<ProjectsPage />} />
        <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
        <Route path="/tickets" element={<TicketsPage />} />
        <Route path="/tickets/new" element={<TicketFormPage mode="create" />} />
        <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
        <Route path="/tickets/:ticketId/edit" element={<TicketFormPage mode="edit" />} />
      </Route>
      <Route path="*" element={<Navigate to="/projects" replace />} />
    </Routes>
  );
}
