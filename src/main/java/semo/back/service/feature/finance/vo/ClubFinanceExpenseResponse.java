package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record ClubFinanceExpenseResponse(
        Long expenseId,
        Long sourceRequestId,
        String expenseTypeCode,
        String expenseTypeLabel,
        String categoryCode,
        String categoryLabel,
        String enteredByDisplayName,
        BigDecimal amount,
        String amountLabel,
        String currencyCode,
        String title,
        String relatedEventName,
        String note,
        String spentAt,
        String spentAtLabel
) {
}
