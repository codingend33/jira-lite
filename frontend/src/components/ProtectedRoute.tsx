import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ProfileWithOrg } from "../types/profile";

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, state } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  // Check if user has org_id in their JWT profile
  const orgId = (state.profile as ProfileWithOrg | null)?.["custom:org_id"];
  if (!orgId && location.pathname !== "/create-org") {
    return <Navigate to="/create-org" replace />;
  }

  return <>{children}</>;
}
