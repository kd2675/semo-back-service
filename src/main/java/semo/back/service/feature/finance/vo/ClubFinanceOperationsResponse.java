package semo.back.service.feature.finance.vo;

import java.util.List;

public record ClubFinanceOperationsResponse(
        Long clubId,
        String clubName,
        boolean canManageBilling,
        boolean canReviewRequests,
        boolean canCreateExpenses,
        boolean canUpdatePayments,
        boolean canExport,
        boolean canClosePeriods,
        List<FinanceAccountResponse> accounts,
        List<FinancePeriodResponse> periods,
        List<FinanceScheduleOptionResponse> scheduleOptions
) {
}
