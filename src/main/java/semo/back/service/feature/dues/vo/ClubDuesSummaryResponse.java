package semo.back.service.feature.dues.vo;

import java.math.BigDecimal;

public record ClubDuesSummaryResponse(
        Long invoiceId,
        Long clubProfileId,
        String memberDisplayName,
        String memberRoleCode,
        int billingYear,
        int billingMonth,
        String billingMonthLabel,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String paymentStatus,
        String paymentStatusLabel,
        boolean overdue,
        String dueAt,
        String dueAtLabel,
        String paidAt,
        String paidAtLabel,
        String note
) {
}
