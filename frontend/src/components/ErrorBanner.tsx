import { Alert, AlertTitle } from "@mui/material";
import { ApiError } from "../api/client";

export default function ErrorBanner({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }

  if (error instanceof ApiError) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        <AlertTitle>{error.payload?.code ?? "ERROR"}</AlertTitle>
        {error.payload?.message ?? error.message}
        {error.payload?.traceId ? ` (traceId: ${error.payload.traceId})` : ""}
      </Alert>
    );
  }

  return (
    <Alert severity="error" sx={{ mb: 2 }}>
      <AlertTitle>ERROR</AlertTitle>
      Unexpected error occurred.
    </Alert>
  );
}
