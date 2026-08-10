package semo.back.service.feature.handover.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HandoverRecentDecisionResponse(
        Long decisionRecordId,
        String recordType,
        String title,
        LocalDate effectiveDate,
        LocalDateTime confirmedAt,
        String targetPath
) {
}
