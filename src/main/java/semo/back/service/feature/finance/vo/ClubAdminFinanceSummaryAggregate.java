package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record ClubAdminFinanceSummaryAggregate(
        long totalPaymentCount,
        long pendingPaymentCount,
        long paidPaymentCount,
        long waivedPaymentCount,
        long overduePaymentCount,
        BigDecimal totalBilledAmount,
        BigDecimal totalCollectedAmount,
        BigDecimal totalOutstandingAmount,
        BigDecimal totalWaivedAmount
) {
}
