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
import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ErrorBanner from "../components/ErrorBanner";
import Loading from "../components/Loading";
import PaginationControls from "../components/PaginationControls";
import { useProject } from "../query/projectQueries";
import { useTickets } from "../query/ticketQueries";
import { useOrgMembers } from "../query/memberQueries";

const STATUSES = ["OPEN", "IN_PROGRESS", "DONE", "CANCELLED"];
const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const SORT_OPTIONS = [
  { value: "createdAt,desc", label: "Created (newest)" },
  { value: "createdAt,asc", label: "Created (oldest)" },
  { value: "updatedAt,desc", label: "Updated (newest)" },
  { value: "updatedAt,asc", label: "Updated (oldest)" },
  { value: "priority,desc", label: "Priority (high → low)" },
  { value: "priority,asc", label: "Priority (low → high)" }
];

export default function ProjectDetailPage() {
  const params = useParams();
  const projectId = params.projectId ?? "";
  const navigate = useNavigate();

  const projectQuery = useProject(projectId);
  const membersQuery = useOrgMembers();
  const [filters, setFilters] = useState({
    status: "",
    priority: "",
    page: 0,
    size: 10,
    sort: "createdAt,desc"
  });

  const ticketsQuery = useTickets({
    status: filters.status || undefined,
    priority: filters.priority || undefined,
    projectId,
    page: filters.page,
    size: filters.size,
    sort: filters.sort
  });

  const memberLookup = useMemo(() => {
    const map = new Map<string, string>();
    for (const member of membersQuery.data ?? []) {
      const label = member.displayName || member.email || member.userId;
      map.set(member.userId, label);
    }
    return map;
  }, [membersQuery.data]);

  if (projectQuery.isLoading) {
    return <Loading />;
  }

  const project = projectQuery.data;

  if (!project) {
    return null;
  }

  return (
    <Stack spacing={3}>
      <ErrorBanner error={projectQuery.error ?? ticketsQuery.error ?? membersQuery.error} />
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            {project.key} - {project.name}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {project.description || "No description"}
          </Typography>
        </Box>
        <Box sx={{ display: "flex", gap: 1 }}>
          <Button variant="outlined" onClick={() => navigate("/projects")}>
            Back to Projects
          </Button>
          <Button variant="contained" onClick={() => navigate("/tickets/new")}>
            New Ticket
          </Button>
        </Box>
      </Box>

      <Card variant="outlined">
        <CardContent sx={{ display: "flex", gap: 1, alignItems: "center", flexWrap: "wrap" }}>
          <Chip label={project.status} color={project.status === "ARCHIVED" ? "default" : "success"} />
          <Chip label={`Created ${new Date(project.createdAt).toLocaleDateString()}`} />
          {project.createdBy && (
            <Chip
              label={`Creator ${memberLookup.get(project.createdBy) ?? project.createdBy}`}
              color="info"
              variant="outlined"
            />
          )}
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent sx={{ display: "grid", gap: 2 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Tickets
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
              <InputLabel>Sort</InputLabel>
              <Select
                label="Sort"
                value={filters.sort}
                onChange={(event) => setFilters({ ...filters, sort: event.target.value, page: 0 })}
              >
                {SORT_OPTIONS.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
              <FormHelperText>Choose sort order</FormHelperText>
            </FormControl>
          </Box>
        </CardContent>
      </Card>

      <Stack spacing={2}>
        {ticketsQuery.data?.content.map((ticket) => (
          <Card
            key={ticket.id}
            variant="outlined"
            onClick={() => navigate(`/tickets/${ticket.id}`)}
            sx={{ cursor: "pointer" }}
          >
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
              <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                <Chip size="small" label={ticket.priority} />
                <Chip
                  size="small"
                  label={`Assignee ${ticket.assigneeId ? memberLookup.get(ticket.assigneeId) ?? ticket.assigneeId : "Unassigned"}`}
                />
                {ticket.createdBy && (
                  <Chip
                    size="small"
                    color="info"
                    variant="outlined"
                    label={`Creator ${memberLookup.get(ticket.createdBy) ?? ticket.createdBy}`}
                  />
                )}
              </Box>
            </CardContent>
          </Card>
        ))}
      </Stack>

      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <PaginationControls
          page={ticketsQuery.data?.page.number ?? 0}
          totalPages={ticketsQuery.data?.page.totalPages ?? 1}
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
    </Stack>
  );
}
