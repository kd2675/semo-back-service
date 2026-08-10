package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancePeriodResponse(
        Long financePeriodId,
        Long clubOperatingTermId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String statusCode,
        BigDecimal openingBalance,
        String openingBalanceLabel,
        BigDecimal collectedAmount,
        String collectedAmountLabel,
        BigDecimal spentAmount,
        String spentAmountLabel,
        BigDecimal currentBalance,
        String currentBalanceLabel,
        BigDecimal closingBalance,
        String closingBalanceLabel,
        String closedAt,
        String note,
        List<FinanceBudgetResponse> budgets
) {
}
