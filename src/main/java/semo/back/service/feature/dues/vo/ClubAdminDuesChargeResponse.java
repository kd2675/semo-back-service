package semo.back.service.feature.dues.vo;

import java.math.BigDecimal;
public record ClubAdminDuesChargeResponse(
        Long chargeId,
        String title,
        String targetScope,
        String targetScopeLabel,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String dueAt,
        String dueAtLabel,
        String issuedAt,
        String issuedAtLabel,
        String issuedByDisplayName,
        String note,
        boolean canDelete,
        int totalInvoiceCount,
        int pendingInvoiceCount,
        int paidInvoiceCount,
        int waivedInvoiceCount,
        int overdueInvoiceCount,
        int collectionRate
) {
}
