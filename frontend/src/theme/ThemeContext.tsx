import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { CssBaseline, ThemeProvider, createTheme, GlobalStyles } from "@mui/material";

type ThemeMode = "light" | "dark";

type ThemeContextValue = {
  mode: ThemeMode;
  toggle: () => void;
};

const STORAGE_KEY = "jira-lite-theme";

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeContextProvider({ children }: { children: React.ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === "light" || saved === "dark") return saved;
    return "light";
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, mode);
  }, [mode]);

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode,
          primary: { main: mode === "dark" ? "#5b9bd5" : "#1f4f75" },
          secondary: { main: "#f97316" },
          ...(mode === "dark" && {
            text: {
              primary: "#ffffff",
              secondary: "#b0bec5"
            },
            background: {
              default: "#121212",
              paper: "#1e1e1e"
            },
            action: {
              active: "#90caf9",
              hover: "rgba(144, 202, 249, 0.08)"
            }
          })
        },
        typography: { fontFamily: "IBM Plex Sans, system-ui, sans-serif" },
        components: {
          MuiButton: {
            styleOverrides: {
              outlined: ({ theme: t }) => ({
                ...(t.palette.mode === "dark" && {
                  borderColor: "#5b9bd5",
                  color: "#90caf9",
                  "&:hover": {
                    borderColor: "#90caf9",
                    backgroundColor: "rgba(144, 202, 249, 0.08)"
                  }
                })
              }),
              text: ({ theme: t }) => ({
                ...(t.palette.mode === "dark" && {
                  color: "#90caf9"
                })
              })
            }
          },
          MuiChip: {
            styleOverrides: {
              outlined: ({ theme: t }) => ({
                ...(t.palette.mode === "dark" && {
                  borderColor: "#5b9bd5",
                  color: "#e3f2fd"
                })
              })
            }
          }
        }
      }),
    [mode]
  );

  const value = useMemo(
    () => ({
      mode,
      toggle: () => setMode((prev) => (prev === "light" ? "dark" : "light"))
    }),
    [mode]
  );

  return (
    <ThemeContext.Provider value={value}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <GlobalStyles
          styles={{
            body: {
              backgroundColor: theme.palette.background.default,
              color: theme.palette.text.primary
            }
          }}
        />
        {children}
      </ThemeProvider>
    </ThemeContext.Provider>
  );
}

export function useThemeContext(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error("ThemeContext not provided");
  }
  return ctx;
}
