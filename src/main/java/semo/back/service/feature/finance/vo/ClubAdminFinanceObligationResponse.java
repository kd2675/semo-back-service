package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

public record ClubAdminFinanceObligationResponse(
        Long obligationId,
        String obligationTypeCode,
        String obligationTypeLabel,
        String title,
        String targetScopeCode,
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
        Long financePeriodId,
        String financePeriodTitle,
        Long financeAccountId,
        String financeAccountName,
        Long linkedScheduleEventId,
        String linkedScheduleEventTitle,
        String recurrenceFrequency,
        int recurrenceInterval,
        String recurrenceEndDate,
        String recurrenceLabel,
        boolean canDelete,
        int totalPaymentCount,
        int pendingPaymentCount,
        int paidPaymentCount,
        int waivedPaymentCount,
        int overduePaymentCount,
        int collectionRate
) {
}
