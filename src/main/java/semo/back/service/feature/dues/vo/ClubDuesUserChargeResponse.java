package semo.back.service.feature.dues.vo;

import java.math.BigDecimal;

public record ClubDuesUserChargeResponse(
        Long chargeId,
        String title,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String dueAt,
        String dueAtLabel,
        String issuedAt,
        String issuedAtLabel,
        String note,
        ClubDuesInvoiceResponse invoice
) {
}
