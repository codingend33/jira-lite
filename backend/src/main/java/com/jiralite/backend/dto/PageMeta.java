package com.jiralite.backend.dto;

public record PageMeta(
        int number,
        int size,
        long totalElements,
        int totalPages
) {
}
