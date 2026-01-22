import { Box, IconButton, Typography } from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";

export default function PaginationControls({
  page,
  totalPages,
  onChange
}: {
  page: number;
  totalPages: number;
  onChange: (next: number) => void;
}) {
  const canPrev = page > 0;
  const canNext = page + 1 < totalPages;

  return (
    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
      <IconButton onClick={() => onChange(page - 1)} disabled={!canPrev}>
        <ArrowBackIcon />
      </IconButton>
      <Typography variant="body2">
        Page {page + 1} / {Math.max(totalPages, 1)}
      </Typography>
      <IconButton onClick={() => onChange(page + 1)} disabled={!canNext}>
        <ArrowForwardIcon />
      </IconButton>
    </Box>
  );
}
