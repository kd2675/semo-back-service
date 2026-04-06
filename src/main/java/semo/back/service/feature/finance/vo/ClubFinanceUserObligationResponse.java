package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record ClubFinanceUserObligationResponse(
        Long obligationId,
        String obligationTypeCode,
        String obligationTypeLabel,
        String title,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String dueAt,
        String dueAtLabel,
        String issuedAt,
        String issuedAtLabel,
        String note,
        ClubFinancePaymentResponse payment
) {
}
