package com.jiralite.backend.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        PageMeta page
) {
}
