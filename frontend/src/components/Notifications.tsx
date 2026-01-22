import { Alert, Snackbar } from "@mui/material";
import React, { createContext, useCallback, useContext, useMemo, useState } from "react";

type Severity = "success" | "info" | "warning" | "error";

type NotificationContextValue = {
  notify: (message: string, severity?: Severity) => void;
  notifySuccess: (message: string) => void;
  notifyError: (message: string) => void;
};

const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

export function NotificationProvider({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");
  const [severity, setSeverity] = useState<Severity>("info");

  const notify = useCallback((msg: string, sev: Severity = "info") => {
    setMessage(msg);
    setSeverity(sev);
    setOpen(true);
  }, []);

  const value = useMemo<NotificationContextValue>(
    () => ({
      notify,
      notifySuccess: (msg: string) => notify(msg, "success"),
      notifyError: (msg: string) => notify(msg, "error")
    }),
    [notify]
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <Snackbar
        open={open}
        autoHideDuration={3000}
        onClose={() => setOpen(false)}
        anchorOrigin={{ vertical: "top", horizontal: "center" }}
      >
        <Alert severity={severity} onClose={() => setOpen(false)} sx={{ width: "100%" }}>
          {message}
        </Alert>
      </Snackbar>
    </NotificationContext.Provider>
  );
}

export function useNotify(): NotificationContextValue {
  const ctx = useContext(NotificationContext);
  if (!ctx) {
    throw new Error("NotificationContext not initialized");
  }
  return ctx;
}
