import { Grid, Paper, Stack, Typography, Box, Divider, Chip } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "../api/client";
import { listAuditLogs } from "../api/audit";

type Metrics = {
  activeProjects: number;
  myTickets: number;
  members: number;
};

export default function DashboardPage() {
  const metricsQuery = useQuery<Metrics>({
    queryKey: ["metrics"],
    queryFn: () => apiRequest("/dashboard/metrics", { method: "GET" })
  });

  const activityQuery = useQuery({
    queryKey: ["audit-logs"],
    queryFn: () => listAuditLogs()
  });

  const metrics = [
    { title: "Active Projects", value: metricsQuery.data?.activeProjects ?? "—" },
    { title: "My Tickets", value: metricsQuery.data?.myTickets ?? "—" },
    { title: "Team Members", value: metricsQuery.data?.members ?? "—" }
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4" fontWeight={700}>
        Dashboard
      </Typography>
      <Grid container spacing={2}>
        {metrics.map((m) => (
          <Grid item xs={12} md={4} key={m.title}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="subtitle2" color="text.secondary">
                {m.title}
              </Typography>
              <Typography variant="h5" fontWeight={700}>
                {m.value}
              </Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>
      <Paper sx={{ p: 2 }}>
        <Typography variant="h6">Recent Activity</Typography>
        <Divider sx={{ my: 1 }} />
        <Stack spacing={1}>
          {(activityQuery.data ?? []).map((log) => (
            <Box key={log.id} sx={{ color: "text.secondary" }} display="flex" gap={1} alignItems="center">
              <Chip size="small" label={log.action} />
              <span>{log.details ?? `${log.entityType} ${log.entityId}`}</span>
              <span>· {new Date(log.createdAt).toLocaleString()}</span>
            </Box>
          ))}
          {activityQuery.data?.length === 0 && (
            <Typography variant="body2" color="text.secondary">
              Recent activity will appear here
            </Typography>
          )}
        </Stack>
      </Paper>
    </Stack>
  );
}
