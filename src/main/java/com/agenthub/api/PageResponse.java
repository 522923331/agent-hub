package com.agenthub.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 稳定的分页响应结构：
 * 避免直接序列化 PageImpl 造成的 JSON 结构不稳定告警与潜在升级风险。
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        int numberOfElements,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.getNumberOfElements(),
                p.isFirst(),
                p.isLast()
        );
    }
}


