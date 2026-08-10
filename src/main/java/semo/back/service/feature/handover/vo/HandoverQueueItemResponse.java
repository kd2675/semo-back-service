package semo.back.service.feature.handover.vo;

import java.time.LocalDateTime;

public record HandoverQueueItemResponse(
        String resourceType,
        Long resourceId,
        String title,
        String statusLabel,
        LocalDateTime dueAt,
        String targetPath,
        boolean urgent
) {
}
