package semo.back.service.feature.dues.vo;

import java.math.BigDecimal;

public record ClubDuesInvoiceResponse(
        Long invoiceId,
        Long clubProfileId,
        String memberDisplayName,
        String memberRoleCode,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String paymentStatus,
        String paymentStatusLabel,
        boolean overdue,
        String paidAt,
        String paidAtLabel,
        String note
) {
}
