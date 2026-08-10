package semo.back.service.feature.handover.vo;

import java.time.LocalDateTime;

public record ClubTermCarryoverItemResponse(
        Long clubTermCarryoverItemId,
        Long fromTermId,
        String fromTermName,
        Long toTermId,
        String toTermName,
        String resourceType,
        Long resourceId,
        String title,
        String statusSnapshot,
        String targetPath,
        LocalDateTime dueAt,
        String statusCode,
        LocalDateTime transferredAt,
        LocalDateTime resolvedAt
) {
}
