package semo.back.service.feature.dues.vo;

public record IssueClubDuesInvoicesResponse(
        int billingYear,
        int billingMonth,
        String billingMonthLabel,
        int createdCount,
        int skippedCount
) {
}
