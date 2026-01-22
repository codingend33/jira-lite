import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography
} from "@mui/material";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ErrorBanner from "../components/ErrorBanner";
import Loading from "../components/Loading";
import { useProjects } from "../query/projectQueries";
import { useOrgMembers } from "../query/memberQueries";
import { useCreateTicket, useTicket, useUpdateTicket } from "../query/ticketQueries";
import { useNotify } from "../components/Notifications";

const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"];

type Props = {
  mode: "create" | "edit";
};

export default function TicketFormPage({ mode }: Props) {
  const navigate = useNavigate();
  const params = useParams();
  const ticketId = params.ticketId ?? "";

  const projectsQuery = useProjects();
  const membersQuery = useOrgMembers();
  const ticketQuery = useTicket(ticketId);
  const createTicket = useCreateTicket();
  const updateTicket = useUpdateTicket();
  const { notifySuccess } = useNotify();

  const [form, setForm] = useState({
    projectId: "",
    title: "",
    description: "",
    priority: "MEDIUM",
    assigneeId: ""
  });

  useEffect(() => {
    if (mode === "edit" && ticketQuery.data) {
      setForm({
        projectId: ticketQuery.data.projectId,
        title: ticketQuery.data.title,
        description: ticketQuery.data.description ?? "",
        priority: ticketQuery.data.priority,
        assigneeId: ticketQuery.data.assigneeId ?? ""
      });
    }
  }, [mode, ticketQuery.data]);

  const handleSubmit = async () => {
    if (mode === "create") {
      const created = await createTicket.mutateAsync({
        projectId: form.projectId,
        title: form.title,
        description: form.description || undefined,
        priority: form.priority,
        assigneeId: form.assigneeId || undefined
      });
      notifySuccess("Ticket created");
      navigate(`/tickets/${created.id}`);
      return;
    }

    if (!ticketId) {
      return;
    }

    await updateTicket.mutateAsync({
      id: ticketId,
      payload: {
        title: form.title,
        description: form.description || undefined,
        priority: form.priority,
        assigneeId: form.assigneeId || undefined
      }
    });
    notifySuccess("Ticket updated");
    navigate(`/tickets/${ticketId}`);
  };

  if (mode === "edit" && ticketQuery.isLoading) {
    return <Loading />;
  }

  return (
    <Stack spacing={3}>
      <ErrorBanner
        error={
          ticketQuery.error ??
          createTicket.error ??
          updateTicket.error ??
          membersQuery.error ??
          projectsQuery.error
        }
      />
      <Typography variant="h4" sx={{ fontWeight: 700 }}>
        {mode === "create" ? "Create Ticket" : "Edit Ticket"}
      </Typography>

      <Card variant="outlined">
        <CardContent sx={{ display: "grid", gap: 2 }}>
          <FormControl fullWidth>
            <InputLabel>Project</InputLabel>
            <Select
              label="Project"
              value={form.projectId}
              onChange={(event) => setForm({ ...form, projectId: event.target.value })}
              disabled={mode === "edit"}
            >
              {projectsQuery.data?.map((project) => (
                <MenuItem key={project.id} value={project.id}>
                  {project.key} - {project.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            label="Title"
            value={form.title}
            onChange={(event) => setForm({ ...form, title: event.target.value })}
          />
          <TextField
            label="Description"
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            multiline
            minRows={4}
          />
          <Box sx={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 2 }}>
            <FormControl fullWidth>
              <InputLabel>Priority</InputLabel>
              <Select
                label="Priority"
                value={form.priority}
                onChange={(event) => setForm({ ...form, priority: event.target.value })}
              >
                {PRIORITIES.map((priority) => (
                  <MenuItem key={priority} value={priority}>
                    {priority}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth disabled={membersQuery.isLoading}>
              <InputLabel>Assignee</InputLabel>
              <Select
                label="Assignee"
                value={form.assigneeId}
                onChange={(event) => setForm({ ...form, assigneeId: event.target.value })}
              >
                <MenuItem value="">Unassigned</MenuItem>
                {membersQuery.data?.map((member) => (
                  <MenuItem key={member.userId} value={member.userId}>
                    {member.displayName || member.email || member.userId}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </CardContent>
      </Card>

      <Box sx={{ display: "flex", gap: 2 }}>
        <Button variant="outlined" onClick={() => navigate(-1)}>
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={!form.title || !form.projectId}
        >
          {mode === "create" ? "Create" : "Save"}
        </Button>
      </Box>
    </Stack>
  );
}
