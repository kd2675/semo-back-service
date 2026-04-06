package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record ClubFinanceRequestResponse(
        Long requestId,
        String requestTypeCode,
        String requestTypeLabel,
        String requesterDisplayName,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String title,
        String relatedEventName,
        String note,
        String statusCode,
        String statusLabel,
        String submittedAt,
        String submittedAtLabel,
        String reviewedAt,
        String reviewedAtLabel,
        String reviewNote
) {
}
