package com.example.orderservice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wrapper uniforme per tutte le risposte paginate.
 * Evita di esporre direttamente il tipo Page di Spring
 * nel contratto API pubblico.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}