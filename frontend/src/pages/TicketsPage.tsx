import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  FormControl,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Typography
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import ErrorBanner from "../components/ErrorBanner";
import Loading from "../components/Loading";
import PaginationControls from "../components/PaginationControls";
import { useProjects } from "../query/projectQueries";
import { useTickets, useSearchTickets } from "../query/ticketQueries";
import { useOrgMembers } from "../query/memberQueries";

const STATUSES = ["OPEN", "IN_PROGRESS", "DONE", "CANCELLED"];
const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"];

export default function TicketsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialProjectId = searchParams.get("projectId") ?? "";
  const keyword = searchParams.get("keyword") ?? "";
  const [filters, setFilters] = useState({
    status: "",
    priority: "",
    projectId: initialProjectId,
    page: 0,
    size: 10,
    sort: "createdAt,desc"
  });

  const projectsQuery = useProjects();
  const membersQuery = useOrgMembers();
  const searchQuery = useSearchTickets(keyword);
  const listQuery = useTickets({
    status: filters.status || undefined,
    priority: filters.priority || undefined,
    projectId: filters.projectId || undefined,
    page: filters.page,
    size: filters.size,
    sort: filters.sort
  });

  const isSearching = keyword.trim().length > 0;
  const tickets = isSearching
    ? (searchQuery.data as any[] | undefined) ?? []
    : (listQuery.data?.content ?? []);

  const projectOptions = useMemo(() => projectsQuery.data ?? [], [projectsQuery.data]);
  const memberLookup = useMemo(() => {
    const map = new Map<string, string>();
    for (const member of membersQuery.data ?? []) {
      const label = member.displayName || member.email || member.userId;
      map.set(member.userId, label);
    }
    return map;
  }, [membersQuery.data]);

  useEffect(() => {
    const projectId = searchParams.get("projectId");
    if (projectId && projectId !== filters.projectId) {
      setFilters({ ...filters, projectId, page: 0 });
    }
  }, [filters, searchParams]);

  if (isSearching ? searchQuery.isLoading : listQuery.isLoading) {
    return <Loading />;
  }

  return (
    <Stack spacing={3}>
      <ErrorBanner error={(isSearching ? searchQuery.error : listQuery.error) ?? projectsQuery.error ?? membersQuery.error} />
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Tickets
        </Typography>
        <Button variant="contained" onClick={() => navigate("/tickets/new")}
        >
          New Ticket
        </Button>
      </Box>

      <Card variant="outlined">
        <CardContent sx={{ display: "grid", gap: 2 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Filters
          </Typography>
          <Box sx={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 2 }}>
            <FormControl fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={filters.status}
                onChange={(event) => setFilters({ ...filters, status: event.target.value, page: 0 })}
              >
                <MenuItem value="">All</MenuItem>
                {STATUSES.map((status) => (
                  <MenuItem key={status} value={status}>
                    {status}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Priority</InputLabel>
              <Select
                label="Priority"
                value={filters.priority}
                onChange={(event) => setFilters({ ...filters, priority: event.target.value, page: 0 })}
              >
                <MenuItem value="">All</MenuItem>
                {PRIORITIES.map((priority) => (
                  <MenuItem key={priority} value={priority}>
                    {priority}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Project</InputLabel>
              <Select
                label="Project"
                value={filters.projectId}
                onChange={(event) => setFilters({ ...filters, projectId: event.target.value, page: 0 })}
              >
                <MenuItem value="">All</MenuItem>
                {projectOptions.map((project) => (
                  <MenuItem key={project.id} value={project.id}>
                    {project.key}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Sort</InputLabel>
              <Select
                label="Sort"
                value={filters.sort}
                onChange={(event) => setFilters({ ...filters, sort: event.target.value, page: 0 })}
              >
                <MenuItem value="createdAt,desc">Created (newest)</MenuItem>
                <MenuItem value="createdAt,asc">Created (oldest)</MenuItem>
                <MenuItem value="updatedAt,desc">Updated (newest)</MenuItem>
                <MenuItem value="updatedAt,asc">Updated (oldest)</MenuItem>
                <MenuItem value="priority,desc">Priority (high → low)</MenuItem>
                <MenuItem value="priority,asc">Priority (low → high)</MenuItem>
              </Select>
              <FormHelperText>Choose sort order</FormHelperText>
            </FormControl>
          </Box>
        </CardContent>
      </Card>

      <Stack spacing={2}>
        {tickets.map((ticket) => (
          <Card key={ticket.id} variant="outlined" onClick={() => navigate(`/tickets/${ticket.id}`)}
          sx={{ cursor: "pointer" }}>
            <CardContent sx={{ display: "grid", gap: 1 }}>
              <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                <Typography variant="h6">
                  {ticket.key} - {ticket.title}
                </Typography>
                <Chip label={ticket.status} color="primary" variant="outlined" />
              </Box>
              <Typography variant="body2" color="text.secondary">
                {ticket.description || "No description"}
              </Typography>
              <Box sx={{ display: "flex", gap: 1 }}>
                <Chip size="small" label={ticket.priority} />
                <Chip size="small" label={`Project ${ticket.projectId.slice(0, 8)}`} />
                <Chip
                  size="small"
                  label={`Assignee ${ticket.assigneeId ? memberLookup.get(ticket.assigneeId) ?? ticket.assigneeId : "Unassigned"}`}
                />
              </Box>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {!isSearching && (
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <PaginationControls
            page={listQuery.data?.page.number ?? 0}
            totalPages={listQuery.data?.page.totalPages ?? 1}
            onChange={(next) => setFilters({ ...filters, page: next })}
          />
          <FormControl size="small">
            <Select
              value={filters.size}
              onChange={(event) => setFilters({ ...filters, size: Number(event.target.value), page: 0 })}
            >
              {[5, 10, 20].map((size) => (
                <MenuItem key={size} value={size}>
                  {size} / page
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      )}
    </Stack>
  );
}
