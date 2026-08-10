package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record FinanceBudgetResponse(
        Long financeBudgetId,
        String categoryCode,
        String categoryLabel,
        BigDecimal allocatedAmount,
        String allocatedAmountLabel,
        BigDecimal spentAmount,
        String spentAmountLabel,
        BigDecimal remainingAmount,
        String remainingAmountLabel,
        int executionRate,
        String note
) {
}
