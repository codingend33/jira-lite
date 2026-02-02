import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import App from "./App";
import { AuthProvider } from "./auth/AuthContext";
import { NotificationProvider } from "./components/Notifications";
import { ThemeContextProvider } from "./theme/ThemeContext";
import "./styles.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false
    }
  }
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ThemeContextProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrowserRouter>
            <NotificationProvider>
              <App />
            </NotificationProvider>
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeContextProvider>
  </React.StrictMode>
);
