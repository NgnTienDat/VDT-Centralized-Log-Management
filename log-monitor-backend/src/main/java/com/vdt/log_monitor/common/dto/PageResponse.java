package com.vdt.log_monitor.common.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> content;
    int pageNumber;
    int pageSize;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
    boolean empty;

    // public static <T> PageResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
    //     return PageResponse.<T>builder()
    //             .content(page.getContent())
    //             .pageNumber(page.getNumber())
    //             .pageSize(page.getSize())
    //             .totalElements(page.getTotalElements())
    //             .totalPages(page.getTotalPages())
    //             .first(page.isFirst())
    //             .last(page.isLast())
    //             .empty(page.isEmpty())
    //             .build();
    // }

    // public static <T> PageResponse<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
    //     int totalPages = (int) Math.ceil((double) totalElements / pageSize);
    //     boolean first = pageNumber == 0;
    //     boolean last = pageNumber >= totalPages - 1;
    //     boolean empty = content.isEmpty();

    //     return new PageResponse<>(content, pageNumber, pageSize, totalElements, totalPages, first, last, empty);
    // }
}