package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record ClubFinancePaymentResponse(
        Long paymentId,
        Long clubProfileId,
        String memberDisplayName,
        String memberRoleCode,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String paymentStatusCode,
        String paymentStatusLabel,
        boolean overdue,
        String paidAt,
        String paidAtLabel,
        String note
) {
}
