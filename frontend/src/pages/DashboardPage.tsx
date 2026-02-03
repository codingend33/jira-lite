import { Paper, Stack, Typography, Box, Divider, Chip, Button, TextField, MenuItem } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "../api/client";
import { listAuditLogs } from "../api/audit";
import { PagedResponse } from "../api/types";
import { useNavigate } from "react-router-dom";
import { useOrgMembers } from "../query/memberQueries";
import { useMemo, useState, useEffect, useCallback } from "react";
import { AuditLog } from "../api/audit";

type Metrics = {
  activeProjects: number;
  myTickets: number;
  members: number;
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const metricsQuery = useQuery<Metrics>({
    queryKey: ["metrics"],
    queryFn: () => apiRequest("/dashboard/metrics", { method: "GET" })
  });

  const pageSize = 20;
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<AuditLog[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [filterType, setFilterType] = useState<string>("ALL");
  const [filterText, setFilterText] = useState<string>("");

  const activityQuery = useQuery<PagedResponse<AuditLog>>({
    queryKey: ["audit-logs", page] as const,
    queryFn: () => listAuditLogs({ page, size: pageSize })
  });
  const membersQuery = useOrgMembers();
  const [visibleCount, setVisibleCount] = useState(20);

  useEffect(() => {
    const data = activityQuery.data;
    if (!data) return;
    if (page === 0) {
      setItems(data.content);
    } else {
      setItems((prev) => [...prev, ...data.content]);
    }
    setHasMore(page + 1 < data.page.totalPages);
  }, [activityQuery.data, page]);

  const memberMap = useMemo(() => {
    const map = new Map<string, string>();
    const data: any = membersQuery.data as any;
    const arr = Array.isArray(data) ? data : data?.content ?? [];
    arr.forEach((m: any) => map.set(m.userId, m.displayName || m.email || m.userId));
    return map;
  }, [membersQuery.data]);

  const humanize = useCallback(
    (text: string) => {
      let t = text;
      memberMap.forEach((name, id) => {
        t = t.split(id).join(name);
      });
      return t;
    },
    [memberMap]
  );

  const summarize = (text: string, max = 110) => {
    if (!text) return text;
    return text.length > max ? `${text.slice(0, max)}…` : text;
  };

  const metrics = [
    { title: "Active Projects", value: metricsQuery.data?.activeProjects ?? "—", to: "/projects" },
    { title: "My Tickets", value: metricsQuery.data?.myTickets ?? "—", to: "/tickets?assignedTo=me" },
    { title: "Team Members", value: metricsQuery.data?.members ?? "—", to: "/settings/members" }
  ];

  const filteredItems = useMemo(() => {
    return items.filter((log) => {
      const matchType = filterType === "ALL" || log.action === filterType;
      const full = humanize(log.details ?? `${log.entityType} ${log.entityId}`);
      const matchText = !filterText || full.toLowerCase().includes(filterText.toLowerCase());
      return matchType && matchText;
    });
  }, [items, filterType, filterText, humanize]);

  const groupedActivities = useMemo(() => {
    const view = filteredItems.slice(0, visibleCount);
    const groups: { label: string; entries: typeof items }[] = [];
    const today = new Date();
    const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
    const todayStart = startOfDay(today);
    const yesterdayStart = todayStart - 24 * 3600 * 1000;
    view.forEach((log) => {
      const t = new Date(log.createdAt).getTime();
      const label =
        t >= todayStart
          ? "Today"
          : t >= yesterdayStart
          ? "Yesterday"
          : "Earlier";
      const group = groups.find((g) => g.label === label);
      if (group) {
        group.entries.push(log);
      } else {
        groups.push({ label, entries: [log] });
      }
    });
    return groups;
  }, [filteredItems, visibleCount]);

  const actionOptions = useMemo(() => {
    const set = new Set(items.map((i) => i.action));
    return ["ALL", ...Array.from(set)];
  }, [items]);

  return (
    <Stack spacing={3} sx={{ maxWidth: 1200, px: 2 }}>
      <Typography variant="h4" fontWeight={700}>
        Dashboard
      </Typography>
      <Stack
        direction={{ xs: "column", md: "row" }}
        spacing={2}
        useFlexGap
        flexWrap="wrap"
        sx={{ width: "100%" }}
      >
        {metrics.map((m) => (
          <Paper
            key={m.title}
            sx={{
              p: 2,
              cursor: "pointer",
              ":hover": { bgcolor: "action.hover" },
              flex: 1,
              minWidth: { xs: "100%", md: 260 }
            }}
            onClick={() => navigate(m.to)}
          >
            <Typography variant="subtitle2" color="text.secondary">
              {m.title}
            </Typography>
            <Typography variant="h5" fontWeight={700}>
              {m.value}
            </Typography>
          </Paper>
        ))}
      </Stack>
      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={2} alignItems={{ md: "center" }} justifyContent="space-between">
          <Typography variant="h6">Recent Activity</Typography>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1} width={{ xs: "100%", md: "auto" }}>
            <TextField
              select
              size="small"
              label="Type"
              value={filterType}
              onChange={(e) => {
                setVisibleCount(20);
                setFilterType(e.target.value);
              }}
              sx={{ minWidth: 160 }}
            >
              {actionOptions.map((opt) => (
                <MenuItem key={opt} value={opt}>
                  {opt}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              size="small"
              label="Filter text"
              value={filterText}
              onChange={(e) => {
                setVisibleCount(20);
                setFilterText(e.target.value);
              }}
            />
          </Stack>
        </Stack>
        <Divider sx={{ my: 1 }} />
        <Stack spacing={1}>
          {groupedActivities.map((group) => (
            <Box key={group.label}>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                {group.label}
              </Typography>
              <Stack spacing={1}>
                {group.entries.map((log) => {
                  const baseText = log.details ?? `${log.entityType} ${log.entityId}`;
                  const withKey =
                    log.entityType?.toUpperCase() === "TICKET" && log.entityId
                      ? `${log.entityId}: ${baseText}`
                      : baseText;
                  const full = humanize(withKey);
                  return (
                    <Box key={log.id} sx={{ color: "text.secondary" }} display="flex" gap={1} alignItems="center">
                      <Chip size="small" label={log.action} />
                      <Typography variant="body2" noWrap title={full}>
                        {summarize(full)}
                      </Typography>
                      <span>· {new Date(log.createdAt).toLocaleString()}</span>
                    </Box>
                  );
                })}
              </Stack>
            </Box>
          ))}
          {filteredItems.length === 0 && (
            <Typography variant="body2" color="text.secondary">
              Recent activity will appear here
            </Typography>
          )}
          {filteredItems.length > visibleCount && (
            <Button size="small" onClick={() => setVisibleCount((v) => v + 20)} sx={{ mt: 1 }}>
              Show more
            </Button>
          )}
          {hasMore && filteredItems.length >= visibleCount && (
            <Button
              size="small"
              variant="text"
              onClick={() => {
                setPage((p) => p + 1);
                setVisibleCount((v) => v + pageSize);
              }}
              sx={{ mt: 1 }}
            >
              Load next page
            </Button>
          )}
        </Stack>
      </Paper>
    </Stack>
  );
}
