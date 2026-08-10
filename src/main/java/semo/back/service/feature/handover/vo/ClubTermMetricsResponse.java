package semo.back.service.feature.handover.vo;

import java.math.BigDecimal;

public record ClubTermMetricsResponse(
        int todoCount,
        int scheduleCount,
        int tournamentCount,
        int financeObligationCount,
        int financeRequestCount,
        BigDecimal financeExpenseAmount,
        String currencyCode
) {
    public static ClubTermMetricsResponse empty() {
        return new ClubTermMetricsResponse(0, 0, 0, 0, 0, BigDecimal.ZERO, "KRW");
    }
}
