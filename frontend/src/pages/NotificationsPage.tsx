import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, Container, List, ListItem, ListItemText, Stack, Typography, Button, Pagination } from "@mui/material";
import { useState } from "react";
import { useNotify } from "../components/Notifications";
import { useNavigate } from "react-router-dom";
import { listNotifications, markNotificationRead, Notification } from "../api/notifications";
import { PagedResponse } from "../api/types";

export default function NotificationsPage() {
  const { notifySuccess } = useNotify();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const pageSize = 10;

  const query = useQuery<PagedResponse<Notification>>({
    queryKey: ["notifications", page] as const,
    queryFn: () => listNotifications({ page: page - 1, size: pageSize })
  });

  const markRead = useMutation({
    mutationFn: (id: string) => markNotificationRead(id),
    onSuccess: (_data, id) => {
      queryClient.setQueryData<PagedResponse<Notification> | undefined>(["notifications", page], (prev) => {
        if (!prev?.content) return prev;
        return {
          ...prev,
          content: prev.content.map((n) => (n.id === id ? { ...n, read: true, isRead: true } : n))
        };
      });
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      notifySuccess("Marked as read");
    }
  });

  const data = query.data?.content ?? [];
  const pageCount = query.data?.page?.totalPages ?? 1;

  return (
    <Container sx={{ maxWidth: 720, py: 3 }}>
      <Stack spacing={2}>
        <Typography variant="h5" fontWeight={700}>Notifications</Typography>
        <Card>
          <CardContent>
            <List>
              {data.map((n) => (
                <ListItem
                  key={n.id}
                  divider
                  secondaryAction={
                    !n.read && !n.isRead ? (
                      <Button size="small" onClick={() => markRead.mutate(n.id)} disabled={markRead.isPending}>
                        Mark read
                      </Button>
                    ) : null
                  }
                  onClick={() => navigate(-1)}
                >
                  <ListItemText
                    primary={n.content}
                    secondary={new Date(n.createdAt).toLocaleString()}
                    primaryTypographyProps={{ fontWeight: n.read || n.isRead ? 400 : 700 }}
                  />
                </ListItem>
              ))}
              {data.length === 0 && (
                <ListItem>
                  <ListItemText primary="No notifications" />
                </ListItem>
              )}
            </List>
            {pageCount > 1 && (
              <Pagination
                sx={{ mt: 2 }}
                count={pageCount}
                page={page}
                onChange={(_e, value) => setPage(value)}
              />
            )}
          </CardContent>
        </Card>
      </Stack>
    </Container>
  );
}

