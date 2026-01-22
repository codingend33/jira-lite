import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography
} from "@mui/material";
import { useState } from "react";
import ErrorBanner from "../components/ErrorBanner";
import Loading from "../components/Loading";
import {
  useArchiveProject,
  useCreateProject,
  useDeleteProject,
  useProjects,
  useUnarchiveProject
} from "../query/projectQueries";
import { useNavigate } from "react-router-dom";

export default function ProjectsPage() {
  const navigate = useNavigate();
  const projectsQuery = useProjects();
  const createProject = useCreateProject();
  const archiveProject = useArchiveProject();
  const unarchiveProject = useUnarchiveProject();
  const deleteProject = useDeleteProject();

  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ key: "", name: "", description: "" });

  const handleCreate = async () => {
    await createProject.mutateAsync({
      key: form.key,
      name: form.name,
      description: form.description || undefined
    });
    setForm({ key: "", name: "", description: "" });
    setOpen(false);
  };

  if (projectsQuery.isLoading) {
    return <Loading />;
  }

  return (
    <Stack spacing={3}>
      <ErrorBanner error={projectsQuery.error} />
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Projects
        </Typography>
        <Button variant="contained" onClick={() => setOpen(true)}>
          New Project
        </Button>
      </Box>

      <Stack spacing={2}>
        {projectsQuery.data?.map((project) => (
          <Card
            key={project.id}
            variant="outlined"
            onClick={() => navigate(`/projects/${project.id}`)}
            sx={{ cursor: "pointer" }}
          >
            <CardContent sx={{ display: "grid", gap: 1.5 }}>
              <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                <Typography variant="h6">
                  {project.key} - {project.name}
                </Typography>
                <Chip label={project.status} color={project.status === "ARCHIVED" ? "default" : "success"} />
              </Box>
              <Typography variant="body2" color="text.secondary">
                {project.description || "No description"}
              </Typography>
              <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                {project.status === "ARCHIVED" ? (
                  <Button
                    size="small"
                    onClick={(event) => {
                      event.stopPropagation();
                      unarchiveProject.mutate(project.id);
                    }}
                  >
                    Unarchive
                  </Button>
                ) : (
                  <Button
                    size="small"
                    onClick={(event) => {
                      event.stopPropagation();
                      archiveProject.mutate(project.id);
                    }}
                  >
                    Archive
                  </Button>
                )}
                <Button
                  size="small"
                  color="error"
                  onClick={(event) => {
                    event.stopPropagation();
                    deleteProject.mutate(project.id);
                  }}
                >
                  Delete
                </Button>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Stack>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth>
        <DialogTitle>Create Project</DialogTitle>
        <DialogContent sx={{ display: "grid", gap: 2, pt: 1 }}>
          <TextField
            label="Key"
            value={form.key}
            onChange={(event) => setForm({ ...form, key: event.target.value.toUpperCase() })}
            helperText="Example: OPS"
          />
          <TextField
            label="Name"
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
          />
          <TextField
            label="Description"
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            multiline
            minRows={3}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={!form.key || !form.name || createProject.isPending}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
