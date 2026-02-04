import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ErrorBanner from "../components/ErrorBanner";
import Loading from "../components/Loading";
import { useNotify } from "../components/Notifications";
import { useComments, useCreateComment } from "../query/commentQueries";
import {
  useAttachments,
  useDeleteAttachment,
  useDownloadAttachment,
  useUploadAttachment
} from "../query/attachmentQueries";
import { useTicket, useTransitionTicket } from "../query/ticketQueries";
import { useOrgMembers } from "../query/memberQueries";
import { useProjects } from "../query/projectQueries";
import { useSoftDeleteTicket } from "../query/trashQueries";
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle
} from "@mui/material";

const STATUSES = ["OPEN", "IN_PROGRESS", "DONE", "CANCELLED"];

export default function TicketDetailPage() {
  const params = useParams();
  const ticketId = params.ticketId ?? "";
  const navigate = useNavigate();
  const { notifySuccess } = useNotify();

  const ticketQuery = useTicket(ticketId);
  const transitionTicket = useTransitionTicket();
  const commentsQuery = useComments(ticketId);
  const createComment = useCreateComment(ticketId);
  const attachmentsQuery = useAttachments(ticketId);
  const uploadAttachment = useUploadAttachment(ticketId);
  const downloadAttachment = useDownloadAttachment(ticketId);
  const deleteAttachment = useDeleteAttachment(ticketId);
  const membersQuery = useOrgMembers();
  const projectsQuery = useProjects();
  const softDeleteTicket = useSoftDeleteTicket();

  const [commentBody, setCommentBody] = useState("");
  const [nextStatus, setNextStatus] = useState("OPEN");
  const [confirmDelete, setConfirmDelete] = useState(false);

  const handleDeleteTicket = async () => {
    await softDeleteTicket.mutateAsync({ id: ticketId });
    notifySuccess("Ticket moved to trash");
    navigate("/tickets");
  };


  const handleAddComment = async () => {
    if (!commentBody.trim()) {
      return;
    }
    await createComment.mutateAsync(commentBody);
    setCommentBody("");
    notifySuccess("Comment posted");
  };

  const handleTransition = async () => {
    await transitionTicket.mutateAsync({ id: ticketId, status: nextStatus });
    notifySuccess("Status updated");
  };

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    await uploadAttachment.mutateAsync(file);
    notifySuccess("Attachment uploaded");
  };

  const handleDownload = async (attachmentId: string) => {
    const response = await downloadAttachment.mutateAsync(attachmentId);
    window.open(response.downloadUrl, "_blank", "noopener,noreferrer");
  };

  const handleDelete = async (attachmentId: string) => {
    await deleteAttachment.mutateAsync(attachmentId);
    notifySuccess("Attachment deleted");
  };

  useEffect(() => {
    if (ticketQuery.data?.status) {
      setNextStatus(ticketQuery.data.status);
    }
  }, [ticketQuery.data?.status]);

  const memberLookup = useMemo(() => {
    const map = new Map<string, string>();
    for (const member of membersQuery.data ?? []) {
      const label = member.displayName || member.email || member.userId;
      map.set(member.userId, label);
    }
    return map;
  }, [membersQuery.data]);
  const projectLookup = useMemo(() => {
    const map = new Map<string, { key: string; name: string }>();
    for (const proj of projectsQuery.data ?? []) {
      map.set(proj.id, { key: proj.key, name: proj.name });
    }
    return map;
  }, [projectsQuery.data]);

  if (ticketQuery.isLoading) {
    return <Loading />;
  }

  const ticket = ticketQuery.data;

  if (!ticket) {
    return null;
  }

  return (
    <Stack spacing={3}>
      <ErrorBanner
        error={
          ticketQuery.error ??
          commentsQuery.error ??
          createComment.error ??
          attachmentsQuery.error ??
          uploadAttachment.error ??
          downloadAttachment.error ??
          deleteAttachment.error ??
          membersQuery.error ??
          transitionTicket.error
        }
      />
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          {ticket.key}
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" onClick={() => navigate(`/tickets/${ticketId}/edit`)}>
            Edit Ticket
          </Button>
          <Button variant="outlined" color="error" onClick={() => setConfirmDelete(true)}>
            Delete
          </Button>
        </Stack>
      </Box>

      <Card variant="outlined">
        <CardContent sx={{ display: "grid", gap: 1.5 }}>
          <Typography variant="h6">{ticket.title}</Typography>
          <Typography variant="body2" color="text.secondary">
            {ticket.description || "No description"}
          </Typography>
          <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
            <Chip label={ticket.status} color="primary" />
            <Chip label={ticket.priority} />
            <Chip
              label={`Project ${projectLookup.get(ticket.projectId)?.key ?? projectLookup.get(ticket.projectId)?.name ?? ticket.projectId.slice(0, 8)
                }`}
            />
            <Chip
              label={
                ticket.assigneeId
                  ? `Assignee ${memberLookup.get(ticket.assigneeId) ?? ticket.assigneeId}`
                  : "Assignee Unassigned"
              }
            />
            <Chip
              label={`Creator ${memberLookup.get(ticket.createdBy ?? "") ?? ticket.createdBy ?? "Unknown"}`}
              variant="filled"
              color="default"
            />
          </Box>
          <Divider />
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel>Next Status</InputLabel>
              <Select
                label="Next Status"
                value={nextStatus}
                onChange={(event) => setNextStatus(event.target.value)}
              >
                {STATUSES.map((status) => (
                  <MenuItem key={status} value={status}>
                    {status}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button variant="contained" onClick={handleTransition}>
              Transition
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent sx={{ display: "grid", gap: 2 }}>
          <Typography variant="h6">Comments</Typography>
          <Stack spacing={1}>
            {commentsQuery.data?.map((comment) => (
              <Box key={comment.id} sx={{ p: 2, border: "1px solid #e5e7eb", borderRadius: 1 }}>
                <Typography variant="body2" color="text.secondary">
                  {memberLookup.get(comment.authorId ?? "") ??
                    comment.authorId ??
                    "Unknown author"}{" "}
                  · {new Date(comment.createdAt).toLocaleString()}
                </Typography>
                <Typography variant="body1">{comment.body}</Typography>
              </Box>
            ))}
          </Stack>
          <TextField
            label="Add comment"
            value={commentBody}
            onChange={(event) => setCommentBody(event.target.value)}
            multiline
            minRows={3}
          />
          <Button variant="contained" onClick={handleAddComment}>
            Post Comment
          </Button>
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent sx={{ display: "grid", gap: 2 }}>
          <Typography variant="h6">Attachments</Typography>
          <Stack spacing={1}>
            {attachmentsQuery.data?.map((attachment) => (
              <Box key={attachment.id} sx={{ display: "flex", justifyContent: "space-between" }}>
                <Box>
                  <Typography variant="body2">{attachment.fileName}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {attachment.contentType} · {Math.max(1, Math.round(attachment.fileSize / 1024))} KB
                  </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                  <Button size="small" onClick={() => handleDownload(attachment.id)}>
                    Download
                  </Button>
                  <Button
                    size="small"
                    color="error"
                    aria-label={`Delete attachment ${attachment.fileName}`}
                    onClick={() => handleDelete(attachment.id)}
                    disabled={deleteAttachment.isPending}
                  >
                    Delete
                  </Button>
                </Stack>
              </Box>
            ))}
          </Stack>
          <Button variant="outlined" component="label">
            Upload file
            <input type="file" hidden onChange={handleUpload} />
          </Button>
        </CardContent>
      </Card>
      <Dialog open={confirmDelete} onClose={() => setConfirmDelete(false)}>
        <DialogTitle>Move ticket to trash?</DialogTitle>
        <DialogContent>
          <Typography>
            {"Ticket \"" + ticket.key + "\" will be moved to trash."}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Items in trash are automatically deleted after 30 days. You can restore them before then.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(false)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={handleDeleteTicket} disabled={softDeleteTicket.isPending}>
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
