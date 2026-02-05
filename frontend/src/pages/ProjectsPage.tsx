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
  Tooltip,
  Typography
} from "@mui/material";
import { useMemo, useState } from "react";
import ErrorBanner from "../components/ErrorBanner";
import Loading from "../components/Loading";
import { useAuth } from "../auth/AuthContext";
import { useNotify } from "../components/Notifications";
import {
  useArchiveProject,
  useCreateProject,
  useDeleteProject,
  useProjects,
  useUnarchiveProject
} from "../query/projectQueries";
import { useOrgMembers } from "../query/memberQueries";
import { useNavigate } from "react-router-dom";

export default function ProjectsPage() {
  const navigate = useNavigate();
  const { state: authState } = useAuth();
  const isAdmin = authState.profile?.["cognito:groups"]?.some((g: string) => g.toUpperCase() === "ADMIN");
  const projectsQuery = useProjects();
  const createProject = useCreateProject();
  const archiveProject = useArchiveProject();
  const unarchiveProject = useUnarchiveProject();
  const deleteProject = useDeleteProject();
  const membersQuery = useOrgMembers();
  const { notifyError } = useNotify();

  const memberLookup = useMemo(() => {
    const map = new Map<string, string>();
    for (const member of membersQuery.data ?? []) {
      const label = member.displayName || member.email || member.userId;
      map.set(member.userId, label);
    }
    return map;
  }, [membersQuery.data]);

  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ key: "", name: "", description: "" });
  const [confirmDelete, setConfirmDelete] = useState<{ id: string; key: string } | null>(null);

  const handleCreate = async () => {
    await createProject.mutateAsync({
      key: form.key,
      name: form.name,
      description: form.description || undefined
    });
    setForm({ key: "", name: "", description: "" });
    setOpen(false);
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await deleteProject.mutateAsync(confirmDelete.id);
      setConfirmDelete(null);
    } catch {
      // ErrorBanner will show the error
    }
  };

  if (projectsQuery.isLoading) {
    return <Loading />;
  }

  const mutationError =
    projectsQuery.error ||
    createProject.error ||
    archiveProject.error ||
    unarchiveProject.error ||
    deleteProject.error ||
    membersQuery.error;

  return (
    <Stack spacing={3}>
      <ErrorBanner error={mutationError} />
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Projects
          </Typography>
        <Box sx={{ display: "flex", gap: 1 }}>
          <Button
            variant="contained"
            disabled={!isAdmin}
            onClick={() => (isAdmin ? setOpen(true) : notifyError("No permission"))}
          >
            New Project
          </Button>
        </Box>
      </Box>

      <Stack spacing={2}>
        {projectsQuery.data?.map((project) => (
          <Card
            key={project.id}
            variant="outlined"
            onClick={() => navigate(`/projects/${project.id}`)}
            sx={{ cursor: "pointer" }}
          >
            <CardContent sx={{ display: "grid", gap: 1.25 }}>
              <Box sx={{ display: "flex", justifyContent: "space-between" }}>
                <Typography variant="h6">
                  {project.key} - {project.name}
                </Typography>
                <Chip label={project.status} color={project.status === "ARCHIVED" ? "default" : "success"} />
              </Box>
              <Typography variant="body2" color="text.secondary">
                {project.description || "No description"}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Creator: {memberLookup.get(project.createdBy ?? "") ?? project.createdBy ?? "Unknown"}
              </Typography>
              <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap", alignItems: "center", pt: 0.5, pl: 0 }}>
                {project.status === "ARCHIVED" ? (
                  <Button
                    size="small"
                    onClick={(event) => {
                      event.stopPropagation();
                      if (!isAdmin) return notifyError("No permission");
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
                      if (!isAdmin) return notifyError("No permission");
                      archiveProject.mutate(project.id);
                    }}
                  >
                    Archive
                  </Button>
                )}
                <Tooltip
                  title={
                    !isAdmin
                      ? "Admins only"
                      : project.status !== "ARCHIVED"
                      ? "Archive project before deleting"
                      : "Move to trash"
                  }
                  arrow
                >
                  <span>
                    <Button
                      size="small"
                      color="error"
                      disabled={project.status !== "ARCHIVED"}
                      onClick={(event) => {
                        event.stopPropagation();
                        if (!isAdmin) return notifyError("No permission");
                        setConfirmDelete({ id: project.id, key: project.key });
                      }}
                    >
                      Delete
                    </Button>
                  </span>
                </Tooltip>
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
            fullWidth
            margin="dense"
          />
          <TextField
            label="Name"
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            fullWidth
            margin="dense"
          />
          <TextField
            label="Description"
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            multiline
            minRows={3}
            fullWidth
            margin="dense"
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

      <Dialog open={Boolean(confirmDelete)} onClose={() => setConfirmDelete(null)}>
        <DialogTitle>Move project to trash?</DialogTitle>
        <DialogContent>
          <Typography>
            {confirmDelete ? `Project "${confirmDelete.key}" will be moved to trash.` : ""}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Items in trash are automatically deleted after 30 days. You can restore them before then.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(null)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={handleDelete} disabled={deleteProject.isPending}>
            Confirm Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
