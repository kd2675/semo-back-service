package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record FinanceExpenseRevisionResponse(
        Long financeExpenseRevisionId,
        String revisionTypeCode,
        String revisedByDisplayName,
        BigDecimal previousAmount,
        BigDecimal nextAmount,
        String previousTitle,
        String nextTitle,
        String previousCategoryCode,
        String nextCategoryCode,
        String previousSpentAt,
        String nextSpentAt,
        Long previousFinancePeriodId,
        Long nextFinancePeriodId,
        Long previousFinanceAccountId,
        Long nextFinanceAccountId,
        Long previousScheduleEventId,
        Long nextScheduleEventId,
        String previousStatusCode,
        String nextStatusCode,
        String reason,
        String revisedAt
) {
}
