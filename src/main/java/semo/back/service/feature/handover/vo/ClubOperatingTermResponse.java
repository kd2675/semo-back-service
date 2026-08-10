package semo.back.service.feature.handover.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClubOperatingTermResponse(
        Long clubOperatingTermId,
        String termName,
        String termType,
        LocalDate startDate,
        LocalDate endDate,
        String statusCode,
        String description,
        LocalDateTime activatedAt,
        LocalDateTime closedAt
) {
}
