package semo.back.service.feature.finance.vo;

import java.util.List;

public record ClubFinanceHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        int pendingPaymentCount,
        int paidPaymentCount,
        int overduePaymentCount,
        int actionRequiredCount,
        String totalPendingAmountLabel,
        String totalPaidAmountLabel,
        List<ClubFinanceUserObligationResponse> recentPayments,
        ClubFinanceUserObligationResponse nextPayableObligation,
        List<ClubFinanceUserObligationResponse> openObligations,
        List<ClubFinanceUserObligationResponse> paymentHistory
) {
}
