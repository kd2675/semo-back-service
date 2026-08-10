package semo.back.service.feature.finance.vo;

import java.util.List;

public record ClubAdminFinanceHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canIssue,
        boolean canMarkPaid,
        boolean canMarkWaive,
        boolean canManageBilling,
        boolean canReviewRequests,
        boolean canCreateExpenses,
        boolean canUpdatePayments,
        boolean canExport,
        boolean canClosePeriods,
        int activeMemberCount,
        int totalObligationCount,
        int totalPaymentCount,
        int pendingPaymentCount,
        int paidPaymentCount,
        int waivedPaymentCount,
        int overduePaymentCount,
        int collectionRate,
        String totalBilledAmountLabel,
        String totalCollectedAmountLabel,
        String totalOutstandingAmountLabel,
        String totalWaivedAmountLabel,
        List<ClubFinanceMemberOptionResponse> availableMembers
) {
}
