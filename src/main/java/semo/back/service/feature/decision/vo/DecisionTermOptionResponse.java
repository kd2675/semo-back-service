package semo.back.service.feature.decision.vo;

import java.time.LocalDate;

public record DecisionTermOptionResponse(
        Long clubOperatingTermId,
        String termName,
        String statusCode,
        LocalDate startDate,
        LocalDate endDate
) {
}
